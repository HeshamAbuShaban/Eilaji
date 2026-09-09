package com.eilaji.backend

import com.eilaji.backend.config.DatabaseConfig
import com.eilaji.backend.config.rateLimitPlugin
import com.eilaji.backend.controller.adminRoutes
import com.eilaji.backend.data.*
import com.eilaji.backend.initialization.DatabaseSeeder
import com.eilaji.backend.routes.apiRoutes
import com.eilaji.backend.security.JwtConfig
import com.eilaji.backend.service.*
import com.typesafe.config.ConfigFactory
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.serialization.Serializable
import org.slf4j.event.Level

@Serializable
data class HealthResponse(val status: String, val timestamp: String)

fun main() {
    val config = ConfigFactory.load()

    val databaseUrl = config.getString("database.url")
    val databaseUser = config.getString("database.user")
    val databasePassword = config.getString("database.password")
    val databaseDriver = config.getString("database.driver")

    val redisUrl = config.getString("redis.url")

    val minioEndpoint = config.getString("minio.endpoint")
    val minioAccessKey = config.getString("minio.accessKey")
    val minioSecretKey = config.getString("minio.secretKey")
    val minioRegion = config.getString("minio.region")

    val jwtSecret = config.getString("jwt.secret")
    val jwtIssuer = config.getString("jwt.issuer")
    val jwtAudience = config.getString("jwt.audience")
    val jwtRealm = config.getString("jwt.realm")

    val eilajiPlusBaseUrl = config.getString("eilaji-plus.baseUrl")
    val eilajiPlusApiKey = config.getString("eilaji-plus.apiKey")

    val port = config.getInt("server.port")

    // Verify password is loaded (won't print the actual value)
    println("DB config – user=$databaseUser, password=${if (databasePassword.isNotBlank()) "<present>" else "<missing>"}")
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

    transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Users, Categories, Subcategories, Medicines, Pharmacies,
            PharmacyMedicines, Prescriptions, Chats, Messages, Favorites,
            Ratings, MedicationReminders, EilajiPlusSync,
            Orders, AuditLogs
        )
    }

    val seedDatabase = config.getBoolean("database.seed-on-startup")
    if (seedDatabase) {
        println("WARNING: Database seeding is enabled - for development only!")
        DatabaseSeeder.seedIfEmpty()
    }

    val minioService = MinioService()
    val redisService = RedisService(redisUrl)
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

    install(CORS) {
        allowHost("localhost:8080", listOf("http"))
        allowHost("localhost:3000", listOf("http"))
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowCredentials = true
        maxAgeInSeconds = 3600
        anyHost()
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(WebSockets) {
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    rateLimitPlugin(redisService = redisService)

    install(Authentication) {
        jwt("jwt-auth") {
            realm = jwtRealm
            verifier(JwtConfig.getVerifier())
            validate { credentials ->
                if (credentials.payload.subject != null) {
                    JWTPrincipal(credentials.payload)
                } else {
                    null
                }
            }
        }
    }

    routing {
        get("/health") {
            call.respond(HealthResponse(status = "UP", timestamp = System.currentTimeMillis().toString()))
        }

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
