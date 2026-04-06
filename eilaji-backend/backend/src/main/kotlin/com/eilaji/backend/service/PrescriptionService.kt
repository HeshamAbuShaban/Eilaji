package com.eilaji.backend.service

import com.eilaji.backend.dto.*
import com.eilaji.backend.model.*
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.InputStream
import java.time.Instant
import java.util.*

class MinioService(
    private val endpoint: String,
    private val accessKey: String,
    private val secretKey: String,
    private val region: String = "us-east-1"
) {
    private val client: MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .region(region)
        .build()
    
    init {
        // Ensure buckets exist
        createBucketIfNotExists("prescriptions")
        createBucketIfNotExists("medicine-images")
    }
    
    private fun createBucketIfNotExists(bucketName: String) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to create bucket $bucketName: ${e.message}", e)
        }
    }
    
    suspend fun uploadFile(bucket: String, objectName: String, inputStream: InputStream, size: Long, contentType: String): String {
        try {
            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .stream(inputStream, size, -1)
                    .contentType(contentType)
                    .build()
            )
            return "$endpoint/$bucket/$objectName"
        } catch (e: Exception) {
            throw RuntimeException("Failed to upload file: ${e.message}", e)
        }
    }
    
    suspend fun deleteFile(bucket: String, objectName: String) {
        try {
            client.removeObject(
                io.minio.RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .build()
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to delete file: ${e.message}", e)
        }
    }
    
    fun getPresignedUrl(bucket: String, objectName: String, expirySeconds: Int = 3600): String {
        return client.getPresignedObjectUrl(
            io.minio.GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .`object`(objectName)
                .expiry(expirySeconds)
                .build()
        )
    }
}

class PrescriptionService(
    private val minioService: MinioService,
    private val eilajiPlusService: EilajiPlusService? = null
) {
    
    fun createPrescription(userId: String, notes: String?, pharmacyId: Int?, imageUrl: String): PrescriptionDto {
        return transaction {
            val prescriptionId = Prescriptions.insertAndGetId {
                it[Prescriptions.userId] = userId
                it[Prescriptions.pharmacyId] = pharmacyId
                it[Prescriptions.imageUrl] = imageUrl
                it[Prescriptions.notes] = notes
                it[Prescriptions.status] = "PENDING"
            }
            
            Prescriptions.leftJoin(Pharmacies)
                .select { Prescriptions.id eq prescriptionId.value }
                .map { row ->
                    PrescriptionDto(
                        id = row[Prescriptions.id],
                        userId = row[Prescriptions.userId],
                        pharmacyId = row[Prescriptions.pharmacyId],
                        pharmacyName = row[Pharmacies.name],
                        imageUrl = row[Prescriptions.imageUrl],
                        notes = row[Prescriptions.notes],
                        status = row[Prescriptions.status],
                        quotedPrice = row[Prescriptions.quotedPrice]?.toDouble(),
                        pharmacistNotes = row[Prescriptions.pharmacistNotes],
                        eilajiPlusRef = row[Prescriptions.eilajiPlusRef],
                        eilajiPlusStatus = row[Prescriptions.eilajiPlusStatus],
                        createdAt = row[Prescriptions.createdAt],
                        updatedAt = row[Prescriptions.updatedAt]
                    )
                }.first()
        }
    }
    
    fun getPrescriptionsForUser(userId: String, status: String? = null, page: Int = 0, pageSize: Int = 20): PaginatedResult<PrescriptionDto> {
        return transaction {
            val baseQuery = if (status != null) {
                Prescriptions.leftJoin(Pharmacies).select { 
                    Prescriptions.userId eq userId and (Prescriptions.status eq status) 
                }
            } else {
                Prescriptions.leftJoin(Pharmacies).select { Prescriptions.userId eq userId }
            }
            
            val total = baseQuery.count()
            
            val prescriptions = baseQuery
                .orderBy(Prescriptions.createdAt.desc())
                .limit(pageSize, (page * pageSize).toLong())
                .map { row ->
                    PrescriptionDto(
                        id = row[Prescriptions.id],
                        userId = row[Prescriptions.userId],
                        pharmacyId = row[Prescriptions.pharmacyId],
                        pharmacyName = row[Pharmacies.name],
                        imageUrl = row[Prescriptions.imageUrl],
                        notes = row[Prescriptions.notes],
                        status = row[Prescriptions.status],
                        quotedPrice = row[Prescriptions.quotedPrice]?.toDouble(),
                        pharmacistNotes = row[Prescriptions.pharmacistNotes],
                        eilajiPlusRef = row[Prescriptions.eilajiPlusRef],
                        eilajiPlusStatus = row[Prescriptions.eilajiPlusStatus],
                        createdAt = row[Prescriptions.createdAt],
                        updatedAt = row[Prescriptions.updatedAt]
                    )
                }
            
            PaginatedResult(
                items = prescriptions,
                total = total,
                page = page,
                pageSize = pageSize,
                totalPages = (total + pageSize - 1) / pageSize
            )
        }
    }
    
    fun getPrescriptionById(prescriptionId: Int): PrescriptionDto? {
        return transaction {
            Prescriptions.leftJoin(Pharmacies)
                .select { Prescriptions.id eq prescriptionId }
                .map { row ->
                    PrescriptionDto(
                        id = row[Prescriptions.id],
                        userId = row[Prescriptions.userId],
                        pharmacyId = row[Prescriptions.pharmacyId],
                        pharmacyName = row[Pharmacies.name],
                        imageUrl = row[Prescriptions.imageUrl],
                        notes = row[Prescriptions.notes],
                        status = row[Prescriptions.status],
                        quotedPrice = row[Prescriptions.quotedPrice]?.toDouble(),
                        pharmacistNotes = row[Prescriptions.pharmacistNotes],
                        eilajiPlusRef = row[Prescriptions.eilajiPlusRef],
                        eilajiPlusStatus = row[Prescriptions.eilajiPlusStatus],
                        createdAt = row[Prescriptions.createdAt],
                        updatedAt = row[Prescriptions.updatedAt]
                    )
                }.firstOrNull()
        }
    }
    
    fun updatePrescriptionStatus(prescriptionId: Int, status: String, quotedPrice: Double?, pharmacistNotes: String?): PrescriptionDto? {
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
            
            Prescriptions.leftJoin(Pharmacies)
                .select { Prescriptions.id eq prescriptionId }
                .map { row ->
                    PrescriptionDto(
                        id = row[Prescriptions.id],
                        userId = row[Prescriptions.userId],
                        pharmacyId = row[Prescriptions.pharmacyId],
                        pharmacyName = row[Pharmacies.name],
                        imageUrl = row[Prescriptions.imageUrl],
                        notes = row[Prescriptions.notes],
                        status = row[Prescriptions.status],
                        quotedPrice = row[Prescriptions.quotedPrice]?.toDouble(),
                        pharmacistNotes = row[Prescriptions.pharmacistNotes],
                        eilajiPlusRef = row[Prescriptions.eilajiPlusRef],
                        eilajiPlusStatus = row[Prescriptions.eilajiPlusStatus],
                        createdAt = row[Prescriptions.createdAt],
                        updatedAt = row[Prescriptions.updatedAt]
                    )
                }.firstOrNull()
        }
    }
    
    fun deletePrescription(prescriptionId: Int, userId: String): Boolean {
        return transaction {
            val prescription = Prescriptions.select { Prescriptions.id eq prescriptionId }.firstOrNull()
                ?: return@transaction false
            
            // Only owner can delete
            if (prescription[Prescriptions.userId] != userId) {
                return@transaction false
            }
            
            Prescriptions.deleteWhere { Prescriptions.id eq prescriptionId }
            true
        }
    }
}
