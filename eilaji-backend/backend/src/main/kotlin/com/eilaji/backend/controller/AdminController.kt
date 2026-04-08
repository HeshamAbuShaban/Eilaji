package com.eilaji.backend.controller

import com.eilaji.backend.data.*
import com.eilaji.backend.dto.ApiResponse
import com.eilaji.backend.model.UserRole
import io.ktor.http.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun Route.adminRoutes() {
    route("/admin") {
        authenticate("jwt-auth") {
            // List all users with filters
            get("/users") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Map<String, Any>?>(success = false, error = "Admin access required"))
                    return@get
                }
                
                val roleFilter = call.request.queryParameters["role"]
                val verifiedFilter = call.request.queryParameters["verified"]?.toBooleanStrictOrNull()
                
                try {
                    val users = transaction {
                        val query = Users.selectAll()
                        
                        val filteredQuery = if (roleFilter != null) {
                            query.filter { Users.role eq roleFilter }
                        } else {
                            query
                        }
                        
                        filteredQuery.orderBy(Users.createdAt.desc()).map { row ->
                            mapOf(
                                "id" to row[Users.id],
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
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                }
            }
            
            // Verify pharmacist account
            put("/users/{id}/verify") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Map<String, Any>?>(success = false, error = "Admin access required"))
                    return@put
                }
                
                val userId = call.parameters["id"]
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Map<String, Any>?>(success = false, error = "User ID required"))
                    return@put
                }
                
                try {
                    val updated = transaction {
                        val user = Users.select { Users.id eq userId }.singleOrNull()
                        if (user == null) {
                            null
                        } else {
                            Users.update({ Users.id eq userId }) {
                                it[Users.updatedAt] = Instant.now()
                            }
                            Users.select { Users.id eq userId }.firstOrNull()?.let { row ->
                                mapOf(
                                    "id" to row[Users.id],
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
                        call.respond(HttpStatusCode.NotFound, ApiResponse<Map<String, Any>?>(success = false, error = "User not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                }
            }
            
            // List all prescriptions with status filter
            get("/prescriptions") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Map<String, Any>?>(success = false, error = "Admin access required"))
                    return@get
                }
                
                val statusFilter = call.request.queryParameters["status"]
                
                try {
                    val prescriptions = transaction {
                        val query = Prescriptions.selectAll()
                        
                        val filteredQuery = if (statusFilter != null) {
                            query.filter { Prescriptions.status eq statusFilter }
                        } else {
                            query
                        }
                        
                        filteredQuery.orderBy(Prescriptions.createdAt.desc()).map { row ->
                            mapOf(
                                "id" to row[Prescriptions.id],
                                "userId" to row[Prescriptions.userId],
                                "pharmacyId" to row[Prescriptions.pharmacyId],
                                "imageUrl" to row[Prescriptions.imageUrl],
                                "status" to row[Prescriptions.status],
                                "quotedPrice" to row[Prescriptions.quotedPrice]?.toDouble(),
                                "createdAt" to row[Prescriptions.createdAt].toString()
                            )
                        }
                    }
                    call.respond(ApiResponse(success = true, data = mapOf("prescriptions" to prescriptions)))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                }
            }
            
            // Analytics endpoint
            get("/analytics") {
                val principal = call.principal<JWTPrincipal>()
                val userRole = UserRole.valueOf(principal!!.payload.getClaim("role").asString())
                
                if (userRole != UserRole.ADMIN) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Map<String, Any>?>(success = false, error = "Admin access required"))
                    return@get
                }
                
                try {
                    val stats = transaction {
                        val totalUsers = Users.selectAll().count()
                        val totalPharmacies = Pharmacies.selectAll().count()
                        val totalPrescriptions = Prescriptions.selectAll().count()
                        val totalOrders = Orders.selectAll().count()
                        
                        val revenue = Orders.select(Orders.totalAmount).filter { Orders.paymentStatus eq PaymentStatus.PAID }
                            .sumByDouble { it[Orders.totalAmount].toDouble() }
                        
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
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Map<String, Any>?>(success = false, error = e.message))
                }
            }
        }
    }
}
