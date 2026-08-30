package com.eilaji.backend.service

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class EilajiPlusService(
    private val eilajiDoctorBaseUrl: String,
    private val eilajiDoctorApiKey: String
) {

    suspend fun sendPrescriptionToEilajiDoctor(prescriptionId: UUID, userId: String): EilajiPlusPrescriptionResponse {
        val userUuid = UUID.fromString(userId)
        return try {
            val prescription = transaction {
                Prescriptions.selectAll().where { Prescriptions.id eq prescriptionId }.firstOrNull()
                    ?: throw IllegalArgumentException("Prescription not found")
            }

            val user = transaction {
                Users.selectAll().where { Users.id eq userUuid }.firstOrNull()
                    ?: throw IllegalArgumentException("User not found")
            }

            val request = EilajiPlusPrescriptionRequest(
                prescriptionId = prescriptionId,
                userId = userId,
                imageUrl = prescription[Prescriptions.imageUrl],
                notes = prescription[Prescriptions.notes],
                patientName = user[Users.fullName],
                patientPhone = user[Users.phone]
            )

            val response = httpClient.post("$eilajiDoctorBaseUrl/api/prescriptions") {
                header("Authorization", "Bearer $eilajiDoctorApiKey")
                header("Content-Type", "application/json")
                setBody(request)
            }

            if (response.status.value in 200..299) {
                val body = Json.decodeFromString<EilajiPlusPrescriptionResponse>(response.bodyAsText())

                transaction {
                    Prescriptions.update({ Prescriptions.id eq prescriptionId }) {
                        it[Prescriptions.eilajiDoctorPrescriptionId] = body.eilajiPlusRef?.let { UUID.fromString(it) }
                        it[Prescriptions.sentToEilajiDoctor] = true
                        it[Prescriptions.updatedAt] = Instant.now()
                    }

                    EilajiPlusSync.insert {
                        it[EilajiPlusSync.prescriptionId] = prescriptionId
                        it[EilajiPlusSync.status] = "SENT"
                        it[EilajiPlusSync.lastAttempt] = Instant.now()
                        it[EilajiPlusSync.eilajiPlusRef] = body.eilajiPlusRef
                    }
                }

                body
            } else {
                throw Exception("Failed to send to Eilaji-Doctor: ${response.status}")
            }
        } catch (e: Exception) {
            transaction {
                EilajiPlusSync.insert {
                    it[EilajiPlusSync.prescriptionId] = prescriptionId
                    it[EilajiPlusSync.status] = "FAILED"
                    it[EilajiPlusSync.lastAttempt] = Instant.now()
                    it[EilajiPlusSync.errorMessage] = e.message
                    it[EilajiPlusSync.retryCount] = 1
                }
            }
            throw e
        }
    }

    suspend fun processWebhook(request: EilajiPlusWebhookRequest): Boolean {
        return try {
            val syncRecord = transaction {
                EilajiPlusSync.selectAll()
                    .where { EilajiPlusSync.eilajiPlusRef eq request.eilajiPlusRef }
                    .firstOrNull()
            } ?: throw IllegalArgumentException("Sync record not found")

            val prescriptionId = syncRecord[EilajiPlusSync.prescriptionId]

            transaction {
                Prescriptions.update({ Prescriptions.id eq prescriptionId }) {
                    it[Prescriptions.eilajiDoctorPrescriptionId] = request.eilajiPlusRef?.let { UUID.fromString(it) }
                    it[Prescriptions.updatedAt] = Instant.now()

                    if (request.quotedPrice != null) {
                        it[Prescriptions.quotedPrice] = request.quotedPrice.toBigDecimal()
                    }

                    if (request.message != null) {
                        it[Prescriptions.pharmacistNotes] = request.message
                    }
                }

                EilajiPlusSync.update({ EilajiPlusSync.id eq syncRecord[EilajiPlusSync.id] }) {
                    it[EilajiPlusSync.status] = when (request.status) {
                        "CONFIRMED", "ACCEPTED" -> "CONFIRMED"
                        "REJECTED" -> "FAILED"
                        else -> request.status
                    }
                    it[EilajiPlusSync.lastAttempt] = Instant.now()
                }
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    fun getPendingRetries(limit: Int = 10): List<UUID> {
        return transaction {
            EilajiPlusSync.selectAll()
                .where {
                    (EilajiPlusSync.status eq "FAILED") and
                    (EilajiPlusSync.retryCount less 3)
                }
                .orderBy(EilajiPlusSync.lastAttempt, SortOrder.ASC_NULLS_LAST)
                .limit(limit, 0L)
                .map { it[EilajiPlusSync.prescriptionId] }
        }
    }

    fun incrementRetryCount(prescriptionId: UUID, errorMessage: String?) {
        transaction {
            val syncRecord = EilajiPlusSync.selectAll()
                .where { EilajiPlusSync.prescriptionId eq prescriptionId }
                .orderBy(EilajiPlusSync.createdAt, SortOrder.DESC)
                .firstOrNull()

            if (syncRecord != null) {
                EilajiPlusSync.update({ EilajiPlusSync.id eq syncRecord[EilajiPlusSync.id] }) {
                    it[EilajiPlusSync.retryCount] = syncRecord[EilajiPlusSync.retryCount] + 1
                    it[EilajiPlusSync.lastAttempt] = Instant.now()
                    it[EilajiPlusSync.errorMessage] = errorMessage
                    it[EilajiPlusSync.status] = "PENDING"
                }
            }
        }
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}
