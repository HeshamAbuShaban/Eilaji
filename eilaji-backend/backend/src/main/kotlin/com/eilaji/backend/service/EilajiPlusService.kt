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

class EilajiPlusService(
    private val eilajiPlusBaseUrl: String,
    private val eilajiPlusApiKey: String
) {

    suspend fun sendPrescriptionToEilajiPlus(prescriptionId: Int, userId: String): EilajiPlusPrescriptionResponse {
        return try {
            val prescription = transaction {
                Prescriptions.selectAll().where { Prescriptions.id eq prescriptionId }.firstOrNull()
                    ?: throw IllegalArgumentException("Prescription not found")
            }

            val user = transaction {
                Users.selectAll().where { Users.id eq userId }.firstOrNull()
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

            val response = httpClient.post("$eilajiPlusBaseUrl/api/prescriptions") {
                header("Authorization", "Bearer $eilajiPlusApiKey")
                header("Content-Type", "application/json")
                setBody(request)
            }

            if (response.status.value in 200..299) {
                val body = Json.decodeFromString<EilajiPlusPrescriptionResponse>(response.bodyAsText())

                transaction {
                    Prescriptions.update({ Prescriptions.id eq prescriptionId }) {
                        it[Prescriptions.eilajiPlusRef] = body.eilajiPlusRef
                        it[Prescriptions.eilajiPlusStatus] = "SENT"
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
                throw Exception("Failed to send to Eilaji-Plus: ${response.status}")
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
                    it[Prescriptions.eilajiPlusStatus] = request.status
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

    fun getPendingRetries(limit: Int = 10): List<Int> {
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

    fun incrementRetryCount(prescriptionId: Int, errorMessage: String?) {
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
