package com.eilaji.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Date
import java.util.UUID

/**
 * These tests pin the identity contract that the Ktor `authenticate("jwt-auth")` plugin relies on:
 * a token from createJWT must contain `sub`, because the plugin's validate block rejects tokens
 * without it. That claim was missing once and every private route answered 401.
 */
class JwtConfigTest {

    @Test
    fun `createJWT creates verifiable token with correct claims`() {
        val userId = UUID.randomUUID().toString()
        val token = JwtConfig.createJWT(userId, "Test User", "PATIENT", "test@example.com")
        assertNotNull(token)
        assertTrue(token.isNotBlank())
        val decoded = JwtConfig.verifyAccess(token)
        assertEquals(userId, decoded.getClaim("user_id").asString())
        assertEquals("PATIENT", decoded.getClaim("role").asString())
        assertEquals("test@example.com", decoded.getClaim("email").asString())
        assertEquals("Test User", decoded.getClaim("full_name").asString())
        assertEquals(JwtConfig.getIssuer(), decoded.issuer)
        assertEquals(JwtConfig.getAudience(), decoded.audience[0])
    }

    @Test
    fun `access token carries a subject so the auth plugin can build a principal`() {
        val userId = UUID.randomUUID().toString()
        val token = JwtConfig.createJWT(userId, "Alice", "DOCTOR", "alice@example.com")
        // The plugin verifies with exactly this verifier and requires a subject.
        val decoded = JwtConfig.getAccessVerifier().verify(token)
        assertEquals(userId, decoded.subject)
        assertNotNull(decoded.subject)
    }

    @Test
    fun `refresh token is rejected by the access verifier`() {
        val userId = UUID.randomUUID().toString()
        val refreshToken = JwtConfig.createRefreshToken(userId)
        assertThrows(JWTVerificationException::class.java) {
            JwtConfig.getAccessVerifier().verify(refreshToken)
        }
        assertNotNull(JwtConfig.getRefreshVerifier().verify(refreshToken))
    }

    @Test
    fun `verify fails with wrong secret`() {
        val wrongAlgorithm = Algorithm.HMAC256("different-secret-that-is-also-at-least-32-chars-long!!")
        val fakeToken = JWT.create()
            .withIssuer(JwtConfig.getIssuer())
            .withAudience(JwtConfig.getAudience())
            .withJWTId(UUID.randomUUID().toString())
            .withSubject(UUID.randomUUID().toString())
            .withClaim("user_id", UUID.randomUUID().toString())
            .withClaim("role", "PATIENT")
            .withClaim("email", "fake@example.com")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(wrongAlgorithm)
        assertThrows(JWTVerificationException::class.java) {
            JwtConfig.verifyAccess(fakeToken)
        }
        assertFalse(JwtConfig.validateToken(fakeToken))
        assertNull(JwtConfig.getUserIdFromToken(fakeToken))
    }

    @Test
    fun `verify fails when missing user_id claim`() {
        val tokenWithoutUserId = JWT.create()
            .withIssuer(JwtConfig.getIssuer())
            .withAudience(JwtConfig.getAudience())
            .withJWTId(UUID.randomUUID().toString())
            .withSubject(UUID.randomUUID().toString())
            .withClaim("role", "PATIENT")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(JwtConfig.getAlgorithm())
        assertThrows(JWTVerificationException::class.java) {
            JwtConfig.verifyAccess(tokenWithoutUserId)
        }
    }

    @Test
    fun `verify fails when missing role claim`() {
        val tokenWithoutRole = JWT.create()
            .withIssuer(JwtConfig.getIssuer())
            .withAudience(JwtConfig.getAudience())
            .withJWTId(UUID.randomUUID().toString())
            .withSubject(UUID.randomUUID().toString())
            .withClaim("user_id", UUID.randomUUID().toString())
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(JwtConfig.getAlgorithm())
        assertThrows(JWTVerificationException::class.java) {
            JwtConfig.verifyAccess(tokenWithoutRole)
        }
    }

    @Test
    fun `verify fails when missing subject claim`() {
        val tokenWithoutSubject = JWT.create()
            .withIssuer(JwtConfig.getIssuer())
            .withAudience(JwtConfig.getAudience())
            .withJWTId(UUID.randomUUID().toString())
            .withClaim("user_id", UUID.randomUUID().toString())
            .withClaim("role", "PATIENT")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(JwtConfig.getAlgorithm())
        assertThrows(JWTVerificationException::class.java) {
            JwtConfig.verifyAccess(tokenWithoutSubject)
        }
    }

    @Test
    fun `getUserIdFromToken returns userId for valid token`() {
        val userId = UUID.randomUUID().toString()
        val token = JwtConfig.createJWT(userId, "Bob", "ADMIN", "bob@example.com")
        assertEquals(userId, JwtConfig.getUserIdFromToken(token))
    }

    @Test
    fun `getUserIdFromToken returns null for invalid token`() {
        assertNull(JwtConfig.getUserIdFromToken("invalid.token.here"))
        assertNull(JwtConfig.getUserIdFromToken(""))
    }

    @Test
    fun `getRoleFromToken returns role for valid token`() {
        val userId = UUID.randomUUID().toString()
        val token = JwtConfig.createJWT(userId, "Carol", "PHARMACIST", "carol@example.com")
        assertEquals("PHARMACIST", JwtConfig.getRoleFromToken(token))
    }

    @Test
    fun `revoking a token invalidates it`() {
        val userId = UUID.randomUUID().toString()
        val token = JwtConfig.createJWT(userId, "Dave", "PATIENT", "dave@example.com")
        assertTrue(JwtConfig.validateToken(token))
        JwtConfig.revokeToken(token)
        assertThrows(JWTVerificationException::class.java) {
            JwtConfig.verifyAccess(token)
        }
        assertFalse(JwtConfig.validateToken(token))
        assertNull(JwtConfig.getUserIdFromToken(token))
    }

    @Test
    fun `createJWT rejects blank userId role and email`() {
        assertThrows(IllegalArgumentException::class.java) {
            JwtConfig.createJWT("", "Name", "PATIENT", "test@example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            JwtConfig.createJWT(UUID.randomUUID().toString(), "Name", "", "test@example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            JwtConfig.createJWT(UUID.randomUUID().toString(), "Name", "PATIENT", "")
        }
    }

    @Test
    fun `validateToken returns false for malformed token`() {
        assertFalse(JwtConfig.validateToken("not.a.jwt"))
        assertFalse(JwtConfig.validateToken(""))
    }

    @Test
    fun `access verifier returns working verifier`() {
        assertNotNull(JwtConfig.getAccessVerifier())
        val token = JwtConfig.createJWT(UUID.randomUUID().toString(), "Eve", "PATIENT", "eve@example.com")
        assertNotNull(JwtConfig.getAccessVerifier().verify(token))
    }
}
