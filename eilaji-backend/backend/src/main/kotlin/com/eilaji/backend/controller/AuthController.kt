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
import org.mindrot.jbcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun Route.registerAuthRoutes(redisService: RedisService) {
    post("/auth/register") {
        try {
            val request = call.receive<RegisterRequest>()

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
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>(passwordErrors.joinToString(", ")))
                return@post
            }

            if (SecurityUtils.isCommonPassword(request.password)) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Password is too common. Please choose a stronger password"))
                return@post
            }

            val sanitizedEmail = SecurityUtils.limitLength(request.email.lowercase().trim(), 255, "email")
            val sanitizedFullName = SecurityUtils.limitLength(request.fullName.trim(), 100, "full name")

            val existingUser = transaction {
                Users.selectAll().where { Users.email eq sanitizedEmail }.firstOrNull()
            }

            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, ApiResponse.error<String>("Email already registered"))
                return@post
            }

            val salt = BCrypt.gensalt()
            val passwordHash = BCrypt.hashpw(request.password, salt)

            val role = try {
                UserRole.valueOf(request.role.uppercase())
            } catch (e: IllegalArgumentException) {
                UserRole.PATIENT
            }

            val userId = transaction {
                Users.insert {
                    it[Users.email] = request.email.lowercase().trim()
                    it[Users.passwordHash] = passwordHash
                    it[Users.fullName] = request.fullName.trim()
                    it[Users.phone] = request.phone?.trim()
                    it[Users.role] = role.name
                    it[Users.isVerified] = false
                } get Users.id
            }

            val accessToken = generateAccessToken(userId.toString(), role.name)
            val refreshToken = generateRefreshToken(userId.toString())

            redisService.storeInCache("refresh:$refreshToken", userId.toString(), JwtConfig.refreshExpiresIn)

            val userDto = transaction {
                Users.selectAll().where { Users.id eq userId }.firstOrNull()?.let { row ->
                    UserDto(
                        id = row[Users.id],
                        email = row[Users.email],
                        fullName = row[Users.fullName],
                        phone = row[Users.phone],
                        avatarUrl = row[Users.avatarUrl],
                        role = row[Users.role],
                        isVerified = row[Users.isVerified],
                        createdAt = row[Users.createdAt]
                    )
                }
            }

            AuditService.logEvent(
                eventType = AuditService.EventType.USER_CREATED,
                userId = userId.toString(),
                ipAddress = call.request.local.remoteHost,
                resourceType = "User",
                resourceId = userId.toString(),
                description = "User registered: ${request.email}"
            )

            call.respond(HttpStatusCode.Created, ApiResponse.success(
                AuthResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = JwtConfig.expiresIn,
                    user = userDto!!
                ),
                "User registered successfully"
            ))

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Registration failed: ${e.message}"))
        }
    }

    post("/auth/login") {
        try {
            val request = call.receive<LoginRequest>()

            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Email and password are required"))
                return@post
            }

            if (!SecurityUtils.validateEmail(request.email)) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid email format"))
                return@post
            }

            val sanitizedEmail = SecurityUtils.limitLength(request.email.lowercase().trim(), 255, "email")

            val user = transaction {
                Users.selectAll().where { Users.email eq sanitizedEmail }.firstOrNull()
            }

            if (user == null) {
                println("WARNING: Failed login attempt for non-existent email: $sanitizedEmail from IP: ${call.request.local.remoteHost}")
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid credentials"))
                return@post
            }

            val passwordHash = user[Users.passwordHash]
            if (!BCrypt.checkpw(request.password, passwordHash)) {
                AuditService.logLoginFailure(request.email, call.request.local.remoteHost, "Wrong password")
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid credentials"))
                return@post
            }

            if (!user[Users.isActive]) {
                AuditService.logLoginFailure(request.email, call.request.local.remoteHost, "Account deactivated")
                call.respond(HttpStatusCode.Forbidden, ApiResponse.error<String>("Account is deactivated"))
                return@post
            }

            val userId = user[Users.id].toString()
            val role = user[Users.role]

            val accessToken = generateAccessToken(userId, role)
            val refreshToken = generateRefreshToken(userId)

            redisService.storeInCache("refresh:$refreshToken", userId, JwtConfig.refreshExpiresIn)

            AuditService.logLoginSuccess(userId, call.request.local.remoteHost, call.request.headers["User-Agent"])
            redisService.setOnlineStatus(userId, true)

            val userDto = UserDto(
                id = user[Users.id],
                email = user[Users.email],
                fullName = user[Users.fullName],
                phone = user[Users.phone],
                avatarUrl = user[Users.avatarUrl],
                role = user[Users.role],
                isVerified = user[Users.isVerified],
                createdAt = user[Users.createdAt]
            )

            call.respond(ApiResponse.success(
                AuthResponse(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    expiresIn = JwtConfig.expiresIn,
                    user = userDto
                ),
                "Login successful"
            ))

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Login failed: ${e.message}"))
        }
    }

    post("/auth/refresh") {
        try {
            val request = call.receive<RefreshTokenRequest>()

            val userId = redisService.getFromCache("refresh:${request.refreshToken}")

            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid or expired refresh token"))
                return@post
            }

            val user = transaction {
                Users.selectAll().where { Users.id eq userId }.firstOrNull()
            }

            if (user == null || !user[Users.isActive]) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("User not found or inactive"))
                return@post
            }

            val role = user[Users.role]

            val newAccessToken = generateAccessToken(userId, role)

            call.respond(ApiResponse.success(
                mapOf("accessToken" to newAccessToken, "expiresIn" to JwtConfig.expiresIn),
                "Token refreshed successfully"
            ))

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Token refresh failed: ${e.message}"))
        }
    }

    post("/auth/logout") {
        try {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("user_id")?.asString()

            if (userId != null) {
                redisService.setOnlineStatus(userId, false)
            }

            call.respond(ApiResponse.success(mapOf("message" to "Logout successful")))

        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Logout failed: ${e.message}"))
        }
    }
}

private fun generateAccessToken(userId: String, role: String): String {
    return JWT.create()
        .withSubject(userId)
        .withClaim("user_id", userId)
        .withClaim("role", role)
        .withIssuer(JwtConfig.getIssuer())
        .withAudience(JwtConfig.getAudience())
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + JwtConfig.expiresIn))
        .sign(JwtConfig.getAlgorithm())
}

private fun generateRefreshToken(userId: String): String {
    return JWT.create()
        .withSubject(userId)
        .withClaim("user_id", userId)
        .withClaim("type", "refresh")
        .withIssuer(JwtConfig.getIssuer())
        .withAudience(JwtConfig.getAudience())
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + JwtConfig.refreshExpiresIn))
        .sign(JwtConfig.getAlgorithm())
}
