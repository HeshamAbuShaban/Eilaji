package com.eilaji.backend

import com.eilaji.backend.config.RateLimitPlugin
import com.eilaji.backend.controller.adminRoutes
import com.eilaji.backend.model.*
import com.eilaji.backend.routes.apiRoutes
import com.eilaji.backend.service.*
import com.typesafe.config.ConfigFactory
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.slf4j.event.Level
import java.time.Duration

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
    
    // Initialize database
    Database.connect(
        url = databaseUrl,
        driver = databaseDriver,
        user = databaseUser,
        password = databasePassword
    )
    
    // Create tables
    org.jetbrains.exposed.sql.transactions.transaction {
        SchemaUtils.createMissingTablesAndColumns(
            Users,
            Categories,
            Medicines,
            Pharmacies,
            Prescriptions,
            Chats,
            Messages,
            EilajiPlusSync,
            Orders
        )
    }
    
    // Initialize services
    val minioService = MinioService(minioEndpoint, minioAccessKey, minioSecretKey, minioRegion)
    val redisService = RedisService(redisUrl)
    val eilajiPlusService = if (eilajiPlusBaseUrl.isNotBlank()) {
        EilajiPlusService(eilajiPlusBaseUrl, eilajiPlusApiKey)
    } else null
    
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = {
        mainModule(
            jwtIssuer = jwtIssuer,
            jwtAudience = jwtAudience,
            jwtRealm = jwtRealm,
            jwtSecret = jwtSecret,
            minioService = minioService,
            redisService = redisService,
            eilajiPlusService = eilajiPlusService
        )
    }).start(wait = true)
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
    // Install plugins
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    
    install(CORS) {
        allowHost("*", schemes = listOf("http", "https"))
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.AccessControlRequestHeaders)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowCredentials = true
        maxAgeInSeconds = 3600
    }
    
    install(CallLogging) {
        level = Level.INFO
        filter { call -> !call.request.path().startsWith("/health") }
    }
    
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    install(Authentication) {
        jwt("jwt-auth") {
            realm = jwtRealm
            verifier {
                JWTVerifier(io.ktor.server.auth.jwt.JWTAlgorithm.HMAC256, jwtSecret)
                    .withIssuer(jwtIssuer)
                    .withAudience(jwtAudience)
                    .build()
            }
            validate { credentials ->
                if (credentials.payload.getSubject() != null) {
                    JWTPrincipal(credentials.payload)
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
    
    // Setup routes
    routing {
        // Health check endpoint
        get("/health") {
            call.respond(mapOf("status" to "UP", "timestamp" to System.currentTimeMillis()))
        }
        
        // Install rate limiting plugin
        RateLimitPlugin(redisService).install(this@mainModule)
        
        apiRoutes(
            jwtIssuer = jwtIssuer,
            jwtAudience = jwtAudience,
            jwtRealm = jwtRealm,
            jwtSecret = jwtSecret,
            minioService = minioService,
            redisService = redisService,
            eilajiPlusService = eilajiPlusService
        )
        
        // Admin routes
        adminRoutes()
    }
}
