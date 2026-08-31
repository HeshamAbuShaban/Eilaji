package com.eilaji.backend.controller

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.eilaji.backend.data.Users
import com.eilaji.backend.data.UserRole
import com.eilaji.backend.dto.*
import com.eilaji.backend.security.AuditService
import com.eilaji.backend.security.JwtConfig
import com.eilaji.backend.security.SecurityUtils
import com.eilaji.backend.service.RedisService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

val json = Json { ignoreUnknownKeys = true }

fun Route.registerAuthRoutes(redisService: RedisService) {
    post("/auth/register") {
        try {
            val request = try {
                json.decodeFromString<RegisterRequest>(call.receiveText())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid JSON: ${e.message}"))
                return@post
            }

            if (request.email.isBlank() || request.password.isBlank() || request.fullName.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Email, password, and full name are required"))
                return@post
            }

            if (!SecurityUtils.validateEmail(request.email)) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid email format"))
                return@post
            }

            val passwordErrors = SecurityUtils.validatePasswordStrength(request.password)
            if (passwordErrors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>(passwordErrors.joinToString("; ")))
                return@post
            }

            val role = try {
                UserRole.valueOf(request.role.uppercase())
            } catch (e: Exception) {
                UserRole.PATIENT
            }

            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())

            val existingUser = transaction {
                Users.selectAll().where { Users.email eq request.email.lowercase().trim() }.firstOrNull()
            }
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, ApiResponse.error<String>("Email already registered"))
                return@post
            }

            val userId = transaction {
                Users.insert {
                    it[Users.id] = UUID.randomUUID()
                    it[Users.email] = request.email.lowercase().trim()
                    it[Users.passwordHash] = passwordHash
                    it[Users.fullName] = request.fullName.trim()
                    it[Users.phone] = request.phone?.trim()
                    it[Users.role] = role.name
                    it[Users.isVerified] = false
                    it[Users.createdAt] = Instant.now()
                } get Users.id
            }

            val user = transaction {
                Users.selectAll().where { Users.id eq userId }.first()
            }

            val userDto = UserDto(
                id = user[Users.id],
                email = user[Users.email],
                fullName = user[Users.fullName],
                phone = user[Users.phone],
                avatarUrl = user[Users.avatarUrl],
                role = user[Users.role],
                isVerified = user[Users.isVerified],
                createdAt = user[Users.createdAt].toString()
            )

            val token = JwtConfig.createJWT(userDto.id.toString(), userDto.fullName, userDto.role, userDto.email)
            val refreshToken = JwtConfig.createRefreshToken(userDto.id.toString())

            AuditService.logEvent(
                eventType = AuditService.EventType.USER_CREATED,
                userId = userDto.id.toString(),
                description = "User registered: ${userDto.email}"
            )

            call.respond(
                ApiResponse.success(
                    data = AuthResponse(
                        accessToken = token,
                        refreshToken = refreshToken,
                        expiresIn = JwtConfig.expiresIn,
                        user = userDto
                    )
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Registration failed: ${e.message}"))
        }
    }

    post("/auth/login") {
        try {
            val request = try {
                json.decodeFromString<LoginRequest>(call.receiveText())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid JSON: ${e.message}"))
                return@post
            }

            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Email and password are required"))
                return@post
            }

            val user = transaction {
                Users.selectAll().where {
                    Users.email eq request.email.lowercase().trim()
                }.firstOrNull()
            }

            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid credentials"))
                return@post
            }

            val storedHash = user[Users.passwordHash]
            if (!BCrypt.checkpw(request.password, storedHash)) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid credentials"))
                return@post
            }

            val userDto = UserDto(
                id = user[Users.id],
                email = user[Users.email],
                fullName = user[Users.fullName],
                phone = user[Users.phone],
                avatarUrl = user[Users.avatarUrl],
                role = user[Users.role],
                isVerified = user[Users.isVerified],
                createdAt = user[Users.createdAt].toString()
            )

            val token = JwtConfig.createJWT(userDto.id.toString(), userDto.fullName, userDto.role, userDto.email)
            val refreshToken = JwtConfig.createRefreshToken(userDto.id.toString())

            AuditService.logLoginSuccess(
                userId = userDto.id.toString(),
                ipAddress = call.request.header("X-Forwarded-For") ?: call.request.local.remoteHost,
                userAgent = call.request.header("User-Agent")
            )

            call.respond(
                ApiResponse.success(
                    data = AuthResponse(
                        accessToken = token,
                        refreshToken = refreshToken,
                        expiresIn = JwtConfig.expiresIn,
                        user = userDto
                    )
                )
            )

        } catch (e: Exception) {
            e.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Login failed: ${e.message}"))
        }
    }

    post("/auth/refresh") {
        try {
            val request = try {
                json.decodeFromString<RefreshTokenRequest>(call.receiveText())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid JSON: ${e.message}"))
                return@post
            }

            val principal = call.principal<JWTPrincipal>()
            if (principal == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid token"))
                return@post
            }

            val userId = principal.payload.getClaim("user_id").asString()
            val role = principal.payload.getClaim("role").asString()

            val newToken = JwtConfig.createJWT(userId, principal.payload.getClaim("fullName").asString(), role, principal.payload.getClaim("email").asString())
            val newRefreshToken = JwtConfig.createRefreshToken(userId)

            call.respond(
                ApiResponse.success(
                    data = AuthResponse(
                        accessToken = newToken,
                        refreshToken = newRefreshToken,
                        expiresIn = JwtConfig.expiresIn,
                        user = null
                    ),
                    message = "Token refreshed successfully"
                )
            )

        } catch (e: Exception) {
            call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Token refresh failed: ${e.message}"))
        }
    }

    authenticate("jwt-auth") {
        post("/auth/logout") {
            val principal = call.principal<JWTPrincipal>()
            if (principal != null) {
                val token = call.request.header("Authorization")?.removePrefix("Bearer ")
                if (token != null) {
                    JwtConfig.blacklistToken(token)
                }
            }
            call.respond(ApiResponse.success(Unit, message = "Logged out successfully"))
        }
    }
}
