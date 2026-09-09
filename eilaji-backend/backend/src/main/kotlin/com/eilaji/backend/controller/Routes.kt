package com.eilaji.backend.routes

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import com.eilaji.backend.security.AuditService
import com.eilaji.backend.security.JwtConfig
import com.eilaji.backend.security.SecurityUtils
import com.eilaji.backend.service.*
import com.eilaji.backend.controller.registerAuthRoutes
import com.eilaji.backend.websocket.WebSocketController
import com.eilaji.backend.websocket.WebSocketSessionManager
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.readRemaining
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

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
    val orderService = OrderService()

    fun getAuthenticatedUserId(call: ApplicationCall): String? {
        return try {
            val principal = call.principal<JWTPrincipal>()
            principal?.payload?.subject
        } catch (e: Exception) {
            null
        }
    }

    fun validateUuid(uuid: String): Boolean {
        return SecurityUtils.isValidUuid(uuid)
    }

    // Public routes
    route("/api/v1") {
        // Auth routes (public)
        registerAuthRoutes(redisService)
        route("/medicines") {
            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val subcategoryId = call.request.queryParameters["subcategoryId"]?.let { UUID.fromString(it) }

                try {
                    val result = transaction {
                        val baseQuery = if (subcategoryId != null) {
                            Medicines.selectAll().where { Medicines.subcategoryId eq subcategoryId }
                        } else {
                            Medicines.selectAll()
                        }

                        val total = baseQuery.count()
                        val medicines = baseQuery
                            .orderBy(Medicines.titleEn)
                            .limit(pageSize, (page * pageSize).toLong())
                            .map { row ->
                                MedicineDto(
                                    id = row[Medicines.id].toString(),
                                    titleEn = row[Medicines.titleEn],
                                    titleAr = row[Medicines.titleAr],
                                    descriptionAr = row[Medicines.descriptionAr],
                                    descriptionEn = row[Medicines.descriptionEn],
                                    imageUrl = row[Medicines.imageUrl],
                                    price = row[Medicines.price]?.toDouble(),
                                    manufacturer = row[Medicines.manufacturer],
                                    requiresPrescription = row[Medicines.requiresPrescription],
                                    isActive = row[Medicines.isActive],
                                    subcategoryNameAr = null,
                                    subcategoryNameEn = null
                                )
                            }

                        PaginatedResult(
                            items = medicines,
                            total = total,
                            page = page,
                            pageSize = pageSize,
                            totalPages = if (total > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
                        )
                    }
                    call.respond(ApiResponse(success = true, data = result))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            get("/search") {
                val query = call.request.queryParameters["q"] ?: ""
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                if (query.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Search query required"))
                    return@get
                }

                try {
                    val result = transaction {
                        val searchPattern = "%$query%"
                        val baseQuery = Medicines.selectAll().where {
                            Medicines.titleEn.like(searchPattern) or
                            Medicines.titleAr.like(searchPattern) or
                            (Medicines.descriptionEn like searchPattern) or
                            (Medicines.descriptionAr like searchPattern) or
                            (Medicines.manufacturer like searchPattern)
                        }

                        val total = baseQuery.count()
                        val medicines = baseQuery
                            .orderBy(Medicines.titleEn)
                            .limit(pageSize, (page * pageSize).toLong())
                            .map { row ->
                                MedicineDto(
                                    id = row[Medicines.id].toString(),
                                    titleEn = row[Medicines.titleEn],
                                    titleAr = row[Medicines.titleAr],
                                    descriptionAr = row[Medicines.descriptionAr],
                                    descriptionEn = row[Medicines.descriptionEn],
                                    imageUrl = row[Medicines.imageUrl],
                                    price = row[Medicines.price]?.toDouble(),
                                    manufacturer = row[Medicines.manufacturer],
                                    requiresPrescription = row[Medicines.requiresPrescription],
                                    isActive = row[Medicines.isActive],
                                    subcategoryNameAr = null,
                                    subcategoryNameEn = null
                                )
                            }

                        PaginatedResult(
                            items = medicines,
                            total = total,
                            page = page,
                            pageSize = pageSize,
                            totalPages = if (total > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
                        )
                    }
                    call.respond(ApiResponse(success = true, data = result))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            get("/{id}") {
                val idStr = call.parameters["id"]
                val id = idStr?.let { UUID.fromString(it) }
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid ID"))
                    return@get
                }

                try {
                    val medicine = transaction {
                        Medicines.selectAll().where { Medicines.id eq id }
                            .map { row ->
                                MedicineDto(
                                    id = row[Medicines.id].toString(),
                                    titleEn = row[Medicines.titleEn],
                                    titleAr = row[Medicines.titleAr],
                                    descriptionAr = row[Medicines.descriptionAr],
                                    descriptionEn = row[Medicines.descriptionEn],
                                    imageUrl = row[Medicines.imageUrl],
                                    price = row[Medicines.price]?.toDouble(),
                                    manufacturer = row[Medicines.manufacturer],
                                    requiresPrescription = row[Medicines.requiresPrescription],
                                    isActive = row[Medicines.isActive],
                                    subcategoryNameAr = null,
                                    subcategoryNameEn = null
                                )
                            }.firstOrNull()
                    }

                    if (medicine != null) {
                        call.respond(ApiResponse(success = true, data = medicine))
                    } else {
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Medicine not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }
        }

        route("/medicines/categories") {
            get {
                try {
                    val categories = transaction {
                        Categories.selectAll().orderBy(Categories.nameEn).map { row ->
                            CategoryDto(
                                id = row[Categories.id].toString(),
                                nameEn = row[Categories.nameEn],
                                nameAr = row[Categories.nameAr],
                                iconUrl = row[Categories.iconUrl],
                                displayOrder = row[Categories.displayOrder],
                                isActive = row[Categories.isActive],
                                subcategories = emptyList()
                            )
                        }
                    }
                    call.respond(ApiResponse(success = true, data = categories))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }
        }

        route("/pharmacies") {
            get("/nearby") {
                val lat = call.request.queryParameters["lat"]?.toDoubleOrNull()
                val lng = call.request.queryParameters["lng"]?.toDoubleOrNull()
                val radius = call.request.queryParameters["radius"]?.toDoubleOrNull() ?: 10.0

                if (lat == null || lng == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Latitude and longitude required"))
                    return@get
                }

                try {
                    val pharmacies = transaction {
                        Pharmacies.selectAll().map { row ->
                            val plat = row[Pharmacies.latitude]
                            val plng = row[Pharmacies.longitude]
                            val d = haversineKm(lat, lng, plat, plng)
                            PharmacyDto(
                                id = row[Pharmacies.id].toString(),
                                name = row[Pharmacies.name],
                                description = row[Pharmacies.description],
                                imageUrl = row[Pharmacies.imageUrl],
                                address = row[Pharmacies.address],
                                city = row[Pharmacies.city],
                                latitude = plat,
                                longitude = plng,
                                phone = row[Pharmacies.phone],
                                isOpen = row[Pharmacies.isOpen],
                                ratingAvg = row[Pharmacies.ratingAvg]?.toDouble() ?: 0.0,
                                totalRatings = row[Pharmacies.totalRatings] ?: 0,
                                isVerified = row[Pharmacies.isVerified],
                                distanceKm = d
                            )
                        }.filter { (it.distanceKm ?: Double.MAX_VALUE) <= radius }
                            .sortedBy { it.distanceKm }
                    }
                    call.respond(ApiResponse(success = true, data = pharmacies))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                val city = call.request.queryParameters["city"]

                try {
                    val result = transaction {
                        var query = Pharmacies.selectAll()

                        if (city != null) {
                            query = query.where { Pharmacies.city eq city }
                        }

                        val total = query.count()
                        val pharmacies = query
                            .orderBy(Pharmacies.name)
                            .limit(pageSize, (page * pageSize).toLong())
                            .map { row ->
                                PharmacyDto(
                                    id = row[Pharmacies.id].toString(),
                                    name = row[Pharmacies.name],
                                    description = row[Pharmacies.description],
                                    imageUrl = row[Pharmacies.imageUrl],
                                    address = row[Pharmacies.address],
                                    city = row[Pharmacies.city],
                                    latitude = row[Pharmacies.latitude],
                                    longitude = row[Pharmacies.longitude],
                                    phone = row[Pharmacies.phone],
                                    isOpen = row[Pharmacies.isOpen],
                                    ratingAvg = row[Pharmacies.ratingAvg]?.toDouble() ?: 0.0,
                                    totalRatings = row[Pharmacies.totalRatings] ?: 0,
                                    isVerified = row[Pharmacies.isVerified]
                                )
                            }

                        PaginatedResult(
                            items = pharmacies,
                            total = total,
                            page = page,
                            pageSize = pageSize,
                            totalPages = if (total > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
                        )
                    }
                    call.respond(ApiResponse(success = true, data = result))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }
            get("/pharmacies/{id}/ratings") {
                val pharmacyIdStr = call.parameters["id"]
                if (pharmacyIdStr == null || !SecurityUtils.isValidUuid(pharmacyIdStr)) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid pharmacy ID"))
                    return@get
                }
                val pharmacyId = UUID.fromString(pharmacyIdStr)
                try {
                    val ratings = transaction {
                        val pharmacyExists = Pharmacies.selectAll().where { Pharmacies.id eq pharmacyId }.singleOrNull() != null
                        if (!pharmacyExists) return@transaction null
                        (Ratings innerJoin Users).selectAll().where { Ratings.pharmacyId eq pharmacyId }
                            .orderBy(Ratings.createdAt, SortOrder.DESC)
                            .map { row ->
                                RatingDto(
                                    id = row[Ratings.id].toString(),
                                    userId = row[Ratings.userId].toString(),
                                    userName = row[Users.fullName],
                                    rating = row[Ratings.rating],
                                    comment = row[Ratings.comment],
                                    createdAt = row[Ratings.createdAt].toString()
                                )
                            }
                    }
                    if (ratings == null) {
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Pharmacy not found"))
                    } else {
                        call.respond(ApiResponse(success = true, data = ratings))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }
        }
    }

    // Protected routes
    authenticate("jwt-auth") {
        route("/api/v1") {
            route("/user") {
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject

                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@get
                    }
                    val userUuid = UUID.fromString(userId)

                    try {
                        val user = transaction {
                            Users.selectAll().where { Users.id eq userUuid }.firstOrNull()?.let { row ->
                                UserDto(
                                    id = row[Users.id].toString(),
                                    email = row[Users.email],
                                    fullName = row[Users.fullName],
                                    phone = row[Users.phone],
                                    avatarUrl = row[Users.avatarUrl],
                                    role = row[Users.role],
                                    isVerified = row[Users.isVerified],
                                    createdAt = row[Users.createdAt].toString()
                                )
                            }
                        }

                        if (user != null) {
                            call.respond(ApiResponse(success = true, data = user))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "User not found"))
                         }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
                put {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@put
                    }
                    val userUuid = UUID.fromString(userId)
                    try {
                        val request = Json { ignoreUnknownKeys = true }.decodeFromString<UpdateUserRequest>(call.receiveText())
                        val updated = transaction {
                            val existing = Users.selectAll().where { Users.id eq userUuid }.singleOrNull() ?: return@transaction null
                            Users.update({ Users.id eq userUuid }) {
                                if (request.fullName != null && request.fullName.isNotBlank()) it[Users.fullName] = request.fullName.trim()
                                if (request.phone != null) it[Users.phone] = request.phone.trim()
                                if (request.avatarUrl != null) it[Users.avatarUrl] = request.avatarUrl
                                it[Users.updatedAt] = Instant.now()
                            }
                            Users.selectAll().where { Users.id eq userUuid }.first().let { row ->
                                UserDto(
                                    id = row[Users.id].toString(),
                                    email = row[Users.email],
                                    fullName = row[Users.fullName],
                                    phone = row[Users.phone],
                                    avatarUrl = row[Users.avatarUrl],
                                    role = row[Users.role],
                                    isVerified = row[Users.isVerified],
                                    createdAt = row[Users.createdAt].toString()
                                )
                            }
                        }
                        if (updated != null) call.respond(ApiResponse(success = true, data = updated))
                        else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "User not found"))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
                put("/password") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@put
                    }
                    val userUuid = UUID.fromString(userId)
                    try {
                        val request = Json { ignoreUnknownKeys = true }.decodeFromString<UpdatePasswordRequest>(call.receiveText())
                        val strengthErrors = SecurityUtils.validatePasswordStrength(request.newPassword)
                        if (strengthErrors.isNotEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = strengthErrors.joinToString("; ")))
                            return@put
                        }
                        val result = transaction {
                            val row = Users.selectAll().where { Users.id eq userUuid }.singleOrNull() ?: return@transaction null
                            val storedHash = row[Users.passwordHash]
                            if (!BCrypt.checkpw(request.oldPassword, storedHash)) return@transaction false
                            val newHash = BCrypt.hashpw(request.newPassword, BCrypt.gensalt())
                            Users.update({ Users.id eq userUuid }) {
                                it[Users.passwordHash] = newHash
                                it[Users.updatedAt] = Instant.now()
                            }
                            true
                        }
                        when (result) {
                            null -> call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "User not found"))
                            false -> call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(success = false, error = "Old password incorrect"))
                            true -> call.respond(ApiResponse<Unit>(success = true, message = "Password updated"))
                            else -> call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Unknown error"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }
            route("/users") {
                put("/change-password") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@put
                    }
                    val userUuid = UUID.fromString(userId)
                    try {
                        val request = Json { ignoreUnknownKeys = true }.decodeFromString<UpdatePasswordRequest>(call.receiveText())
                        val strengthErrors = SecurityUtils.validatePasswordStrength(request.newPassword)
                        if (strengthErrors.isNotEmpty()) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = strengthErrors.joinToString("; ")))
                            return@put
                        }
                        val result = transaction {
                            val row = Users.selectAll().where { Users.id eq userUuid }.singleOrNull() ?: return@transaction null
                            val storedHash = row[Users.passwordHash]
                            if (!BCrypt.checkpw(request.oldPassword, storedHash)) return@transaction false
                            val newHash = BCrypt.hashpw(request.newPassword, BCrypt.gensalt())
                            Users.update({ Users.id eq userUuid }) {
                                it[Users.passwordHash] = newHash
                                it[Users.updatedAt] = Instant.now()
                            }
                            true
                        }
                        when (result) {
                            null -> call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "User not found"))
                            false -> call.respond(HttpStatusCode.Unauthorized, ApiResponse<Unit>(success = false, error = "Old password incorrect"))
                            true -> call.respond(ApiResponse<Unit>(success = true, message = "Password updated"))
                            else -> call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Unknown error"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }

            route("/prescriptions") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject

                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@post
                    }

                    try {
                        val multipart = call.receiveMultipart()
                        var notes: String? = null
                        var pharmacyId: UUID? = null
                        var imageData: ByteArray? = null
                        var imageContentType = "image/jpeg"

                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> {
                                    when (part.name) {
                                        "notes" -> notes = part.value
                                        "selectedPharmacyId" -> pharmacyId = part.value?.let { UUID.fromString(it) }
                                    }
                                }
                                is PartData.FileItem -> {
                                    if (part.name == "image") {
                                        val bytes = part.streamProvider().readBytes()
                                        imageData = bytes
                                        imageContentType = part.contentType?.toString() ?: "image/jpeg"
                                    }
                                }
                                else -> {}
                            }
                            part.dispose()
                        }

                        if (imageData == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Image required"))
                            return@post
                        }

                        val timestamp = System.currentTimeMillis()
                        val objectName = "prescriptions/$userId/${timestamp}_prescription.jpg"

                        val uploadResult = minioService.uploadFile(
                            bucket = "prescriptions",
                            objectName = objectName,
                            inputStream = java.io.ByteArrayInputStream(imageData!!),
                            contentType = imageContentType
                        )

                        val imageUrl = uploadResult.getOrNull()
                            ?: throw Exception("Failed to upload image")

                        val prescriptionResult = prescriptionService.createPrescription(userId, notes, pharmacyId, imageUrl)

                        if (eilajiPlusService != null) {
                            try {
                                eilajiPlusService.sendPrescriptionToEilajiDoctor(UUID.fromString(prescriptionResult.id), userId)
                            } catch (e: Exception) {
                                // Log error but don't fail
                            }
                        }

                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = prescriptionResult))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject

                    val status = call.request.queryParameters["status"]
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                    try {
                        val result = prescriptionService.getPrescriptionsForUser(userId, status, page, pageSize)
                        call.respond(ApiResponse(success = true, data = result))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                get("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    val idStr = call.parameters["id"]
                    val id = idStr?.let { UUID.fromString(it) }

                    if (id == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid ID"))
                        return@get
                    }

                    try {
                        val prescription = prescriptionService.getPrescriptionById(id)

                        if (prescription != null && prescription.userId == userId) {
                            call.respond(ApiResponse(success = true, data = prescription))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Prescription not found"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }

            route("/chats") {
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

                    try {
                        val result = chatService.getChatsForUser(userId, page, pageSize)
                        call.respond(ApiResponse(success = true, data = result))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject

                    try {
                        val request = Json.decodeFromString<CreateChatRequest>(call.receiveText())
                        val prescriptionId = request.prescriptionId?.let { UUID.fromString(it) }
                        val pharmacyId = request.pharmacyId?.let { UUID.fromString(it) }
                        val chat = chatService.createChat(userId, prescriptionId, pharmacyId)
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = chat))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                get("/{chatId}/messages") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    val chatIdStr = call.parameters["chatId"]
                    val chatId = chatIdStr?.let { UUID.fromString(it) }

                    if (chatId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid chat ID"))
                        return@get
                    }

                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 0
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 50

                    try {
                        val result = messageService.getMessagesForChat(chatId, userId, page, pageSize)
                        call.respond(ApiResponse(success = true, data = result))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                post("/{chatId}/read") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    val chatIdStr = call.parameters["chatId"]
                    val chatId = chatIdStr?.let { UUID.fromString(it) }

                    if (chatId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid chat ID"))
                        return@post
                    }

                    try {
                        val count = messageService.markChatAsRead(chatId, userId)
                        call.respond(ApiResponse(success = true, data = mapOf("markedCount" to count)))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }

            route("/presence") {
                get("/online") {
                    try {
                        val onlineUsers = redisService.getOnlineUsers()
                        call.respond(ApiResponse(success = true, data = mapOf("onlineUsers" to onlineUsers.toList())))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                get("/{userId}") {
                    val targetUserId = call.parameters["userId"]

                    if (targetUserId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "User ID required"))
                        return@get
                    }

                    try {
                        val isOnline = redisService.isUserOnline(targetUserId)
                        call.respond(ApiResponse(success = true, data = mapOf("userId" to targetUserId, "isOnline" to isOnline)))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }

            route("/eilaji-plus/webhook") {
                post {
                    try {
                        if (eilajiPlusService == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Eilaji-Plus not configured"))
                            return@post
                        }
                        val webhookRequest = Json.decodeFromString<EilajiPlusWebhookRequest>(call.receiveText())
                        val success = eilajiPlusService.processWebhook(webhookRequest)

                        if (success) {
                            call.respond(ApiResponse<Unit>(success = true, message = "Webhook processed successfully"))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Failed to process webhook"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }

            webSocket("/ws/chat") {
                val token = call.request.queryParameters["token"]

                if (token.isNullOrBlank()) {
                    close(CloseReason(1008, "Authentication token required"))
                    return@webSocket
                }

                try {
                    val decodedJWT = JwtConfig.verify(token)
                    val userId = decodedJWT.getClaim("user_id").asString()

                    if (userId.isNotBlank()) {
                        val sessionManager = WebSocketSessionManager(messageService, chatService, redisService)
                        val webSocketController = WebSocketController(sessionManager, redisService, messageService)
                        webSocketController.handleWebSocketSession(this, userId)
                    } else {
                        close(CloseReason(1008, "Invalid token"))
                    }
                } catch (e: Exception) {
                    close(CloseReason(1008, "Authentication failed"))
                }
            }

            route("/orders") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject

                    try {
                        val request = Json.decodeFromString<OrderService.OrderCreateRequest>(call.receiveText())
                        val order = orderService.createOrder(request, userId)

                        if (order != null) {
                            call.respond(HttpStatusCode.Created, ApiResponse<OrderService.OrderResult>(success = true, data = order))
                        } else {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Failed to create order. Ensure prescription is accepted."))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    val userRole = UserRole.valueOf(principal.payload.getClaim("role").asString())

                    try {
                        val orders = orderService.getUserOrders(userId, userRole)
                        call.respond(ApiResponse(success = true, data = orders))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                get("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    val userRole = UserRole.valueOf(principal.payload.getClaim("role").asString())
                    val orderId = call.parameters["id"]

                    if (orderId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid order ID"))
                        return@get
                    }

                    try {
                        val order = orderService.getOrderById(orderId, userId, userRole)
                        if (order != null) {
                            call.respond(ApiResponse(success = true, data = order))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Order not found or access denied"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                put("/{id}/status") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    val userRole = UserRole.valueOf(principal.payload.getClaim("role").asString())
                    val orderId = call.parameters["id"]

                    if (orderId == null) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid order ID"))
                        return@put
                    }

                    if (userRole != UserRole.PHARMACIST && userRole != UserRole.ADMIN) {
                        call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Only pharmacists and admins can update order status"))
                        return@put
                    }

                    try {
                        val request = Json.decodeFromString<OrderService.OrderUpdateStatusRequest>(call.receiveText())
                        val order = orderService.updateOrderStatus(orderId, request.status, request.paymentStatus, userId, userRole)

                        if (order != null) {
                            call.respond(ApiResponse(success = true, data = order))
                        } else {
                            call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Order not found or access denied"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }

            route("/favorites") {
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@get
                    }
                    val userUuid = UUID.fromString(userId)
                    try {
                        val favorites = transaction {
                            Favorites.selectAll().where { Favorites.userId eq userUuid }
                                .orderBy(Favorites.createdAt, SortOrder.DESC)
                                .map { row ->
                                    val medId = row[Favorites.medicineId]
                                    val pharmId = row[Favorites.pharmacyId]
                                    val medRow = medId?.let { Medicines.selectAll().where { Medicines.id eq it }.singleOrNull() }
                                    val pharmRow = pharmId?.let { Pharmacies.selectAll().where { Pharmacies.id eq it }.singleOrNull() }
                                    val type = when {
                                        medId != null && pharmId != null -> "BOTH"
                                        medId != null -> "MEDICINE"
                                        else -> "PHARMACY"
                                    }
                                    FavoriteDto(
                                        id = row[Favorites.id].toString(),
                                        type = type,
                                        medicineId = medId?.toString(),
                                        medicineTitleEn = medRow?.get(Medicines.titleEn),
                                        medicineTitleAr = medRow?.get(Medicines.titleAr),
                                        pharmacyId = pharmId?.toString(),
                                        pharmacyName = pharmRow?.get(Pharmacies.name),
                                        createdAt = row[Favorites.createdAt].toString()
                                    )
                                }
                        }
                        call.respond(ApiResponse(success = true, data = favorites))
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@post
                    }
                    val userUuid = UUID.fromString(userId)
                    try {
                        val req = Json { ignoreUnknownKeys = true }.decodeFromString<CreateFavoriteRequest>(call.receiveText())
                        if (req.medicineId == null && req.pharmacyId == null) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "medicineId or pharmacyId required"))
                            return@post
                        }
                        if (req.medicineId != null && !SecurityUtils.isValidUuid(req.medicineId)) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid medicineId"))
                            return@post
                        }
                        if (req.pharmacyId != null && !SecurityUtils.isValidUuid(req.pharmacyId)) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid pharmacyId"))
                            return@post
                        }
                        val medUuid = req.medicineId?.let { UUID.fromString(it) }
                        val pharmUuid = req.pharmacyId?.let { UUID.fromString(it) }
                        val result = transaction {
                            if (medUuid != null) {
                                val exists = Medicines.selectAll().where { Medicines.id eq medUuid }.singleOrNull() != null
                                if (!exists) return@transaction null to "Medicine not found"
                            }
                            if (pharmUuid != null) {
                                val exists = Pharmacies.selectAll().where { Pharmacies.id eq pharmUuid }.singleOrNull() != null
                                if (!exists) return@transaction null to "Pharmacy not found"
                            }
                            val duplicate = Favorites.selectAll().where { Favorites.userId eq userUuid }.toList()
                                .any { it[Favorites.medicineId] == medUuid && it[Favorites.pharmacyId] == pharmUuid }
                            if (duplicate) return@transaction null to "Already favorited"
                            val id = UUID.randomUUID()
                            Favorites.insert {
                                it[Favorites.id] = id
                                it[Favorites.userId] = userUuid
                                it[Favorites.medicineId] = medUuid
                                it[Favorites.pharmacyId] = pharmUuid
                                it[Favorites.createdAt] = Instant.now()
                            }
                            val row = Favorites.selectAll().where { Favorites.id eq id }.single()
                            val medRow = medUuid?.let { Medicines.selectAll().where { Medicines.id eq it }.singleOrNull() }
                            val pharmRow = pharmUuid?.let { Pharmacies.selectAll().where { Pharmacies.id eq it }.singleOrNull() }
                            val type = when {
                                medUuid != null && pharmUuid != null -> "BOTH"
                                medUuid != null -> "MEDICINE"
                                else -> "PHARMACY"
                            }
                            FavoriteDto(
                                id = row[Favorites.id].toString(),
                                type = type,
                                medicineId = medUuid?.toString(),
                                medicineTitleEn = medRow?.get(Medicines.titleEn),
                                medicineTitleAr = medRow?.get(Medicines.titleAr),
                                pharmacyId = pharmUuid?.toString(),
                                pharmacyName = pharmRow?.get(Pharmacies.name),
                                createdAt = row[Favorites.createdAt].toString()
                            ) to null
                        }
                        if (result.first == null) {
                            val err = result.second ?: "Not found"
                            val status = when (err) {
                                "Already favorited" -> HttpStatusCode.Conflict
                                "Medicine not found", "Pharmacy not found" -> HttpStatusCode.NotFound
                                else -> HttpStatusCode.BadRequest
                            }
                            call.respond(status, ApiResponse<Unit>(success = false, error = err))
                        } else {
                            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.first))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }

                delete("/{id}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@delete
                    }
                    val favIdStr = call.parameters["id"]
                    if (favIdStr == null || !SecurityUtils.isValidUuid(favIdStr)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid favorite ID"))
                        return@delete
                    }
                    val favId = UUID.fromString(favIdStr)
                    val userUuid = UUID.fromString(userId)
                    try {
                        val deleted = transaction {
                            val fav = Favorites.selectAll().where { Favorites.id eq favId }.singleOrNull() ?: return@transaction null
                            if (fav[Favorites.userId] != userUuid) return@transaction false
                            Favorites.deleteWhere { Favorites.id eq favId } > 0
                        }
                        when (deleted) {
                            null -> call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Favorite not found"))
                            false -> call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Not your favorite"))
                            true -> call.respond(ApiResponse<Unit>(success = true, message = "Favorite deleted"))
                            else -> call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Unknown"))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }

            route("/ratings") {
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal!!.payload.subject
                    if (!validateUuid(userId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                        return@post
                    }
                    val userUuid = UUID.fromString(userId)
                    try {
                        val req = Json { ignoreUnknownKeys = true }.decodeFromString<CreateRatingRequest>(call.receiveText())
                        if (!SecurityUtils.isValidUuid(req.pharmacyId)) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid pharmacyId"))
                            return@post
                        }
                        if (req.rating !in 1..5) {
                            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "rating must be 1..5"))
                            return@post
                        }
                        val pharmUuid = UUID.fromString(req.pharmacyId)
                        val result = transaction {
                            val pharmacy = Pharmacies.selectAll().where { Pharmacies.id eq pharmUuid }.singleOrNull()
                                ?: return@transaction null
                            val existing = Ratings.selectAll().where { (Ratings.userId eq userUuid) and (Ratings.pharmacyId eq pharmUuid) }.singleOrNull()
                            if (existing != null) {
                                Ratings.update({ (Ratings.userId eq userUuid) and (Ratings.pharmacyId eq pharmUuid) }) {
                                    it[Ratings.rating] = req.rating
                                    it[Ratings.comment] = req.comment
                                }
                            } else {
                                Ratings.insert {
                                    it[Ratings.id] = UUID.randomUUID()
                                    it[Ratings.userId] = userUuid
                                    it[Ratings.pharmacyId] = pharmUuid
                                    it[Ratings.rating] = req.rating
                                    it[Ratings.comment] = req.comment
                                    it[Ratings.createdAt] = Instant.now()
                                }
                            }
                            val allRatings = Ratings.selectAll().where { Ratings.pharmacyId eq pharmUuid }.toList()
                            val total = allRatings.size
                            val avg = if (total > 0) allRatings.map { it[Ratings.rating].toDouble() }.average() else 0.0
                            Pharmacies.update({ Pharmacies.id eq pharmUuid }) {
                                it[Pharmacies.ratingAvg] = avg.toBigDecimal()
                                it[Pharmacies.totalRatings] = total
                                it[Pharmacies.updatedAt] = Instant.now()
                            }
                            val saved = Ratings.selectAll().where { (Ratings.userId eq userUuid) and (Ratings.pharmacyId eq pharmUuid) }.single()
                            val userName = Users.selectAll().where { Users.id eq userUuid }.singleOrNull()?.get(Users.fullName) ?: "User"
                            RatingDto(
                                id = saved[Ratings.id].toString(),
                                userId = userUuid.toString(),
                                userName = userName,
                                rating = saved[Ratings.rating],
                                comment = saved[Ratings.comment],
                                createdAt = saved[Ratings.createdAt].toString()
                            )
                        }
                        if (result == null) {
                            call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Pharmacy not found"))
                        } else {
                            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result))
                        }
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                    }
                }
            }
        }
    }
}

private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}
