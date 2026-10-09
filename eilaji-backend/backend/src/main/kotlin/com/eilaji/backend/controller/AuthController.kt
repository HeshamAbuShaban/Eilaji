package com.eilaji.backend.controller

import com.eilaji.backend.data.PasswordResetTokens
import com.eilaji.backend.data.Users
import com.eilaji.backend.data.UserRole
import com.eilaji.backend.dto.*
import com.eilaji.backend.security.ApiException
import com.eilaji.backend.security.AppConfig
import com.eilaji.backend.security.AuditService
import com.eilaji.backend.security.JwtConfig
import com.eilaji.backend.security.SecurityUtils
import com.eilaji.backend.service.RedisService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.mindrot.jbcrypt.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

val json = Json { ignoreUnknownKeys = true }

fun Route.registerAuthRoutes(redisService: RedisService) {

    post("/auth/register") {
        try {
            val request = try {
                json.decodeFromString<RegisterRequest>(call.receiveText())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid request body"))
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

            // Self-registration can never create an elevated account.
            val role = try {
                val parsed = UserRole.valueOf(request.role.uppercase())
                if (parsed == UserRole.ADMIN || parsed == UserRole.PHARMACIST) UserRole.PATIENT else parsed
            } catch (e: Exception) {
                UserRole.PATIENT
            }

            val email = request.email.lowercase().trim()
            val existingUser = transaction {
                Users.selectAll().where { Users.email eq email }.firstOrNull()
            }
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, ApiResponse.error<String>("Email already registered"))
                return@post
            }

            val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt(AppConfig.bcryptCost))
            val userId = transaction {
                Users.insert {
                    it[Users.id] = UUID.randomUUID()
                    it[Users.email] = email
                    it[Users.passwordHash] = passwordHash
                    it[Users.fullName] = request.fullName.trim()
                    it[Users.phone] = request.phone?.trim()
                    it[Users.role] = role.name
                    it[Users.isVerified] = false
                    it[Users.createdAt] = Instant.now()
                } get Users.id
            }

            val user = transaction { Users.selectAll().where { Users.id eq userId }.first() }
            val userDto = user.toDto()

            AuditService.logEvent(
                eventType = AuditService.EventType.USER_CREATED,
                userId = userDto.id,
                description = "User registered"
            )

            call.respond(
                ApiResponse.success(
                    data = AuthResponse(
                        accessToken = JwtConfig.createJWT(userDto.id, userDto.fullName, userDto.role, userDto.email),
                        refreshToken = JwtConfig.createRefreshToken(userDto.id),
                        expiresIn = JwtConfig.expiresIn,
                        user = userDto
                    )
                )
            )
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            call.application.log.error("Registration failed", e)
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Registration failed"))
        }
    }

    post("/auth/login") {
        try {
            val request = try {
                json.decodeFromString<LoginRequest>(call.receiveText())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid request body"))
                return@post
            }
            if (request.email.isBlank() || request.password.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Email and password are required"))
                return@post
            }

            val ip = call.request.header("X-Forwarded-For") ?: call.request.local.remoteHost
            val user = transaction {
                Users.selectAll().where { Users.email eq request.email.lowercase().trim() }.firstOrNull()
            }

            if (user == null || !BCrypt.checkpw(request.password, user[Users.passwordHash])) {
                AuditService.logLoginFailure(
                    email = request.email.lowercase().trim(),
                    ipAddress = ip,
                    reason = if (user == null) "unknown email" else "wrong password"
                )
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid credentials"))
                return@post
            }
            if (!user[Users.isActive]) {
                AuditService.logLoginFailure(email = user[Users.email], ipAddress = ip, reason = "account deactivated")
                call.respond(HttpStatusCode.Forbidden, ApiResponse.error<String>("This account is deactivated"))
                return@post
            }

            val userDto = user.toDto()
            AuditService.logLoginSuccess(userId = userDto.id, ipAddress = ip, userAgent = call.request.header("User-Agent"))

            call.respond(
                ApiResponse.success(
                    data = AuthResponse(
                        accessToken = JwtConfig.createJWT(userDto.id, userDto.fullName, userDto.role, userDto.email),
                        refreshToken = JwtConfig.createRefreshToken(userDto.id),
                        expiresIn = JwtConfig.expiresIn,
                        user = userDto
                    )
                )
            )
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            call.application.log.error("Login failed", e)
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Login failed"))
        }
    }

    /**
     * Exchanges a refresh token for a new pair.
     * The refresh token is verified as a refresh token (separate audience), never as an access token.
     */
    post("/auth/refresh") {
        try {
            val request = try {
                json.decodeFromString<RefreshTokenRequest>(call.receiveText())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid request body"))
                return@post
            }
            if (request.refreshToken.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("refreshToken is required"))
                return@post
            }

            val decoded = try {
                JwtConfig.verifyRefresh(request.refreshToken)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid or expired refresh token"))
                return@post
            }

            val userId = decoded.getClaim("user_id").asString()
            val user = transaction { Users.selectAll().where { Users.id eq UUID.fromString(userId) }.firstOrNull() }
            if (user == null || !user[Users.isActive]) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Account is no longer active"))
                return@post
            }

            // Rotate: the used refresh token stops working.
            JwtConfig.revokeToken(request.refreshToken)

            val userDto = user.toDto()
            call.respond(
                ApiResponse.success(
                    data = AuthResponse(
                        accessToken = JwtConfig.createJWT(userDto.id, userDto.fullName, userDto.role, userDto.email),
                        refreshToken = JwtConfig.createRefreshToken(userDto.id),
                        expiresIn = JwtConfig.expiresIn,
                        user = userDto
                    ),
                    message = "Token refreshed successfully"
                )
            )
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            call.application.log.error("Token refresh failed", e)
            call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Token refresh failed"))
        }
    }

    /**
     * Starts a password reset.
     *
     * The response is always the same, so the endpoint cannot be used to discover which emails
     * have accounts. Without an email/SMS provider the token is returned in the response, which
     * is acceptable for the current deployment and must be swapped for a delivery channel later.
     */
    post("/auth/forgot-password") {
        try {
            val body = call.receiveText()
            val email = try {
                json.decodeFromString<Map<String, String>>(body)["email"].orEmpty().lowercase().trim()
            } catch (e: Exception) {
                ""
            }
            val genericResponse = ApiResponse.success<Unit>(
                data = Unit,
                message = "If that email has an account, a reset code was sent."
            )
            if (email.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Email is required"))
                return@post
            }

            val user = transaction { Users.selectAll().where { Users.email eq email }.firstOrNull() }
            if (user == null) {
                call.respond(genericResponse)
                return@post
            }

            val rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "")
            val tokenHash = sha256(rawToken)
            val expiresAt = Instant.now().plus(AppConfig.passwordResetTokenTtlMinutes, ChronoUnit.MINUTES)
            val userId = user[Users.id]

            transaction {
                // Invalidate older tokens for the same account.
                PasswordResetTokens.update({ (PasswordResetTokens.userId eq userId) and (PasswordResetTokens.usedAt.isNull()) }) {
                    it[PasswordResetTokens.usedAt] = Instant.now()
                }
                PasswordResetTokens.insert {
                    it[PasswordResetTokens.id] = UUID.randomUUID()
                    it[PasswordResetTokens.userId] = userId
                    it[PasswordResetTokens.tokenHash] = tokenHash
                    it[PasswordResetTokens.expiresAt] = expiresAt
                }
            }

            AuditService.logEvent(
                eventType = AuditService.EventType.USER_UPDATED,
                userId = userId.toString(),
                description = "Password reset requested"
            )

            // Delivery hook: replace this with an email/SMS send once a provider is configured.
            call.respond(
                ApiResponse.success(
                    data = mapOf(
                        "resetToken" to rawToken,
                        "expiresInMinutes" to AppConfig.passwordResetTokenTtlMinutes
                    ),
                    message = "If that email has an account, a reset code was sent."
                )
            )
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            call.application.log.error("Forgot password failed", e)
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Could not start a password reset"))
        }
    }

    post("/auth/reset-password") {
        try {
            val request = try {
                json.decodeFromString<ResetPasswordRequest>(call.receiveText())
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid request body"))
                return@post
            }
            if (request.token.isBlank() || request.newPassword.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Token and new password are required"))
                return@post
            }
            val passwordErrors = SecurityUtils.validatePasswordStrength(request.newPassword)
            if (passwordErrors.isNotEmpty()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>(passwordErrors.joinToString("; ")))
                return@post
            }

            val tokenHash = sha256(request.token.trim())
            val now = Instant.now()
            val record = transaction {
                PasswordResetTokens.selectAll()
                    .where { (PasswordResetTokens.tokenHash eq tokenHash) and (PasswordResetTokens.usedAt.isNull()) }
                    .firstOrNull()
            }
            if (record == null || record[PasswordResetTokens.expiresAt].isBefore(now)) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("This reset code is invalid or expired"))
                return@post
            }

            val hash = BCrypt.hashpw(request.newPassword, BCrypt.gensalt(AppConfig.bcryptCost))
            val userId = record[PasswordResetTokens.userId]
            transaction {
                Users.update({ Users.id eq userId }) { it[Users.passwordHash] = hash }
                PasswordResetTokens.update({ PasswordResetTokens.id eq record[PasswordResetTokens.id] }) {
                    it[PasswordResetTokens.usedAt] = now
                }
                // Any other pending token for the account is now unusable.
                PasswordResetTokens.update({ (PasswordResetTokens.userId eq userId) and (PasswordResetTokens.usedAt.isNull()) }) {
                    it[PasswordResetTokens.usedAt] = now
                }
            }

            AuditService.logEvent(
                eventType = AuditService.EventType.USER_UPDATED,
                userId = userId.toString(),
                description = "Password reset completed"
            )

            call.respond(ApiResponse.success<Unit>(data = Unit, message = "Password updated. Please sign in."))
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            call.application.log.error("Reset password failed", e)
            call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Could not reset the password"))
        }
    }

    authenticate("jwt-auth") {
        post("/auth/logout") {
            val token = call.request.header("Authorization")?.removePrefix("Bearer ")?.trim()
            if (!token.isNullOrBlank()) JwtConfig.revokeToken(token)
            call.respond(ApiResponse.success(Unit, message = "Logged out successfully"))
        }
    }
}

private fun sha256(value: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(value.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

private fun ResultRow.toDto(): UserDto = UserDto(
    id = this[Users.id].toString(),
    email = this[Users.email],
    fullName = this[Users.fullName],
    phone = this[Users.phone],
    avatarUrl = this[Users.avatarUrl],
    role = this[Users.role],
    isVerified = this[Users.isVerified],
    createdAt = this[Users.createdAt].toString()
)
