package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import com.eilaji.backend.security.ApiException
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class PrescriptionService(
    private val minioService: MinioService,
    private val eilajiDoctorService: EilajiPlusService? = null,
) {
    var pushService: PushService? = null

    companion object {
        private val REVIEW_STATUSES = setOf(
            "PENDING", "SENT_TO_PHARMACY", "RECEIVED_QUOTE",
            "ACCEPTED", "REJECTED", "COMPLETED", "CANCELLED"
        )
        private val REVIEW_TRANSITIONS = mapOf(
            "PENDING" to setOf("SENT_TO_PHARMACY", "REJECTED", "CANCELLED"),
            "SENT_TO_PHARMACY" to setOf("RECEIVED_QUOTE", "REJECTED", "CANCELLED"),
            "RECEIVED_QUOTE" to setOf("ACCEPTED", "REJECTED"),
            "ACCEPTED" to setOf("COMPLETED", "CANCELLED"),
            "REJECTED" to setOf<String>(),
            "COMPLETED" to setOf<String>(),
            "CANCELLED" to setOf<String>()
        )
    }

    fun createPrescription(userId: String, notes: String?, pharmacyId: UUID?, imageUrl: String): PrescriptionDto {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val prescriptionId = Prescriptions.insert {
                it[Prescriptions.patientUserId] = userUuid
                it[Prescriptions.selectedPharmacyId] = pharmacyId
                it[Prescriptions.imageUrl] = imageUrl
                it[Prescriptions.notes] = notes
                it[Prescriptions.status] = "PENDING"
                it[Prescriptions.createdAt] = Instant.now()
                it[Prescriptions.updatedAt] = Instant.now()
            } get Prescriptions.id

            getPrescriptionDtoById(prescriptionId)!!
        }
    }

    fun getPrescriptionsForUser(userId: String, status: String? = null, page: Int = 0, pageSize: Int = 20): PaginatedResult<PrescriptionDto> {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val baseQuery = if (status != null) {
                Prescriptions.join(Pharmacies, JoinType.LEFT, Prescriptions.selectedPharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { (Prescriptions.patientUserId eq userUuid) and (Prescriptions.status eq status) }
            } else {
                Prescriptions.join(Pharmacies, JoinType.LEFT, Prescriptions.selectedPharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { Prescriptions.patientUserId eq userUuid }
            }

            val total = baseQuery.count()

            val prescriptions = baseQuery
                .orderBy(Prescriptions.createdAt, SortOrder.DESC)
                .limit(pageSize, (page * pageSize).toLong())
                .map { row -> mapRowToPrescriptionDto(row) }

            PaginatedResult(
                items = prescriptions,
                total = total,
                page = page,
                pageSize = pageSize,
                totalPages = if (total > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
            )
        }
    }

    fun getPrescriptionById(prescriptionId: UUID): PrescriptionDto? {
        return transaction {
            getPrescriptionDtoById(prescriptionId)
        }
    }

    /**
     * Reviews a prescription: quote a price and accept or reject it.
     *
     * Only the pharmacy that the prescription was sent to (or an admin) may do this, and the
     * status must move along the review path. ACCEPTED is the state an order can be built from.
     */
    fun updatePrescriptionStatus(
        prescriptionId: UUID,
        status: String,
        quotedPrice: Double? = null,
        pharmacistNotes: String? = null,
        actorId: String,
        actorRole: com.eilaji.backend.data.UserRole
    ): PrescriptionDto {
        val next = status.trim().uppercase()
        if (next !in REVIEW_STATUSES) throw ApiException(HttpStatusCode.BadRequest, "Unknown prescription status")
        if (quotedPrice != null && quotedPrice < 0) throw ApiException(HttpStatusCode.BadRequest, "Quoted price cannot be negative")
        val notes = pharmacistNotes?.trim()?.take(1000)

        return transaction {
            val prescription = Prescriptions.selectAll().where { Prescriptions.id eq prescriptionId }.singleOrNull()
                ?: throw ApiException(HttpStatusCode.NotFound, "Prescription not found")

            if (actorRole == com.eilaji.backend.data.UserRole.PHARMACIST) {
                val actorUuid = try { UUID.fromString(actorId) } catch (_: Exception) {
                    throw ApiException(HttpStatusCode.Unauthorized, "Invalid user")
                }
                val targetPharmacyId = prescription[Prescriptions.selectedPharmacyId]
                if (targetPharmacyId == null) {
                    throw ApiException(HttpStatusCode.Forbidden, "This prescription was not sent to a pharmacy")
                }
                val owned = Pharmacies.selectAll().where { Pharmacies.ownerUserId eq actorUuid }.map { it[Pharmacies.id] }
                if (targetPharmacyId !in owned) {
                    throw ApiException(HttpStatusCode.NotFound, "Prescription not found")
                }
            }

            val current = prescription[Prescriptions.status].uppercase()
            val allowed = REVIEW_TRANSITIONS[current] ?: setOf()
            if (next !in allowed) throw ApiException(HttpStatusCode.Conflict, "Cannot move a prescription from $current to $next")
            if (next in setOf("RECEIVED_QUOTE", "ACCEPTED") && quotedPrice == null) {
                throw ApiException(HttpStatusCode.BadRequest, "A quoted price is required for $next")
            }

            Prescriptions.update({ Prescriptions.id eq prescriptionId }) {
                it[Prescriptions.status] = next
                if (quotedPrice != null) it[Prescriptions.quotedPrice] = quotedPrice.toBigDecimal()
                if (notes != null) it[Prescriptions.pharmacistNotes] = notes
                it[Prescriptions.updatedAt] = Instant.now()
            }

            val updated = getPrescriptionDtoById(prescriptionId)
                ?: throw ApiException(HttpStatusCode.NotFound, "Prescription not found")
            pushService?.let { ps ->
                try {
                    ps.send(
                        PushService.Payload(
                            title = "Prescription ${next.lowercase().replaceFirstChar { it.uppercase() }}",
                            body = "Your prescription is now $next",
                            route = "prescriptions",
                            targetUserId = updated.userId
                        )
                    )
                } catch (_: Exception) { }
            }
            updated
        }
    }

    fun deletePrescription(prescriptionId: UUID, userId: String): Boolean {
        val userUuid = UUID.fromString(userId)
        return transaction {
            val prescription = Prescriptions.selectAll().where { Prescriptions.id eq prescriptionId }.firstOrNull()
                ?: return@transaction false

            if (prescription[Prescriptions.patientUserId] != userUuid) {
                return@transaction false
            }

            Prescriptions.deleteWhere { Prescriptions.id eq prescriptionId }
            true
        }
    }

    private fun getPrescriptionDtoById(prescriptionId: UUID): PrescriptionDto? {
        return Prescriptions.join(Pharmacies, JoinType.LEFT, Prescriptions.selectedPharmacyId, Pharmacies.id)
            .selectAll()
            .where { Prescriptions.id eq prescriptionId }
            .map { row -> mapRowToPrescriptionDto(row) }
            .firstOrNull()
    }

    private fun mapRowToPrescriptionDto(row: ResultRow): PrescriptionDto {
        return PrescriptionDto(
            id = row[Prescriptions.id].toString(),
            userId = row[Prescriptions.patientUserId].toString(),
            pharmacyId = row[Prescriptions.selectedPharmacyId]?.toString(),
            pharmacyName = row.getOrNull(Pharmacies.name),
            imageUrl = row[Prescriptions.imageUrl],
            notes = row[Prescriptions.notes],
            status = row[Prescriptions.status],
            quotedPrice = row[Prescriptions.quotedPrice]?.toDouble(),
            pharmacistNotes = row[Prescriptions.pharmacistNotes],
            eilajiPlusRef = row[Prescriptions.eilajiDoctorPrescriptionId]?.toString(),
            eilajiPlusStatus = row[Prescriptions.sentToEilajiDoctor]?.toString(),
            createdAt = row[Prescriptions.createdAt].toString(),
            updatedAt = row[Prescriptions.updatedAt].toString()
        )
    }
}
