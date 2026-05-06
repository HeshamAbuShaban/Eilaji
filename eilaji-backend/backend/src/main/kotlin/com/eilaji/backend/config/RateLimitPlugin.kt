package com.eilaji.backend.config

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import com.eilaji.backend.service.RedisService

class RateLimitPlugin(
    private val redisService: RedisService,
    private val limit: Int = 100,
    private val windowSeconds: Long = 60
) {

    // Stricter rate limits for sensitive endpoints
    private val endpointLimits = mapOf(
        "/api/auth/login" to Pair(5, 60L),      // 5 attempts per minute
        "/api/auth/register" to Pair(3, 3600L), // 3 registrations per hour
        "/api/auth/refresh" to Pair(10, 60L),   // 10 refreshes per minute
        "/api/auth/logout" to Pair(10, 60L),    // 10 logouts per minute
        "/api/admin" to Pair(10, 60L)           // 10 admin requests per minute
    )
    
    fun install(application: Application) {
        application.intercept(ApplicationCallPipeline.Call) { call ->
            // Skip rate limiting for health checks and metrics
            val path = call.request.path()
            if (path.startsWith("/health") || path.startsWith("/metrics")) {
                return@intercept
            }

            // Get client identifier (IP)
            val clientIp = call.request.headers["X-Forwarded-For"]
                ?.split(",")?.firstOrNull()?.trim()
                ?: call.request.local.remoteHost

            // Check if this is a sensitive endpoint with stricter limits
            val (strictLimit, strictWindow) = endpointLimits.entries.firstOrNull {
                path.startsWith(it.key)
            }?.value ?: Pair(limit, windowSeconds)

            // Apply per-IP rate limit
            val ipKey = "rate_limit:ip:$clientIp"
            val pathKey = "rate_limit:path:$clientIp:$path"

            try {
                // Check general rate limit
                val generalCount = redisService.incrementKey(ipKey)
                if (generalCount == 1L) {
                    redisService.expireKey(ipKey, windowSeconds)
                }

                // Check path-specific rate limit for sensitive endpoints
                val shouldCheckPathLimit = endpointLimits.keys.any { path.startsWith(it) }
                if (shouldCheckPathLimit) {
                    val pathCount = redisService.incrementKey(pathKey)
                    if (pathCount == 1L) {
                        redisService.expireKey(pathKey, strictWindow)
                    }

                    if (pathCount > strictLimit) {
                        call.respond(
                            io.ktor.http.HttpStatusCode.TooManyRequests,
                            mapOf("success" to false, "error" to "Too many requests to this endpoint. Please try again later.")
                        )
                        finish()
                        return@intercept
                    }
                }

                // Check general limit
                if (generalCount > limit) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.TooManyRequests,
                        mapOf("success" to false, "error" to "Rate limit exceeded. Try again in $windowSeconds seconds.")
                    )
                    finish()
                    return@intercept
                }

                // Add security headers to all responses
                call.response.headers.append("X-Content-Type-Options", "nosniff")
                call.response.headers.append("X-Frame-Options", "DENY")
                call.response.headers.append("X-XSS-Protection", "1; mode=block")
                call.response.headers.append("Strict-Transport-Security", "max-age=31536000; includeSubDomains")

            } catch (e: Exception) {
                // If Redis is unavailable, allow the request but log warning
                println("WARNING: Rate limiting unavailable - ${e.message}")
                // Still add security headers
                call.response.headers.append("X-Content-Type-Options", "nosniff")
                call.response.headers.append("X-Frame-Options", "DENY")
            }
        }
    }
}
