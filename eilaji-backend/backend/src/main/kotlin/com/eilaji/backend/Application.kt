package com.eilaji.backend

import com.eilaji.backend.config.*
import com.eilaji.backend.controller.*
import com.eilaji.backend.data.Database
import com.eilaji.backend.security.JwtConfig
import com.eilaji.backend.service.MinioService
import com.eilaji.backend.service.RedisService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Initialize database connection
    Database.init()
    
    // Initialize Redis
    val redisService = RedisService()
    
    // Initialize MinIO
    val minioService = MinioService()
    
    // Configure JSON serialization
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    
    // Configure CORS
    install(CORS) {
        allowHost("http://localhost:3000")
        allowHost("http://localhost:8080")
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowMethod(io.ktor.http.HttpMethod.Patch)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader("X-Requested-With")
        allowCredentials = true
    }
    
    // Configure JWT Authentication
    install(Authentication) {
        jwt("jwt-auth") {
            realm = "Eilaji Backend"
            verifier(JwtConfig.getVerifier())
            validate { credential ->
                if (credential.payload.getClaim("user_id").asString() != null) {
                    JwtPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
    
    // Configure Status Pages (Error Handling)
    install(io.ktor.server.plugins.statuspages.StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> {
                    call.respond(io.ktor.http.HttpStatusCode.BadRequest, mapOf("error" to cause.message ?: "Bad Request"))
                }
                is java.util.NoSuchElementException -> {
                    call.respond(io.ktor.http.HttpStatusCode.NotFound, mapOf("error" to "Resource not found"))
                }
                else -> {
                    call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("error" to "Internal Server Error"))
                }
            }
        }
    }
    
    // Configure Call Logging
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
    }
    
    // Configure Routing
    routing {
        // Health check endpoint
        get("/health") {
            call.respond(mapOf("status" to "UP", "timestamp" to System.currentTimeMillis()))
        }
        
        // API v1 routes
        route("/api/v1") {
            // Public routes (no auth required)
            authenticate(optional = true) {
                // Auth routes
                registerAuthRoutes(redisService)
                
                // Public medicine & pharmacy browsing
                registerMedicineRoutes()
                registerPharmacyRoutes()
            }
            
            // Protected routes (auth required)
            authenticate("jwt-auth") {
                // User profile routes
                registerUserRoutes()
                
                // Prescription routes
                registerPrescriptionRoutes(minioService)
                
                // Chat routes
                registerChatRoutes(redisService)
                
                // Favorites routes
                registerFavoritesRoutes()
                
                // Reminders routes
                registerReminderRoutes()
                
                // Rating routes
                registerRatingRoutes()
            }
            
            // Admin routes
            route("/admin") {
                authenticate("jwt-auth") {
                    registerAdminRoutes()
                }
            }
        }
        
        // WebSocket routes for real-time chat
        registerWebSocketRoutes(redisService)
        
        // Swagger documentation
        setupSwagger()
    }
    
    // Start background jobs
    startBackgroundJobs(redisService)
}
