package com.eilaji.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.JWTVerifier
import com.eilaji.backend.service.RedisService
import java.util.Date
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * JWT issuing and verification.
 *
 * Security rules enforced here:
 *  - Every token carries `sub` (the user id). The Ktor JWT plugin rejects tokens without it,
 *    which is what makes `authenticate("jwt-auth")` usable at all.
 *  - Refresh tokens use a separate audience and are only accepted by [verifyRefresh].
 *  - Revocation is stored by `jti` in Redis (with a process-local fallback) so logout has effect.
 */
object JwtConfig {
    // AppConfig owns secret resolution (env var, config, or a generated development secret).
    val secret: String = AppConfig.jwtSecret.also {
        require(it.length >= 32) { "JWT secret must be at least 32 characters" }
    }

    private val issuer: String = AppConfig.jwtIssuer
    private val audience: String = AppConfig.jwtAudience
    val realm: String = AppConfig.jwtRealm
    val expiresIn: Long = AppConfig.jwtExpiresIn
    val refreshExpiresIn: Long = AppConfig.jwtRefreshExpiresIn

    private val algorithm = Algorithm.HMAC256(secret)

    /** Injected once at boot; optional so unit tests stay independent. */
    @Volatile
    private var redisService: RedisService? = null

    /** Fallback store used when Redis is unreachable: jti -> expiry epoch millis. */
    private val localRevocations = ConcurrentHashMap<String, Long>()

    private const val CLAIM_TYPE = "type"
    private const val TOKEN_TYPE_ACCESS = "access"
    private const val TOKEN_TYPE_REFRESH = "refresh"

    fun bind(service: RedisService?) {
        redisService = service
    }

    fun getAccessVerifier(): JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .acceptLeeway(30)
            .build()

    fun getRefreshVerifier(): JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience("$audience-refresh")
            .acceptLeeway(30)
            .build()

    /** Full check used by the HTTP auth plugin and the WebSocket handshake. */
    @Throws(JWTVerificationException::class)
    fun verifyAccess(token: String): DecodedJWT {
        val decoded = getAccessVerifier().verify(token)
        requireClaims(decoded)
        if (isRevoked(decoded.id)) throw JWTVerificationException("Token has been revoked")
        return decoded
    }

    @Throws(JWTVerificationException::class)
    fun verifyRefresh(token: String): DecodedJWT {
        val decoded = getRefreshVerifier().verify(token)
        val userId = decoded.getClaim("user_id").asString()
        if (userId.isNullOrBlank()) throw JWTVerificationException("Invalid token: missing user_id claim")
        if (isRevoked(decoded.id)) throw JWTVerificationException("Token has been revoked")
        return decoded
    }

    private fun requireClaims(decoded: DecodedJWT) {
        val userId = decoded.getClaim("user_id").asString()
        val role = decoded.getClaim("role").asString()
        if (userId.isNullOrBlank()) throw JWTVerificationException("Invalid token: missing user_id claim")
        if (role.isNullOrBlank()) throw JWTVerificationException("Invalid token: missing role claim")
        if (decoded.subject.isNullOrBlank()) throw JWTVerificationException("Invalid token: missing subject claim")
    }

    fun createJWT(userId: String, fullName: String, role: String, email: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        require(role.isNotBlank()) { "Role cannot be blank" }
        require(email.isNotBlank()) { "Email cannot be blank" }
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withJWTId(UUID.randomUUID().toString())
            .withSubject(userId)
            .withClaim("user_id", userId)
            .withClaim("full_name", fullName)
            .withClaim("role", role)
            .withClaim("email", email)
            .withClaim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + expiresIn))
            .sign(algorithm)
    }

    fun createRefreshToken(userId: String): String {
        require(userId.isNotBlank()) { "User ID cannot be blank" }
        val now = System.currentTimeMillis()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience("$audience-refresh")
            .withJWTId(UUID.randomUUID().toString())
            .withSubject(userId)
            .withClaim("user_id", userId)
            .withClaim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + refreshExpiresIn))
            .sign(algorithm)
    }

    /** Revokes one token by its id for the remaining lifetime of that token. */
    fun revokeToken(token: String) {
        val decoded = try {
            JWT.decode(token)
        } catch (_: Exception) {
            return
        }
        revokeJti(decoded.id, decoded.expiresAt?.time ?: (System.currentTimeMillis() + expiresIn))
    }

    fun revokeJti(jti: String?, expiresAtMillis: Long?) {
        if (jti.isNullOrBlank()) return
        val ttlSeconds = ((expiresAtMillis ?: return) - System.currentTimeMillis()) / 1000
        if (ttlSeconds <= 0) return
        val service = redisService
        if (service != null && service.revokeTokenSync(jti, ttlSeconds)) return
        localRevocations[jti] = expiresAtMillis
    }

    fun isRevoked(jti: String?): Boolean {
        if (jti.isNullOrBlank()) return false
        redisService?.let { if (it.isTokenRevokedSync(jti)) return true }
        val localExpiry = localRevocations[jti] ?: return false
        if (localExpiry <= System.currentTimeMillis()) {
            localRevocations.remove(jti)
            return false
        }
        return true
    }

    fun validateToken(token: String): Boolean = try {
        verifyAccess(token)
        true
    } catch (e: Exception) {
        false
    }

    fun getUserIdFromToken(token: String): String? = try {
        verifyAccess(token).getClaim("user_id").asString()
    } catch (e: Exception) {
        null
    }

    fun getRoleFromToken(token: String): String? = try {
        verifyAccess(token).getClaim("role").asString()
    } catch (e: Exception) {
        null
    }

    fun getAlgorithm(): Algorithm = algorithm
    fun getIssuer(): String = issuer
    fun getAudience(): String = audience
}
