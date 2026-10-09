package com.eilaji.backend.security

import com.eilaji.backend.data.UserRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.response.respond

/** Identity of the caller taken from a verified JWT. */
data class Caller(val userId: String, val role: UserRole)

/**
 * Reads the verified caller or answers 401 and returns null.
 *
 * Every private route used to do `principal!!`, which turns any missing claim into a 500.
 * This makes the failure explicit and keeps one place for the checks.
 */
suspend fun ApplicationCall.callerOr401(): Caller? {
    val principal = principal<JWTPrincipal>()
    if (principal == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "error" to "Authentication required"))
        return null
    }
    val userId = principal.payload.subject ?: principal.payload.getClaim("user_id").asString().orEmpty()
    if (userId.isBlank()) {
        respond(HttpStatusCode.Unauthorized, mapOf("success" to false, "error" to "Authentication required"))
        return null
    }
    val role = principal.payload.getClaim("role").asString()
    val parsed = try {
        UserRole.valueOf(role.uppercase())
    } catch (_: Exception) {
        // Unknown role in the token: treat as the least privileged role instead of failing the request.
        UserRole.PATIENT
    }
    return Caller(userId, parsed)
}

/** Rejects the call with 403 when the role is not allowed. Returns true when allowed. */
suspend fun ApplicationCall.requireRole(vararg allowed: UserRole): Caller? {
    val caller = callerOr401() ?: return null
    if (caller.role !in allowed) {
        respond(HttpStatusCode.Forbidden, mapOf("success" to false, "error" to "You do not have permission for this action"))
        return null
    }
    return caller
}
