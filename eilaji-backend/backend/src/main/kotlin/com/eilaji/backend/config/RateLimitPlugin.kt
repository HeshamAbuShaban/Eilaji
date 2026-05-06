package com.eilaji.backend.config

import com.eilaji.backend.service.RedisService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*

fun Application.rateLimitPlugin(
    redisService: RedisService,
    limit: Int = 100,
    windowSeconds: Long = 60
) {
    val endpointLimits = mapOf(
        "/api/auth/login" to Pair(5, 60L),
        "/api/auth/register" to Pair(3, 3600L),
        "/api/auth/refresh" to Pair(10, 60L),
        "/api/auth/logout" to Pair(10, 60L),
        "/api/admin" to Pair(10, 60L)
    )

    val plugin = createRouteScopedPlugin(
        name = "RateLimiter",
        createConfiguration = { }
    ) {
        onCall { call ->
            val path = call.request.path()
            if (path.startsWith("/health") || path.startsWith("/metrics")) {
                return@onCall
            }

            val clientIp = call.request.local.remoteHost

            val (strictLimit, strictWindow) = endpointLimits.entries.firstOrNull {
                path.startsWith(it.key)
            }?.value ?: Pair(limit, windowSeconds)

            val ipKey = "rate_limit:ip:$clientIp"
            val pathKey = "rate_limit:path:$clientIp:$path"

            try {
                val generalCount = redisService.incrementKey(ipKey)
                if (generalCount == 1L) {
                    redisService.expireKey(ipKey, windowSeconds)
                }

                val shouldCheckPathLimit = endpointLimits.keys.any { path.startsWith(it) }
                if (shouldCheckPathLimit) {
                    val pathCount = redisService.incrementKey(pathKey)
                    if (pathCount == 1L) {
                        redisService.expireKey(pathKey, strictWindow)
                    }

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
            } catch (e: Exception) {
                println("WARNING: Rate limiting unavailable - ${e.message}")
            }
        }
    }

    install(plugin)
}
