package com.eilaji.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.typesafe.config.ConfigFactory
import java.util.Date
import java.util.UUID

object JwtConfig {
    private val config = ConfigFactory.load()

    private val secret: String = config.getString("jwt.secret")
        .takeIf { it.length >= 32 } ?: throw IllegalStateException("JWT secret must be at least 32 characters")

    private val issuer: String = config.getString("jwt.issuer")
    private val audience: String = config.getString("jwt.audience")
    val expiresIn: Long = config.getLong("jwt.expiresIn")
    val refreshExpiresIn: Long = config.getLong("jwt.refreshExpiresIn")

    private val algorithm = Algorithm.HMAC256(secret)

    // Token blacklist for logout functionality
    private val tokenBlacklist = mutableSetOf<String>()
    
    fun getVerifier(): JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .acceptLeeway(60)
            .build()

    @Throws(JWTVerificationException::class)
    fun verify(token: String): DecodedJWT {
        if (tokenBlacklist.contains(token)) throw JWTVerificationException("Token has been revoked")
        val decodedJWT = getVerifier().verify(token)
        val userId = decodedJWT.getClaim("user_id").asString()
        val role = decodedJWT.getClaim("role").asString()
        if (userId.isNullOrBlank()) throw JWTVerificationException("Invalid token: missing user_id claim")
        if (role.isNullOrBlank()) throw JWTVerificationException("Invalid token: missing role claim")
        return decodedJWT
    }

    fun createJWT(userId: String, fullName: String, role: String, email: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(role.isNotBlank()) { "Role cannot be blank" }
        require(email.isNotBlank()) { "Email cannot be blank" }
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("user_id", userId)
            .withClaim("full_name", fullName)
            .withClaim("role", role)
            .withClaim("email", email)
            .withIssuedAt(Date(System.currentTimeMillis()))
            .withExpiresAt(Date(System.currentTimeMillis() + expiresIn))
            .sign(algorithm)
    }

    fun createRefreshToken(userId: String): String =
        JWT.create()
            .withIssuer(issuer)
            .withAudience("${audience}-refresh")
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("user_id", userId)
            .withClaim("type", "refresh")
            .withIssuedAt(Date(System.currentTimeMillis()))
            .withExpiresAt(Date(System.currentTimeMillis() + refreshExpiresIn))
            .sign(algorithm)

    fun blacklistToken(token: String) { tokenBlacklist.add(token) }

    fun validateToken(token: String): Boolean = try {
        verify(token)
        true
    } catch (e: Exception) {
        false
    }

    fun getUserIdFromToken(token: String): String? = try {
        verify(token).getClaim("user_id").asString()
    } catch (e: Exception) {
        null
    }

    fun getRoleFromToken(token: String): String? = try {
        verify(token).getClaim("role").asString()
    } catch (e: Exception) {
        null
    }
    
    fun getAlgorithm(): Algorithm {
        return algorithm
    }
    
    fun getIssuer(): String {
        return issuer
    }
    
    fun getAudience(): String {
        return audience
    }
}
