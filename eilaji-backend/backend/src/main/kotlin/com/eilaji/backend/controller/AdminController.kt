package com.eilaji.backend.controller

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.*
import com.eilaji.backend.data.UserRole
import com.eilaji.backend.security.SecurityUtils
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

fun Route.adminRoutes() {
    route("/admin") {
        authenticate("jwt-auth") {
            get("/users") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())

                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@get
                }

                val roleFilter = call.request.queryParameters["role"]
                val verifiedFilter = call.request.queryParameters["verified"]?.toBooleanStrictOrNull()

                try {
                    val users = transaction {
                        val query = if (roleFilter != null) {
                            Users.selectAll().where { Users.role eq roleFilter }
                        } else {
                            Users.selectAll()
                        }

                        query.orderBy(Users.createdAt, SortOrder.DESC).map { row ->
                            mapOf(
                                "id" to row[Users.id].toString(),
                                "email" to row[Users.email],
                                "fullName" to row[Users.fullName],
                                "phone" to row[Users.phone],
                                "role" to row[Users.role],
                                "createdAt" to row[Users.createdAt].toString()
                            )
                        }
                    }
                    call.respond(ApiResponse(success = true, data = mapOf("users" to users)))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            put("/users/{id}/verify") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())

                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@put
                }

                val userIdStr = call.parameters["id"]
                if (userIdStr == null || !SecurityUtils.isValidUuid(userIdStr)) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid user ID"))
                    return@put
                }
                val userId = UUID.fromString(userIdStr)

                try {
                    val updated = transaction {
                        val user = Users.selectAll().where { Users.id eq userId }.singleOrNull()
                        if (user == null) {
                            null
                        } else {
                            Users.update({ Users.id eq userId }) {
                                it[Users.updatedAt] = Instant.now()
                                it[Users.isVerified] = true
                            }
                            Users.selectAll().where { Users.id eq userId }.firstOrNull()?.let { row ->
                                mapOf(
                                    "id" to row[Users.id].toString(),
                                    "email" to row[Users.email],
                                    "fullName" to row[Users.fullName],
                                    "role" to row[Users.role],
                                    "verified" to true
                                )
                            }
                        }
                    }

                    if (updated != null) {
                        call.respond(ApiResponse(success = true, data = updated))
                    } else {
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "User not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            post("/medicines") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@post
                }
                try {
                    val request = Json { ignoreUnknownKeys = true }.decodeFromString<CreateMedicineRequest>(call.receiveText())
                    if (request.titleEn.isBlank() || request.titleAr.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "titleEn and titleAr required"))
                        return@post
                    }
                    if (request.subcategoryId != null && !SecurityUtils.isValidUuid(request.subcategoryId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid subcategoryId"))
                        return@post
                    }
                    val subcatUuid = request.subcategoryId?.let { UUID.fromString(it) }
                    val id = UUID.randomUUID()
                    val dto = transaction {
                        Medicines.insert {
                            it[Medicines.id] = id
                            it[Medicines.titleEn] = request.titleEn
                            it[Medicines.titleAr] = request.titleAr
                            it[Medicines.descriptionEn] = request.descriptionEn
                            it[Medicines.descriptionAr] = request.descriptionAr
                            it[Medicines.imageUrl] = request.imageUrl
                            it[Medicines.price] = request.price?.toBigDecimal()
                            it[Medicines.manufacturer] = request.manufacturer
                            it[Medicines.requiresPrescription] = request.requiresPrescription
                            it[Medicines.isActive] = request.isActive
                            it[Medicines.subcategoryId] = subcatUuid
                        }
                        Medicines.selectAll().where { Medicines.id eq id }.first().let { row ->
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
                                isActive = row[Medicines.isActive]
                            )
                        }
                    }
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = dto))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            put("/medicines/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@put
                }
                val idStr = call.parameters["id"]
                if (idStr == null || !SecurityUtils.isValidUuid(idStr)) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid ID"))
                    return@put
                }
                val id = UUID.fromString(idStr)
                try {
                    val request = Json { ignoreUnknownKeys = true }.decodeFromString<UpdateMedicineRequest>(call.receiveText())
                    if (request.subcategoryId != null && !SecurityUtils.isValidUuid(request.subcategoryId)) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid subcategoryId"))
                        return@put
                    }
                    val dto = transaction {
                        val existing = Medicines.selectAll().where { Medicines.id eq id }.singleOrNull() ?: return@transaction null
                        Medicines.update({ Medicines.id eq id }) {
                            if (request.titleEn != null) it[Medicines.titleEn] = request.titleEn
                            if (request.titleAr != null) it[Medicines.titleAr] = request.titleAr
                            if (request.descriptionEn != null) it[Medicines.descriptionEn] = request.descriptionEn
                            if (request.descriptionAr != null) it[Medicines.descriptionAr] = request.descriptionAr
                            if (request.imageUrl != null) it[Medicines.imageUrl] = request.imageUrl
                            if (request.price != null) it[Medicines.price] = request.price.toBigDecimal()
                            if (request.manufacturer != null) it[Medicines.manufacturer] = request.manufacturer
                            if (request.requiresPrescription != null) it[Medicines.requiresPrescription] = request.requiresPrescription
                            if (request.isActive != null) it[Medicines.isActive] = request.isActive
                            if (request.subcategoryId != null) it[Medicines.subcategoryId] = UUID.fromString(request.subcategoryId)
                            it[Medicines.updatedAt] = Instant.now()
                        }
                        Medicines.selectAll().where { Medicines.id eq id }.first().let { row ->
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
                                isActive = row[Medicines.isActive]
                            )
                        }
                    }
                    if (dto != null) call.respond(ApiResponse(success = true, data = dto))
                    else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Medicine not found"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            delete("/medicines/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@delete
                }
                val idStr = call.parameters["id"]
                if (idStr == null || !SecurityUtils.isValidUuid(idStr)) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid ID"))
                    return@delete
                }
                val id = UUID.fromString(idStr)
                try {
                    val deleted = transaction {
                        val exists = Medicines.selectAll().where { Medicines.id eq id }.singleOrNull() != null
                        if (!exists) false else {
                            Medicines.deleteWhere { Medicines.id eq id } > 0
                        }
                    }
                    if (deleted) call.respond(ApiResponse<Unit>(success = true, message = "Medicine deleted"))
                    else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Medicine not found"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            post("/categories") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@post
                }
                try {
                    val request = Json { ignoreUnknownKeys = true }.decodeFromString<CreateCategoryRequest>(call.receiveText())
                    if (request.nameEn.isBlank() || request.nameAr.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "nameEn and nameAr required"))
                        return@post
                    }
                    val id = UUID.randomUUID()
                    val dto = transaction {
                        Categories.insert {
                            it[Categories.id] = id
                            it[Categories.nameEn] = request.nameEn
                            it[Categories.nameAr] = request.nameAr
                            it[Categories.iconUrl] = request.iconUrl
                            it[Categories.displayOrder] = request.displayOrder
                            it[Categories.isActive] = request.isActive
                        }
                        Categories.selectAll().where { Categories.id eq id }.first().let { row ->
                            CategoryDto(
                                id = row[Categories.id].toString(),
                                nameEn = row[Categories.nameEn],
                                nameAr = row[Categories.nameAr],
                                iconUrl = row[Categories.iconUrl],
                                displayOrder = row[Categories.displayOrder],
                                isActive = row[Categories.isActive]
                            )
                        }
                    }
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = dto))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            put("/categories/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@put
                }
                val idStr = call.parameters["id"]
                if (idStr == null || !SecurityUtils.isValidUuid(idStr)) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid ID"))
                    return@put
                }
                val id = UUID.fromString(idStr)
                try {
                    val request = Json { ignoreUnknownKeys = true }.decodeFromString<UpdateCategoryRequest>(call.receiveText())
                    val dto = transaction {
                        val exists = Categories.selectAll().where { Categories.id eq id }.singleOrNull() ?: return@transaction null
                        Categories.update({ Categories.id eq id }) {
                            if (request.nameEn != null) it[Categories.nameEn] = request.nameEn
                            if (request.nameAr != null) it[Categories.nameAr] = request.nameAr
                            if (request.iconUrl != null) it[Categories.iconUrl] = request.iconUrl
                            if (request.displayOrder != null) it[Categories.displayOrder] = request.displayOrder
                            if (request.isActive != null) it[Categories.isActive] = request.isActive
                        }
                        Categories.selectAll().where { Categories.id eq id }.first().let { row ->
                            CategoryDto(
                                id = row[Categories.id].toString(),
                                nameEn = row[Categories.nameEn],
                                nameAr = row[Categories.nameAr],
                                iconUrl = row[Categories.iconUrl],
                                displayOrder = row[Categories.displayOrder],
                                isActive = row[Categories.isActive]
                            )
                        }
                    }
                    if (dto != null) call.respond(ApiResponse(success = true, data = dto))
                    else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Category not found"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            delete("/categories/{id}") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@delete
                }
                val idStr = call.parameters["id"]
                if (idStr == null || !SecurityUtils.isValidUuid(idStr)) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid ID"))
                    return@delete
                }
                val id = UUID.fromString(idStr)
                try {
                    val deleted = transaction {
                        val exists = Categories.selectAll().where { Categories.id eq id }.singleOrNull() != null
                        if (!exists) false else {
                            Categories.deleteWhere { Categories.id eq id } > 0
                        }
                    }
                    if (deleted) call.respond(ApiResponse<Unit>(success = true, message = "Category deleted"))
                    else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Category not found"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            put("/pharmacies/{id}/verify") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@put
                }
                val idStr = call.parameters["id"]
                if (idStr == null || !SecurityUtils.isValidUuid(idStr)) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid pharmacy ID"))
                    return@put
                }
                val id = UUID.fromString(idStr)
                try {
                    val result = transaction {
                        val pharmacy = Pharmacies.selectAll().where { Pharmacies.id eq id }.singleOrNull() ?: return@transaction null
                        Pharmacies.update({ Pharmacies.id eq id }) {
                            it[Pharmacies.isVerified] = true
                            it[Pharmacies.updatedAt] = Instant.now()
                        }
                        Pharmacies.selectAll().where { Pharmacies.id eq id }.first().let { row ->
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
                    }
                    if (result != null) call.respond(ApiResponse(success = true, data = result))
                    else call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Pharmacy not found"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            get("/prescriptions") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())

                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@get
                }

                val statusFilter = call.request.queryParameters["status"]

                try {
                    val prescriptions = transaction {
                        val query = if (statusFilter != null) {
                            Prescriptions.selectAll().where { Prescriptions.status eq statusFilter }
                        } else {
                            Prescriptions.selectAll()
                        }

                        query.orderBy(Prescriptions.createdAt, SortOrder.DESC).map { row ->
                            mapOf(
                                "id" to row[Prescriptions.id].toString(),
                                "userId" to row[Prescriptions.patientUserId].toString(),
                                "pharmacyId" to row[Prescriptions.selectedPharmacyId]?.toString(),
                                "imageUrl" to row[Prescriptions.imageUrl],
                                "status" to row[Prescriptions.status],
                                "quotedPrice" to row[Prescriptions.quotedPrice]?.toDouble(),
                                "createdAt" to row[Prescriptions.createdAt].toString()
                            )
                        }
                    }
                    call.respond(ApiResponse(success = true, data = mapOf("prescriptions" to prescriptions)))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }

            get("/analytics") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())

                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@get
                }

                try {
                    val stats = transaction {
                        val totalUsers = Users.selectAll().count()
                        val totalPharmacies = Pharmacies.selectAll().count()
                        val totalPrescriptions = Prescriptions.selectAll().count()
                        val totalOrders = Orders.selectAll().count()

                        val revenue = Orders.selectAll().where { Orders.paymentStatus eq "PAID" }
                            .sumOf { it[Orders.totalAmount].toDouble() }

                        mapOf(
                            "totalUsers" to totalUsers,
                            "totalPharmacies" to totalPharmacies,
                            "totalPrescriptions" to totalPrescriptions,
                            "totalOrders" to totalOrders,
                            "totalRevenue" to revenue
                        )
                    }
                    call.respond(ApiResponse(success = true, data = stats))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = e.message))
                }
            }
        }
    }
}
