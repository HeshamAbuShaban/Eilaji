package com.eilaji.backend.service

import com.eilaji.backend.security.SecurityUtils
import io.minio.*
import io.minio.messages.Item
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

class MinioService {
    private val config = ConfigFactory.load()
    
    private val endpoint: String = config.getString("minio.endpoint")
    private val accessKey: String = config.getString("minio.accessKey")
    private val secretKey: String = config.getString("minio.secretKey")
    
    private val bucketPrescriptions: String = config.getString("minio.buckets.prescriptions")
    private val bucketMedicineImages: String = config.getString("minio.buckets.medicineImages")
    private val bucketPharmacyImages: String = config.getString("minio.buckets.pharmacyImages")
    private val bucketUserAvatars: String = config.getString("minio.buckets.userAvatars")
    
    private val client: MinioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()
    
    init {
        ensureBucketsExist()
    }
    
    private fun ensureBucketsExist() {
        val buckets = listOf(
            bucketPrescriptions,
            bucketMedicineImages,
            bucketPharmacyImages,
            bucketUserAvatars
        )
        
        buckets.forEach { bucketName ->
            try {
                if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build())
                    println("✅ Created MinIO bucket: $bucketName")
                } else {
                    println("ℹ️  MinIO bucket already exists: $bucketName")
                }
            } catch (e: Exception) {
                println("❌ Error ensuring bucket $bucketName exists: ${e.message}")
            }
        }
    }
    
    suspend fun uploadFile(
        bucket: String,
        objectName: String,
        inputStream: InputStream,
        contentType: String = "application/octet-stream"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate and sanitize inputs
            if (bucket.isBlank() || objectName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Bucket and object name cannot be blank"))
            }

            // Sanitize bucket name
            if (!bucket.matches(Regex("^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$"))) {
                return@withContext Result.failure(IllegalArgumentException("Invalid bucket name format"))
            }

            // Sanitize object name to prevent path traversal
            val sanitizedObjectName = SecurityUtils.sanitizeFilename(objectName)

            // Validate content type
            val allowedContentTypes = setOf(
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "application/pdf", "text/plain", "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )
            val safeContentType = if (contentType in allowedContentTypes) contentType else "application/octet-stream"

            client.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(sanitizedObjectName)
                    .stream(inputStream, inputStream.available().toLong(), -1)
                    .contentType(safeContentType)
                    .build()
            )

            val fileUrl = "$endpoint/$bucket/$sanitizedObjectName"
            Result.success(fileUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFile(
        bucket: String,
        objectName: String
    ): Result<InputStream> = withContext(Dispatchers.IO) {
        try {
            val response = client.getObject(
                GetObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .build()
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteFile(
        bucket: String,
        objectName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .`object`(objectName)
                    .build()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun listFiles(
        bucket: String,
        prefix: String = ""
    ): Result<List<Item>> = withContext(Dispatchers.IO) {
        try {
            val objects = mutableListOf<Item>()
            val results = client.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .recursive(true)
                    .build()
            )
            
            for (result in results) {
                result?.let { objects.add(it) }
            }
            
            Result.success(objects)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getPublicUrl(bucket: String, objectName: String): String {
        return "$endpoint/$bucket/$objectName"
    }
}
