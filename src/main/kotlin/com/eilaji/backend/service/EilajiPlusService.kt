package com.eilaji.backend.service

import com.eilaji.backend.dto.*
import com.eilaji.backend.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class EilajiPlusService(
    private val eilajiPlusBaseUrl: String,
    private val eilajiPlusApiKey: String
) {
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
    
    suspend fun sendPrescriptionToEilajiPlus(prescriptionId: Int, userId: String): EilajiPlusPrescriptionResponse {
        return try {
            val prescription = transaction {
                Prescriptions.select { Prescriptions.id eq prescriptionId }.firstOrNull()
                    ?: throw IllegalArgumentException("Prescription not found")
            }
            
            val user = transaction {
                Users.select { Users.id eq userId }.firstOrNull()
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
                val body = response.body<EilajiPlusPrescriptionResponse>()
                
                // Update prescription with Eilaji-Plus reference
                transaction {
                    Prescriptions.update({ Prescriptions.id eq prescriptionId }) {
                        it[eilajiPlusRef] = body.eilajiPlusRef
                        it[eilajiPlusStatus] = "SENT"
                        it[updatedAt] = Instant.now()
                    }
                    
                    // Create sync record
                    EilajiPlusSync.insert {
                        it[prescriptionId] = prescriptionId
                        it[status] = "SENT"
                        it[lastAttempt] = Instant.now()
                        it[eilajiPlusRef] = body.eilajiPlusRef
                    }
                }
                
                body
            } else {
                throw Exception("Failed to send to Eilaji-Plus: ${response.status}")
            }
        } catch (e: Exception) {
            // Record failure for retry
            transaction {
                EilajiPlusSync.insert {
                    it[prescriptionId] = prescriptionId
                    it[status] = "FAILED"
                    it[lastAttempt] = Instant.now()
                    it[errorMessage] = e.message
                    it[retryCount] = 1
                }
            }
            throw e
        }
    }
    
    suspend fun processWebhook(request: EilajiPlusWebhookRequest): Boolean {
        return try {
            val syncRecord = transaction {
                EilajiPlusSync.select { 
                    EilajiPlusSync.eilajiPlusRef eq request.eilajiPlusRef 
                }.firstOrNull()
            } ?: throw IllegalArgumentException("Sync record not found")
            
            val prescriptionId = syncRecord[EilajiPlusSync.prescriptionId].value
            
            transaction {
                Prescriptions.update({ Prescriptions.id eq prescriptionId }) {
                    it[eilajiPlusStatus] = request.status
                    it[updatedAt] = Instant.now()
                    
                    if (request.quotedPrice != null) {
                        it[quotedPrice] = request.quotedPrice.toBigDecimal()
                    }
                    
                    if (request.message != null) {
                        it[pharmacistNotes] = request.message
                    }
                }
                
                EilajiPlusSync.update({ EilajiPlusSync.id eq syncRecord[EilajiPlusSync.id] }) {
                    it[status] = when (request.status) {
                        "CONFIRMED", "ACCEPTED" -> "CONFIRMED"
                        "REJECTED" -> "FAILED"
                        else -> request.status
                    }
                    it[lastAttempt] = Instant.now()
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getPendingRetries(limit: Int = 10): List<Long> {
        return transaction {
            EilajiPlusSync.select { 
                EilajiPlusSync.status eq "FAILED" and 
                (EilajiPlusSync.retryCount less 3)
            }
            .orderBy(EilajiPlusSync.lastAttempt.asc())
            .limit(limit)
            .map { it[EilajiPlusSync.prescriptionId].value }
        }
    }
    
    fun incrementRetryCount(prescriptionId: Int, errorMessage: String?) {
        transaction {
            val syncRecord = EilajiPlusSync.select { 
                EilajiPlusSync.prescriptionId eq prescriptionId 
            }.orderBy(EilajiPlusSync.createdAt.desc()).firstOrNull()
            
            if (syncRecord != null) {
                EilajiPlusSync.update({ EilajiPlusSync.id eq syncRecord[EilajiPlusSync.id] }) {
                    it[retryCount] = syncRecord[EilajiPlusSync.retryCount] + 1
                    it[lastAttempt] = Instant.now()
                    it[errorMessage] = errorMessage
                    it[status] = "PENDING"
                }
            }
        }
    }
}
