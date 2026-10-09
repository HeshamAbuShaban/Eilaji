package com.eilaji.backend.config

import com.eilaji.backend.security.AppConfig
import com.eilaji.backend.service.RedisService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.path
import io.ktor.server.response.respond

/**
 * Rate limiting backed by Redis counters.
 *
 * - Strict buckets on the auth routes (per IP + path).
 * - Global bucket per IP.
 * - When Redis is unreachable the limiter answers 429 (fail closed) for the strict buckets,
 *   because silently disabling brute-force protection is the worse failure.
 */
fun Application.rateLimitPlugin(
    redisService: RedisService,
    limit: Int = AppConfig.rateLimitPerMinute,
    windowSeconds: Long = 60
) {
    val loginLimit = AppConfig.rateLimitLoginPerMinute
    val registerLimit = AppConfig.rateLimitRegisterPerHour
    val refreshLimit = AppConfig.rateLimitRefreshPerMinute
    val failClosed = AppConfig.rateLimitFailClosed
    val globalLimit = AppConfig.rateLimitPerMinute

    val endpointLimits = mapOf(
        "/api/v1/auth/login" to Pair(loginLimit, 60L),
        "/api/v1/auth/register" to Pair(registerLimit, 3600L),
        "/api/v1/auth/refresh" to Pair(refreshLimit, 60L),
        "/api/v1/auth/logout" to Pair(refreshLimit, 60L),
        "/api/v1/auth/forgot-password" to Pair(refreshLimit, 60L),
        "/api/v1/admin" to Pair(loginLimit, 60L)
    )

    val plugin = createRouteScopedPlugin(name = "RateLimiter") {
        onCall { call ->
            val path = call.request.path()
            if (path.startsWith("/health") || path.startsWith("/metrics")) return@onCall

            val clientIp = clientIp(call)

            val strict = endpointLimits.entries.firstOrNull { path.startsWith(it.key) }?.value
            val ipKey = "rate_limit:ip:$clientIp"

            if (!redisService.isAvailable()) {
                if (failClosed && strict != null) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("success" to false, "error" to "Service is busy. Please try again shortly.")
                    )
                }
                return@onCall
            }

            val generalCount = redisService.incrementKey(ipKey)
            if (generalCount == 1L) redisService.expireKey(ipKey, windowSeconds)

            if (strict != null) {
                val (strictLimit, strictWindow) = strict
                val pathKey = "rate_limit:path:$clientIp:$path"
                val pathCount = redisService.incrementKey(pathKey)
                if (pathCount == 1L) redisService.expireKey(pathKey, strictWindow)
                if (pathCount > strictLimit) {
                    call.respond(
                        HttpStatusCode.TooManyRequests,
                        mapOf("success" to false, "error" to "Too many requests to this endpoint. Please try again later.")
                    )
                    return@onCall
                }
            }

            if (generalCount > limit) {
                call.respond(
                    HttpStatusCode.TooManyRequests,
                    mapOf("success" to false, "error" to "Rate limit exceeded. Try again in $windowSeconds seconds.")
                )
                return@onCall
            }
        }
    }

    install(plugin)
}

private fun clientIp(call: io.ktor.server.application.ApplicationCall): String {
    val forwarded = call.request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
    return if (!forwarded.isNullOrBlank()) forwarded else call.request.local.remoteHost
}
