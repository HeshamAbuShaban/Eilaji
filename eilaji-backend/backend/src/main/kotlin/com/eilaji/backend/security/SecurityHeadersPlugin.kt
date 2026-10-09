package com.eilaji.backend.security

import io.ktor.server.application.Application
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.application.pluginOrNull
import io.ktor.server.application.install
import io.ktor.server.response.header

/**
 * Security headers on every response. (Ktor 3 removed the DefaultHeaders plugin, so the same
 * headers are set with a small local plugin.)
 */
class SecurityHeadersConfig

val SecurityHeaders = createApplicationPlugin(
    name = "SecurityHeaders",
    createConfiguration = { SecurityHeadersConfig() }
) {
    on(ResponseSent) { call ->
        call.response.header("X-Content-Type-Options", "nosniff")
        call.response.header("X-Frame-Options", "DENY")
        call.response.header("Referrer-Policy", "no-referrer")
    }
}

fun Application.securityHeadersPlugin() {
    pluginOrNull(SecurityHeaders) ?: install(SecurityHeaders)
}
