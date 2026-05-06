package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class PrescriptionService(
    private val minioService: MinioService,
    private val eilajiPlusService: EilajiPlusService? = null,
) {

    fun createPrescription(userId: String, notes: String?, pharmacyId: Int?, imageUrl: String): PrescriptionDto {
        return transaction {
            val prescriptionId = Prescriptions.insert {
                it[Prescriptions.userId] = userId
                it[Prescriptions.pharmacyId] = pharmacyId
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
        return transaction {
            val baseQuery = if (status != null) {
                Prescriptions.join(Pharmacies, JoinType.LEFT, Prescriptions.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { (Prescriptions.userId eq userId) and (Prescriptions.status eq status) }
            } else {
                Prescriptions.join(Pharmacies, JoinType.LEFT, Prescriptions.pharmacyId, Pharmacies.id)
                    .selectAll()
                    .where { Prescriptions.userId eq userId }
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

    fun getPrescriptionById(prescriptionId: Int): PrescriptionDto? {
        return transaction {
            getPrescriptionDtoById(prescriptionId)
        }
    }

    fun updatePrescriptionStatus(prescriptionId: Int, status: String, quotedPrice: Double? = null,
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

    fun deletePrescription(prescriptionId: Int, userId: String): Boolean {
        return transaction {
            val prescription = Prescriptions.selectAll().where { Prescriptions.id eq prescriptionId }.firstOrNull()
                ?: return@transaction false

            if (prescription[Prescriptions.userId] != userId) {
                return@transaction false
            }

            Prescriptions.deleteWhere { Prescriptions.id eq prescriptionId }
            true
        }
    }

    private fun getPrescriptionDtoById(prescriptionId: Int): PrescriptionDto? {
        return Prescriptions.join(Pharmacies, JoinType.LEFT, Prescriptions.pharmacyId, Pharmacies.id)
            .selectAll()
            .where { Prescriptions.id eq prescriptionId }
            .map { row -> mapRowToPrescriptionDto(row) }
            .firstOrNull()
    }

    private fun mapRowToPrescriptionDto(row: ResultRow): PrescriptionDto {
        return PrescriptionDto(
            id = row[Prescriptions.id],
            userId = row[Prescriptions.userId],
            pharmacyId = row[Prescriptions.pharmacyId],
            pharmacyName = row.getOrNull(Pharmacies.name),
            imageUrl = row[Prescriptions.imageUrl],
            notes = row[Prescriptions.notes],
            status = row[Prescriptions.status],
            quotedPrice = row[Prescriptions.quotedPrice]?.toDouble(),
            pharmacistNotes = row[Prescriptions.pharmacistNotes],
            eilajiPlusRef = row[Prescriptions.eilajiPlusRef],
            eilajiPlusStatus = row[Prescriptions.eilajiPlusStatus],
            createdAt = row[Prescriptions.createdAt].toString(),
            updatedAt = row[Prescriptions.updatedAt].toString()
        )
    }
}
