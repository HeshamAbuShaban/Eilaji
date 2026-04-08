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
    
    fun install(application: Application) {
        application.intercept(ApplicationCallPipeline.Call) { call ->
            // Skip rate limiting for health checks
            if (call.request.path().startsWith("/health") || call.request.path().startsWith("/metrics")) {
                return@intercept
            }
            
            val clientId = call.request.headers["X-Forwarded-For"] 
                ?: call.request.local.remoteHost
            
            val key = "rate_limit:$clientId"
            
            try {
                val currentCount = redisService.incrementKey(key)
                
                if (currentCount == 1L) {
                    redisService.expireKey(key, windowSeconds)
                }
                
                if (currentCount > limit) {
                    call.respond(
                        io.ktor.http.HttpStatusCode.TooManyRequests,
                        mapOf("success" to false, "error" to "Rate limit exceeded. Try again in $windowSeconds seconds.")
                    )
                    finish()
                }
            } catch (e: Exception) {
                // If Redis is unavailable, allow the request but log warning
                println("WARNING: Rate limiting unavailable - ${e.message}")
            }
        }
    }
}
