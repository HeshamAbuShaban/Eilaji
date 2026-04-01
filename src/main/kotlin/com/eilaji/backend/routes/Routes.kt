package com.eilaji.backend.routes

import com.eilaji.backend.dto.*
import com.eilaji.backend.model.*
import com.eilaji.backend.service.*
import com.eilaji.backend.websocket.WebSocketController
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.pipeline.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest
import java.time.Instant

fun Route.apiRoutes(
    jwtIssuer: String,
    jwtAudience: String,
    jwtRealm: String,
    jwtSecret: String,
    minioService: MinioService,
    redisService: RedisService,
    eilajiPlusService: EilajiPlusService? = null
) {
    val prescriptionService = PrescriptionService(minioService, eilajiPlusService)
    val chatService = ChatService()
    val messageService = MessageService()
    val sessionManager = WebSocketSessionManager(messageService, chatService, redisService)
    val webSocketController = WebSocketController(sessionManager, redisService, messageService)
    
    // Authentication configuration
    val authentication = authenticate("jwt-auth")
    
    routes {
        // Health check
        get("/health") {
            call.respond(ApiResponse(success = true, message = "OK"))
        }
        
        // Public routes (no auth required)
        route("/api/v1") {
            // Medicine catalog - public read access
            route("/medicines") {
                get {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                    val categoryId = call.request.queryParameters["categoryId"]?.toIntOrNull()
                    val subcategoryId = call.request.queryParameters["subcategoryId"]?.toIntOrNull()
                    val requiresPrescription = call.request.queryParameters["requiresPrescription"]?.toBooleanStrictOrNull()
                    
                    try {
                        val result = transaction {
                            val baseQuery = if (categoryId != null) {
                                if (subcategoryId != null) {
                                    Medicines.leftJoin(Categories, "category_join", Medicines.categoryId, Categories.id)
                                        .leftJoin(Categories, "subcategory_join", Medicines.subcategoryId, Categories.id)
                                        .select { Medicines.categoryId eq categoryId and (Medicines.subcategoryId eq subcategoryId) }
                                } else {
                                    Medicines.leftJoin(Categories, "category_join", Medicines.categoryId, Categories.id)
                                        .leftJoin(Categories, "subcategory_join", Medicines.subcategoryId, Categories.id)
                                        .select { Medicines.categoryId eq categoryId }
                                }
                            } else {
                                Medicines.leftJoin(Categories, "category_join", Medicines.categoryId, Categories.id)
                                    .leftJoin(Categories, "subcategory_join", Medicines.subcategoryId, Categories.id)
                                    .selectAll()
                            }
                            
                            val total = baseQuery.count()
                            val medicines = baseQuery
                                .orderBy(Medicines.titleEn.asc())
                                .limit(pageSize, (page * pageSize).toLong())
                                .map { row ->
                                    MedicineDto(
                                        id = row[Medicines.id],
                                        titleEn = row[Medicines.titleEn],
                                        titleAr = row[Medicines.titleAr],
                                        description = row[Medicines.description],
                                        categoryId = row[Medicines.categoryId],
                                        categoryName = row[Categories, "category_join"].nameEn,
                                        subcategoryId = row[Medicines.subcategoryId],
                                        subcategoryName = row[Categories, "subcategory_join"]?.nameEn,
                                        manufacturer = row[Medicines.manufacturer],
                                        requiresPrescription = row[Medicines.requiresPrescription],
                                        price = row[Medicines.price]?.toDouble(),
                                        imageUrl = row[Medicines.imageUrl],
                                        isActive = row[Medicines.isActive],
                                        createdAt = row[Medicines.createdAt]
                                    )
                                }
                            
                            PaginatedResult(
                                items = medicines,
                                total = total,
                                page = page,
                                pageSize = pageSize,
                                totalPages = (total + pageSize - 1) / pageSize
                            )
                        }
                        call.respond(ApiResponse(success = true, data = result))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                    }
                }
                
                get("/search") {
                    val query = call.request.queryParameters["q"] ?: ""
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                    
                    if (query.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "Search query required"))
                        return@get
                    }
                    
                    try {
                        val result = transaction {
                            val searchPattern = "%$query%"
                            val baseQuery = Medicines.leftJoin(Categories, "category_join", Medicines.categoryId, Categories.id)
                                .leftJoin(Categories, "subcategory_join", Medicines.subcategoryId, Categories.id)
                                .select {
                                    Medicines.titleEn.like(searchPattern) or
                                    Medicines.titleAr.like(searchPattern) or
                                    (Medicines.description like searchPattern) or
                                    (Medicines.manufacturer like searchPattern)
                                }
                            
                            val total = baseQuery.count()
                            val medicines = baseQuery
                                .orderBy(Medicines.titleEn.asc())
                                .limit(pageSize, (page * pageSize).toLong())
                                .map { row ->
                                    MedicineDto(
                                        id = row[Medicines.id],
                                        titleEn = row[Medicines.titleEn],
                                        titleAr = row[Medicines.titleAr],
                                        description = row[Medicines.description],
                                        categoryId = row[Medicines.categoryId],
                                        categoryName = row[Categories, "category_join"].nameEn,
                                        subcategoryId = row[Medicines.subcategoryId],
                                        subcategoryName = row[Categories, "subcategory_join"]?.nameEn,
                                        manufacturer = row[Medicines.manufacturer],
                                        requiresPrescription = row[Medicines.requiresPrescription],
                                        price = row[Medicines.price]?.toDouble(),
                                        imageUrl = row[Medicines.imageUrl],
                                        isActive = row[Medicines.isActive],
                                        createdAt = row[Medicines.createdAt]
                                    )
                                }
                            
                            PaginatedResult(
                                items = medicines,
                                total = total,
                                page = page,
                                pageSize = pageSize,
                                totalPages = (total + pageSize - 1) / pageSize
                            )
                        }
                        call.respond(ApiResponse(success = true, data = result))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                    }
                }
                
                get("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "Invalid ID"))
                        return@get
                    }
                    
                    try {
                        val medicine = transaction {
                            Medicines.leftJoin(Categories, "category_join", Medicines.categoryId, Categories.id)
                                .leftJoin(Categories, "subcategory_join", Medicines.subcategoryId, Categories.id)
                                .select { Medicines.id eq id }
                                .map { row ->
                                    MedicineDto(
                                        id = row[Medicines.id],
                                        titleEn = row[Medicines.titleEn],
                                        titleAr = row[Medicines.titleAr],
                                        description = row[Medicines.description],
                                        categoryId = row[Medicines.categoryId],
                                        categoryName = row[Categories, "category_join"].nameEn,
                                        subcategoryId = row[Medicines.subcategoryId],
                                        subcategoryName = row[Categories, "subcategory_join"]?.nameEn,
                                        manufacturer = row[Medicines.manufacturer],
                                        requiresPrescription = row[Medicines.requiresPrescription],
                                        price = row[Medicines.price]?.toDouble(),
                                        imageUrl = row[Medicines.imageUrl],
                                        isActive = row[Medicines.isActive],
                                        createdAt = row[Medicines.createdAt]
                                    )
                                }.firstOrNull()
                        }
                        
                        if (medicine != null) {
                            call.respond(ApiResponse(success = true, data = medicine))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ApiResponse<Map<String, Any>?>(success = false, error = "Medicine not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                    }
                }
            }
            
            // Categories - public read access
            route("/medicines/categories") {
                get {
                    try {
                        val categories = transaction {
                            Categories.selectAll().orderBy(Categories.nameEn.asc()).map { row ->
                                CategoryDto(
                                    id = row[Categories.id],
                                    nameEn = row[Categories.nameEn],
                                    nameAr = row[Categories.nameAr],
                                    description = row[Categories.description],
                                    parentId = row[Categories.parentId],
                                    createdAt = row[Categories.createdAt]
                                )
                            }
                        }
                        call.respond(ApiResponse(success = true, data = categories))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                    }
                }
            }
            
            // Pharmacies - public read access for nearby search
            route("/pharmacies") {
                get("/nearby") {
                    val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
                    val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
                    val radius = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 10.0
                    
                    if (lat == null || lng == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "Latitude and longitude required"))
                        return@get
                    }
                    
                    try {
                        val pharmacies = transaction {
                            // Use Haversine formula for distance calculation
                            val haversine = "(6371 * acos(cos(radians($lat)) * cos(radians(latitude)) * cos(radians(longitude) - radians($lng)) + sin(radians($lat)) * sin(radians(latitude))))"
                            
                            val query = """
                                SELECT *, $haversine AS distance_km
                                FROM pharmacies
                                WHERE $haversine <= ?
                                ORDER BY distance_km ASC
                            """.trimIndent()
                            
                            val result = execSQL(query, listOf(radius)) { rs ->
                                buildList {
                                    while (rs.next()) {
                                        add(
                                            PharmacyDto(
                                                id = rs.getInt("id"),
                                                ownerId = rs.getString("owner_id"),
                                                ownerName = null,
                                                name = rs.getString("name"),
                                                address = rs.getString("address"),
                                                city = rs.getString("city"),
                                                latitude = rs.getDouble("latitude"),
                                                longitude = rs.getDouble("longitude"),
                                                phone = rs.getString("phone"),
                                                isVerified = rs.getBoolean("is_verified"),
                                                isOpen = rs.getBoolean("is_open"),
                                                openingHours = rs.getString("opening_hours"),
                                                licenseNumber = rs.getString("license_number"),
                                                distanceKm = rs.getDouble("distance_km"),
                                                isOnline = false,
                                                createdAt = Instant.now()
                                            )
                                        )
                                    }
                                }
                            }
                            result
                        }
                        call.respond(ApiResponse(success = true, data = pharmacies))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                    }
                }
                
                get {
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                    val city = call.request.queryParameters["city"]
                    val isOpen = call.request.queryParameters["isOpen"]?.toBooleanStrictOrNull()
                    val isVerified = call.request.queryParameters["isVerified"]?.toBooleanStrictOrNull()
                    
                    try {
                        val result = transaction {
                            var query = Pharmacies.selectAll()
                            
                            if (city != null) {
                                query = query.where { Pharmacies.city eq city }
                            }
                            if (isOpen != null) {
                                query = query.and { Pharmacies.isOpen eq isOpen }
                            }
                            if (isVerified != null) {
                                query = query.and { Pharmacies.isVerified eq isVerified }
                            }
                            
                            val total = query.count()
                            val pharmacies = query
                                .orderBy(Pharmacies.name.asc())
                                .limit(pageSize, (page * pageSize).toLong())
                                .map { row ->
                                    PharmacyDto(
                                        id = row[Pharmacies.id],
                                        ownerId = row[Pharmacies.ownerId],
                                        ownerName = null,
                                        name = row[Pharmacies.name],
                                        address = row[Pharmacies.address],
                                        city = row[Pharmacies.city],
                                        latitude = row[Pharmacies.latitude],
                                        longitude = row[Pharmacies.longitude],
                                        phone = row[Pharmacies.phone],
                                        isVerified = row[Pharmacies.isVerified],
                                        isOpen = row[Pharmacies.isOpen],
                                        openingHours = row[Pharmacies.openingHours],
                                        licenseNumber = row[Pharmacies.licenseNumber],
                                        createdAt = row[Pharmacies.createdAt]
                                    )
                                }
                            
                            PaginatedResult(
                                items = pharmacies,
                                total = total,
                                page = page,
                                pageSize = pageSize,
                                totalPages = (total + pageSize - 1) / pageSize
                            )
                        }
                        call.respond(ApiResponse(success = true, data = result))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                    }
                }
            }
        }
        
        // Protected routes (authentication required)
        authentication {
            route("/api/v1") {
                // User's own data
                route("/user") {
                    get {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getSubject()
                        
                        try {
                            val user = transaction {
                                Users.select { Users.id eq userId }.firstOrNull()?.let { row ->
                                    UserDto(
                                        id = row[Users.id],
                                        email = row[Users.email],
                                        fullName = row[Users.fullName],
                                        phone = row[Users.phone],
                                        role = row[Users.role],
                                        createdAt = row[Users.createdAt]
                                    )
                                }
                            }
                            
                            if (user != null) {
                                call.respond(ApiResponse(success = true, data = user))
                            } else {
                                call.respond(HttpStatusCode.NotFound, ApiResponse<Map<String, Any>?>(success = false, error = "User not found"))
                            }
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                }
                
                // Prescriptions (protected)
                route("/prescriptions") {
                    post {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getSubject()
                        
                        try {
                            val multipart = call.receiveMultipart()
                            var notes: String? = null
                            var pharmacyId: Int? = null
                            var imagePart: PartData.FileItem? = null
                            
                            multipart.forEachPart { part ->
                                when (part.name) {
                                    "notes" -> notes = (part as? PartData.FormItem)?.value
                                    "selectedPharmacyId" -> pharmacyId = (part as? PartData.FormItem)?.value?.toIntOrNull()
                                    "image" -> imagePart = part as? PartData.FileItem
                                }
                            }
                            
                            if (imagePart == null) {
                                call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "Image required"))
                                return@post
                            }
                            
                            // Upload to MinIO
                            val timestamp = System.currentTimeMillis()
                            val extension = imagePart.originalFileName?.substringAfterLast('.') ?: "jpg"
                            val objectName = "prescriptions/$userId/${timestamp}.$extension"
                            
                            val imageUrl = minioService.uploadFile(
                                bucket = "prescriptions",
                                objectName = objectName,
                                inputStream = imagePart.provider(),
                                size = imagePart.partSize,
                                contentType = imagePart.contentType?.toString() ?: "image/jpeg"
                            )
                            
                            // Create prescription record
                            val prescription = prescriptionService.createPrescription(userId, notes, pharmacyId, imageUrl)
                            
                            // Send to Eilaji-Plus if configured
                            if (eilajiPlusService != null) {
                                try {
                                    eilajiPlusService.sendPrescriptionToEilajiPlus(prescription.id, userId)
                                } catch (e: Exception) {
                                    // Log error but don't fail the request
                                }
                            }
                            
                            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = prescription))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                    
                    get {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getSubject()
                        
                        val status = call.request.queryParameters["status"]
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                        
                        try {
                            val result = prescriptionService.getPrescriptionsForUser(userId, status, page, pageSize)
                            call.respond(ApiResponse(success = true, data = result))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                    
                    get("/{id}") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getSubject()
                        val id = call.parameters["id"]?.toIntOrNull()
                        
                        if (id == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "Invalid ID"))
                            return@get
                        }
                        
                        try {
                            val prescription = prescriptionService.getPrescriptionById(id)
                            
                            if (prescription != null && prescription.userId == userId) {
                                call.respond(ApiResponse(success = true, data = prescription))
                            } else {
                                call.respond(HttpStatusCode.NotFound, ApiResponse<Map<String, Any>?>(success = false, error = "Prescription not found"))
                            }
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                }
                
                // Chats (protected)
                route("/chats") {
                    get {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getSubject()
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                        
                        try {
                            val result = chatService.getChatsForUser(userId, page, pageSize)
                            call.respond(ApiResponse(success = true, data = result))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                    
                    post {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getSubject()
                        
                        try {
                            val request = call.receive<CreateChatRequest>()
                            val chat = chatService.createChat(request, userId)
                            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = chat))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                    
                    get("/{chatId}/messages") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getSubject()
                        val chatId = call.parameters["chatId"]?.toLongOrNull()
                        
                        if (chatId == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "Invalid chat ID"))
                            return@get
                        }
                        
                        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50
                        
                        try {
                            val result = messageService.getMessagesForChat(chatId, page, pageSize)
                            call.respond(ApiResponse(success = true, data = result))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                    
                    post("/{chatId}/read") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal!!.payload.getSubject()
                        val chatId = call.parameters["chatId"]?.toLongOrNull()
                        
                        if (chatId == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "Invalid chat ID"))
                            return@post
                        }
                        
                        try {
                            val request = call.receive<MarkAsReadRequest>()
                            val count = messageService.markChatAsRead(chatId, userId)
                            call.respond(ApiResponse(success = true, data = mapOf("markedCount" to count)))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                }
                
                // Presence endpoint
                route("/presence") {
                    get("/online") {
                        try {
                            val onlineUsers = redisService.getOnlineUsers()
                            call.respond(ApiResponse(success = true, data = mapOf("onlineUsers" to onlineUsers.toList())))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                    
                    get("/{userId}") {
                        val targetUserId = call.parameters["userId"]
                        
                        if (targetUserId == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "User ID required"))
                            return@get
                        }
                        
                        try {
                            val isOnline = redisService.isUserOnline(targetUserId)
                            call.respond(ApiResponse(success = true, data = mapOf("userId" to targetUserId, "isOnline" to isOnline)))
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                }
                
                // Eilaji-Plus webhook (for receiving status updates)
                route("/eilaji-plus/webhook") {
                    post {
                        try {
                            val request = call.receive<EilajiPlusWebhookRequest>()
                            val success = eilajiPlusService?.processWebhook(request) ?: false
                            
                            if (success) {
                                call.respond(ApiResponse(success = true, message = "Webhook processed successfully"))
                            } else {
                                call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "Failed to process webhook"))
                            }
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                        }
                    }
                }
                
                // WebSocket endpoint
                webSocket("/ws/chat") {
                    val token = call.request.queryParameters["token"]
                    
                    if (token.isNullOrBlank()) {
                        close(CloseReason(CloseReason.Codes.INVALID_PAYLOAD, "Authentication token required"))
                        return@webSocket
                    }
                    
                    // Validate JWT token from query parameter
                    try {
                        val verifier = io.ktor.server.auth.jwt.JWTVerifier(io.ktor.server.auth.jwt.JWTAlgorithm.RSA256, jwtIssuer, jwtAudience)
                        // Simplified validation - in production use proper JWT verification
                        val userId = extractUserIdFromToken(token) // Implement proper token parsing
                        
                        if (userId != null) {
                            webSocketController.handleWebSocketSession(this, userId)
                        } else {
                            close(CloseReason(CloseReason.Codes.INVALID_PAYLOAD, "Invalid token"))
                        }
                    } catch (e: Exception) {
                        close(CloseReason(CloseReason.Codes.INVALID_PAYLOAD, "Authentication failed"))
                    }
                }
            }
        }
    }
}

// Helper function to extract user ID from JWT token
private fun extractUserIdFromToken(token: String): String? {
    try {
        val parts = token.split(".")
        if (parts.size < 2) return null
        
        val payload = String(java.util.Base64.getDecoder().decode(parts[1]))
        val json = kotlinx.serialization.json.Json.parseToJsonElement(payload).jsonObject
        return json["sub"]?.jsonPrimitive?.content
    } catch (e: Exception) {
        return null
    }
}

// Helper for raw SQL execution
private inline fun <T> Transaction.execSQL(sql: String, params: List<Any?>, transform: (java.sql.ResultSet) -> T): T {
    return exec(sql) { statement ->
        params.forEachIndexed { index, param ->
            statement.setObject(index + 1, param)
        }
        val rs = statement.executeQuery()
        transform(rs)
    }
}
