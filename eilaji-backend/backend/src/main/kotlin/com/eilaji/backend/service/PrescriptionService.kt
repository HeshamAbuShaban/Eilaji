package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class PrescriptionService(
    private val minioService: MinioService,
    private val eilajiDoctorService: EilajiPlusService? = null,
) {

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

    fun updatePrescriptionStatus(prescriptionId: UUID, status: String, quotedPrice: Double? = null,
                                 pharmacistNotes: String? = null): PrescriptionDto? {
        return transaction {
            Prescriptions.update({ Prescriptions.id eq prescriptionId }) {
                it[Prescriptions.status] = status
                if (quotedPrice != null) {
                    it[Prescriptions.quotedPrice] = quotedPrice.toBigDecimal()
                }
                if (pharmacistNotes != null) {
                    it[Prescriptions.pharmacistNotes] = pharmacistNotes
                }
                it[Prescriptions.updatedAt] = Instant.now()
            }

            getPrescriptionDtoById(prescriptionId)
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
