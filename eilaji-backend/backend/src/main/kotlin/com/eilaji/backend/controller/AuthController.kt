package com.eilaji.backend.controller

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.eilaji.backend.data.Users
import com.eilaji.backend.data.UserRole
import com.eilaji.backend.dto.*
import com.eilaji.backend.security.JwtConfig
import com.eilaji.backend.service.RedisService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.bouncycastle.crypto.generators.BCrypt
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

fun Route.registerAuthRoutes(redisService: RedisService) {
    // Register new user
    post("/auth/register") {
        try {
            val request = call.receive<RegisterRequest>()
            
            // Validate input
            if (request.email.isBlank() || request.password.length < 6 || request.fullName.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse.error<String>("Invalid input data"))
                return@post
            }
            
            // Check if user already exists
            val existingUser = transaction {
                Users.select { Users.email eq request.email }.firstOrNull()
            }
            
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, ApiResponse.error<String>("Email already registered"))
                return@post
            }
            
            // Hash password with BCrypt
            val salt = BCrypt.gensalt(10)
            val passwordHash = String(BCrypt.hashpw(request.password.toByteArray(), salt))
            
            // Parse role
            val role = try {
                UserRole.valueOf(request.role.uppercase())
            } catch (e: IllegalArgumentException) {
                UserRole.PATIENT
            }
            
            // Create user
            val userId = transaction {
                Users.insert {
                    it[email] = request.email.lowercase().trim()
                    it[passwordHash] = passwordHash
                    it[fullName] = request.fullName.trim()
                    it[phone] = request.phone?.trim()
                    it[this.role] = role
                    it[isVerified] = false
                } get Users.id
            }
            
            // Generate tokens
            val accessToken = generateAccessToken(userId.toString(), role.name)
            val refreshToken = generateRefreshToken(userId.toString())
            
            // Store refresh token in Redis
            redisService.storeSession("refresh:$refreshToken", userId.toString(), JwtConfig.refreshExpiresIn.toInt())
            
            val userDto = transaction {
                Users.select { Users.id eq userId }.firstOrNull()?.let { row ->
                    UserDto(
                        id = row[Users.id],
                        email = row[Users.email],
                        fullName = row[Users.fullName],
                        phone = row[Users.phone],
                        avatarUrl = row[Users.avatarUrl],
                        role = row[Users.role].name,
                        isVerified = row[Users.isVerified],
                        createdAt = row[Users.createdAt]
                    )
                }
            }
            
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
    
    // Login
    post("/auth/login") {
        try {
            val request = call.receive<LoginRequest>()
            
            // Find user by email
            val user = transaction {
                Users.select { Users.email eq request.email.lowercase().trim() }.firstOrNull()
            }
            
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid credentials"))
                return@post
            }
            
            // Verify password
            val passwordHash = user[Users.passwordHash]
            if (!BCrypt.checkpw(request.password.toByteArray(), passwordHash.toByteArray())) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid credentials"))
                return@post
            }
            
            // Check if user is active
            if (!user[Users.isActive]) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse.error<String>("Account is deactivated"))
                return@post
            }
            
            val userId = user[Users.id].toString()
            val role = user[Users.role].name
            
            // Generate tokens
            val accessToken = generateAccessToken(userId, role)
            val refreshToken = generateRefreshToken(userId)
            
            // Store refresh token in Redis
            redisService.storeSession("refresh:$refreshToken", userId, JwtConfig.refreshExpiresIn.toInt())
            
            // Set user online
            redisService.setUserOnline(userId)
            
            val userDto = UserDto(
                id = user[Users.id],
                email = user[Users.email],
                fullName = user[Users.fullName],
                phone = user[Users.phone],
                avatarUrl = user[Users.avatarUrl],
                role = user[Users.role].name,
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
    
    // Refresh token
    post("/auth/refresh") {
        try {
            val request = call.receive<RefreshTokenRequest>()
            
            // Verify refresh token from Redis
            val userId = redisService.getSessionUserId("refresh:${request.refreshToken}")
            
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("Invalid or expired refresh token"))
                return@post
            }
            
            // Get user from database
            val user = transaction {
                Users.select { Users.id eq UUID.fromString(userId) }.firstOrNull()
            }
            
            if (user == null || !user[Users.isActive]) {
                call.respond(HttpStatusCode.Unauthorized, ApiResponse.error<String>("User not found or inactive"))
                return@post
            }
            
            val role = user[Users.role].name
            
            // Generate new access token
            val newAccessToken = generateAccessToken(userId, role)
            
            call.respond(ApiResponse.success(
                mapOf("accessToken" to newAccessToken, "expiresIn" to JwtConfig.expiresIn),
                "Token refreshed successfully"
            ))
            
        } catch (e: Exception) {
            call.respond(HttpStatusCode.InternalServerError, ApiResponse.error<String>("Token refresh failed: ${e.message}"))
        }
    }
    
    // Logout
    post("/auth/logout") {
        try {
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("user_id")?.asString()
            
            if (userId != null) {
                // Set user offline
                redisService.setUserOffline(userId)
                
                // Invalidate all sessions for this user (optional - you might want to keep a blacklist)
                // For now, we'll just set the user offline
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
