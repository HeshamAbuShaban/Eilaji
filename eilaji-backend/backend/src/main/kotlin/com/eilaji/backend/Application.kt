package com.eilaji.backend

import com.eilaji.backend.config.DatabaseConfig
import com.eilaji.backend.config.rateLimitPlugin
import com.eilaji.backend.controller.adminRoutes
import com.eilaji.backend.data.*
import com.eilaji.backend.data.Migrations
import com.eilaji.backend.initialization.DatabaseSeeder
import com.eilaji.backend.routes.apiRoutes
import com.eilaji.backend.security.AppConfig
import com.eilaji.backend.security.JwtConfig
import com.eilaji.backend.security.securityHeadersPlugin
import com.eilaji.backend.service.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.event.Level

@Serializable
data class HealthResponse(val status: String, val timestamp: String, val checks: Map<String, String> = emptyMap())

fun main() {
    AppConfig.validate()

    val databaseUrl = AppConfig.databaseUrl
    val databaseUser = AppConfig.databaseUser
    val databasePassword = AppConfig.databasePassword
    val databaseDriver = AppConfig.databaseDriver

    val redisUrl = AppConfig.redisUrl

    val minioEndpoint = AppConfig.minioEndpoint
    val minioAccessKey = AppConfig.minioAccessKey
    val minioSecretKey = AppConfig.minioSecretKey
    val minioRegion = AppConfig.minioRegion

    val jwtSecret = AppConfig.jwtSecret
    val jwtIssuer = AppConfig.jwtIssuer
    val jwtAudience = AppConfig.jwtAudience
    val jwtRealm = AppConfig.jwtRealm

    val eilajiPlusBaseUrl = AppConfig.eilajiPlusBaseUrl
    val eilajiPlusApiKey = AppConfig.eilajiPlusApiKey

    val port = AppConfig.port

    // Retry init in case the DB container hasn't finished starting yet
    var initSuccess = false
    repeat(5) { attempt ->
        try {
            DatabaseConfig.init()
            initSuccess = true
            println("Database connection initialized successfully (attempt ${attempt + 1})")
            return@repeat
        } catch (e: Exception) {
            println("DB init failed (attempt ${attempt + 1}): ${e.message}")
            if (attempt == 4) throw e
            Thread.sleep(2000L)
        }
    }
    if (!initSuccess) {
        throw IllegalStateException("Failed to initialize database after retries")
    }

    if (AppConfig.createMissingTables) {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users, Categories, Subcategories, Medicines, Pharmacies,
                PharmacyMedicines, Prescriptions, Chats, Messages, Favorites,
                Ratings, MedicationReminders, EilajiPlusSync,
                Orders, AuditLogs, PasswordResetTokens
            )
        }
        Migrations.run()
    }

    if (AppConfig.seedOnStartup) {
        println("WARNING: Database seeding is enabled - for development only!")
        DatabaseSeeder.seedIfEmpty()
    }

    val minioService = MinioService()
    val redisService = RedisService(redisUrl)
    JwtConfig.bind(redisService)
    val eilajiPlusService = if (eilajiPlusBaseUrl.isNotBlank()) {
        EilajiPlusService(eilajiPlusBaseUrl, eilajiPlusApiKey)
    } else null

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        mainModule(
            jwtIssuer = jwtIssuer,
            jwtAudience = jwtAudience,
            jwtRealm = jwtRealm,
            jwtSecret = jwtSecret,
            minioService = minioService,
            redisService = redisService,
            eilajiPlusService = eilajiPlusService
        )
    }.start(wait = true)
}

fun Application.mainModule(
    jwtIssuer: String,
    jwtAudience: String,
    jwtRealm: String,
    jwtSecret: String,
    minioService: MinioService,
    redisService: RedisService,
    eilajiPlusService: EilajiPlusService? = null
) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    securityHeadersPlugin()

    install(CORS) {
        AppConfig.allowedHosts.forEach { entry ->
            val scheme = entry.substringBefore("://")
            val authority = entry.substringAfter("://")
            allowHost(authority, listOf(scheme))
        }
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowCredentials = AppConfig.allowedHosts.isNotEmpty()
        maxAgeInSeconds = 3600
    }

    install(StatusPages) {
        exception<com.eilaji.backend.security.ApiException> { call, cause ->
            call.respond(cause.status, mapOf("success" to false, "error" to cause.message))
        }
        exception<kotlinx.serialization.SerializationException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "error" to "Malformed request body"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error on ${call.request.uri}", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "error" to "Internal server error"))
        }
    }

    install(CallLogging) {
        level = Level.INFO
        // Never log requests that carry a token in the query string.
        filter { call -> !call.request.uri.contains("token=") }
    }

    install(WebSockets) {
        maxFrameSize = AppConfig.maxWebSocketFrameBytes
        masking = true
    }

    rateLimitPlugin(redisService = redisService)

    install(Authentication) {
        jwt("jwt-auth") {
            realm = jwtRealm
            verifier(JwtConfig.getAccessVerifier())
            validate { credentials ->
                if (credentials.payload.subject.isNullOrBlank()) null
                else JWTPrincipal(credentials.payload)
            }
        }
    }

    routing {
        get("/health") {
            val checks = linkedMapOf<String, String>()
            var ok = true
            try {
                transaction { exec("SELECT 1") { rs -> rs.next() } }
                checks["database"] = "UP"
            } catch (e: Exception) {
                ok = false
                checks["database"] = "DOWN"
            }
            checks["redis"] = if (redisService.isAvailable()) "UP" else "DOWN"
            if (checks["redis"] == "DOWN") ok = false
            call.respond(
                if (ok) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                HealthResponse(
                    status = if (ok) "UP" else "DEGRADED",
                    timestamp = System.currentTimeMillis().toString(),
                    checks = checks
                )
            )
        }

        // Test dashboard (pharmacy ops + customer mini-view) — dev testing until E-Doctor lands.
        // It authenticates against the same API as the app, so no separate login is served here.
        staticResources("/dashboard", "static.dashboard")
        staticResources("/assets", "static.assets")

        apiRoutes(
            jwtIssuer = jwtIssuer,
            jwtAudience = jwtAudience,
            jwtRealm = jwtRealm,
            jwtSecret = jwtSecret,
            minioService = minioService,
            redisService = redisService,
            eilajiPlusService = eilajiPlusService
        )

        authenticate("jwt-auth") {
            adminRoutes()
        }
    }
}
