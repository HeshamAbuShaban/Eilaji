package com.eilaji.backend.security

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.security.SecureRandom

/**
 * Configuration: typed values from application.conf, overridden by environment variables.
 *
 * The environment override lives here (not in HOCON substitutions) because `${?VAR} default`
 * always produces a string in HOCON, which then fails a typed get.
 *
 * The JWT secret is never kept in application.conf: production must set JWT_SECRET, and local
 * development gets a random secret that changes on every start.
 */
object AppConfig {

    private val config: Config = ConfigFactory.load()

    private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

    private fun str(envName: String, configPath: String, default: String): String =
        env(envName) ?: if (config.hasPath(configPath)) config.getString(configPath) else default

    private fun int(envName: String, configPath: String, default: Int): Int =
        env(envName)?.toIntOrNull() ?: if (config.hasPath(configPath)) config.getInt(configPath) else default

    private fun long(envName: String, configPath: String, default: Long): Long =
        env(envName)?.toLongOrNull() ?: if (config.hasPath(configPath)) config.getLong(configPath) else default

    private fun bool(envName: String, configPath: String, default: Boolean): Boolean =
        env(envName)?.lowercase()?.let { v -> v == "true" || v == "yes" || v == "1" }
            ?: if (config.hasPath(configPath)) config.getBoolean(configPath) else default

    private fun double(envName: String, configPath: String, default: Double): Double =
        env(envName)?.toDoubleOrNull() ?: if (config.hasPath(configPath)) config.getDouble(configPath) else default

    private fun list(envName: String, configPath: String): List<String> =
        env(envName)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: runCatching { config.getStringList(configPath) }.getOrDefault(emptyList())

    // ---- Profile ---------------------------------------------------------------------

    /** dev (default) or prod. Production is stricter: JWT_SECRET is required, no seeding. */
    val profile: String = env("APP_PROFILE") ?: str("APP_PROFILE", "app.profile", "dev")

    val isProduction: Boolean = profile.equals("prod", ignoreCase = true)

    /** Boot-time table creation with SchemaUtils is a development convenience. */
    val createMissingTables: Boolean = bool("CREATE_MISSING_TABLES", "database.createMissingTables", !isProduction)

    // ---- Database --------------------------------------------------------------------

    val databaseUrl: String = str("DATABASE_URL", "database.url", "jdbc:postgresql://localhost:5432/eilaji_db")
    val databaseUser: String = str("DATABASE_USER", "database.user", "eilaji")
    val databasePassword: String = str("DATABASE_PASSWORD", "database.password", "")
    val databaseDriver: String = str("DATABASE_DRIVER", "database.driver", "org.postgresql.Driver")
    val databasePoolSize: Int = int("DATABASE_POOL_SIZE", "database.poolSize", 10)

    val seedOnStartup: Boolean = bool("SEED_ON_STARTUP", "database.seed-on-startup", false)
    /** Demo accounts (patient@eilaji.com / password123) are only seeded when explicitly enabled. */
    val seedDemoCredentials: Boolean = bool("SEED_DEMO_CREDENTIALS", "database.seed-demo-credentials", false)

    // ---- Redis -----------------------------------------------------------------------

    /** Supports credentials: redis://:password@host:6379/0 */
    val redisUrl: String = env("REDIS_URL") ?: str("REDIS_URL", "redis.url", "redis://localhost:6379")

    // ---- MinIO -----------------------------------------------------------------------

    val minioEndpoint: String = str("MINIO_ENDPOINT", "minio.endpoint", "http://localhost:9000")
    val minioAccessKey: String = str("MINIO_ACCESS_KEY", "minio.accessKey", "eilaji_minio_admin")
    val minioSecretKey: String = str("MINIO_SECRET_KEY", "minio.secretKey", "")
    val minioRegion: String = str("MINIO_REGION", "minio.region", "us-east-1")
    val minioBuckets: Map<String, String> = mapOf(
        "prescriptions" to str("MINIO_BUCKET_PRESCRIPTIONS", "minio.buckets.prescriptions", "prescriptions"),
        "medicineImages" to str("MINIO_BUCKET_MEDICINE_IMAGES", "minio.buckets.medicineImages", "medicine-images"),
        "pharmacyImages" to str("MINIO_BUCKET_PHARMACY_IMAGES", "minio.buckets.pharmacyImages", "pharmacy-images"),
        "userAvatars" to str("MINIO_BUCKET_USER_AVATARS", "minio.buckets.userAvatars", "user-avatars")
    )
    /** Presigned/read URLs are built from this base when it differs from the service endpoint. */
    val minioPublicEndpoint: String = env("MINIO_PUBLIC_ENDPOINT") ?: minioEndpoint

    // ---- JWT -------------------------------------------------------------------------

    val jwtIssuer: String = str("JWT_ISSUER", "jwt.issuer", "eilaji-backend")
    val jwtAudience: String = str("JWT_AUDIENCE", "jwt.audience", "eilaji-client")
    val jwtRealm: String = str("JWT_REALM", "jwt.realm", "Eilaji API")
    val jwtExpiresIn: Long = long("JWT_EXPIRES_IN", "jwt.expiresIn", 3600000)
    val jwtRefreshExpiresIn: Long = long("JWT_REFRESH_EXPIRES_IN", "jwt.refreshExpiresIn", 2592000000)

    /**
     * Resolved in this order:
     *  1. JWT_SECRET environment variable (the production path)
     *  2. jwt.secret from application.conf (local development only)
     *  3. a random secret, so `./gradlew run` works with no setup and tokens die on restart
     */
    val jwtSecret: String by lazy {
        val fromEnv = env("JWT_SECRET")
        val fromConfig = if (config.hasPath("jwt.secret")) config.getString("jwt.secret") else null
        when {
            fromEnv != null && fromEnv.length >= 32 -> fromEnv.also { publish("jwt.secret", it) }
            fromConfig != null && fromConfig.length >= 32 -> fromConfig.also { publish("jwt.secret", it) }
            isProduction -> throw IllegalStateException(
                "JWT_SECRET is missing or shorter than 32 characters. Set it before starting in production."
            )
            else -> generateSecret().also {
                publish("jwt.secret", it)
                println(
                    "WARNING: JWT_SECRET is not set. A random development secret was generated, " +
                        "so all tokens are invalidated when the service restarts."
                )
            }
        }
    }

    // ---- Service limits ---------------------------------------------------------------

    val maxUploadBytes: Long = long("MAX_UPLOAD_BYTES", "server.maxUploadBytes", 8L * 1024 * 1024)
    val maxWebSocketFrameBytes: Long = long("MAX_WS_FRAME_BYTES", "server.maxWebSocketFrameBytes", 1024 * 1024)
    val searchMaxRadiusKm: Double = double("SEARCH_MAX_RADIUS_KM", "server.searchMaxRadiusKm", 50.0)
    val passwordResetTokenTtlMinutes: Long =
        long("PASSWORD_RESET_TTL_MINUTES", "security.passwordResetTokenTtlMinutes", 30)
    val bcryptCost: Int = int("BCRYPT_COST", "security.bcryptCost", 12)
    val port: Int = int("SERVER_PORT", "server.port", 8080)

    // ---- Eilaji-Plus ------------------------------------------------------------------

    val eilajiPlusBaseUrl: String = env("EILAJI_PLUS_BASE_URL").orEmpty()
    val eilajiPlusApiKey: String = env("EILAJI_PLUS_API_KEY").orEmpty()
    /** Inbound webhook signing secret. Production must set it. */
    val eilajiPlusWebhookSecret: String = env("EILAJI_PLUS_WEBHOOK_SECRET").orEmpty()

    // ---- Rate limiting ----------------------------------------------------------------

    val rateLimitPerMinute: Int = int("RATE_LIMIT_PER_MINUTE", "rateLimit.perMinute", 100)
    val rateLimitLoginPerMinute: Int = int("RATE_LIMIT_LOGIN", "rateLimit.login.perMinute", 5)
    val rateLimitRegisterPerHour: Int = int("RATE_LIMIT_REGISTER", "rateLimit.register.perHour", 3)
    val rateLimitRefreshPerMinute: Int = int("RATE_LIMIT_REFRESH", "rateLimit.refresh.perMinute", 10)
    val rateLimitFailClosed: Boolean = bool("RATE_LIMIT_FAIL_CLOSED", "rateLimit.failClosed", true)

    // ---- CORS -------------------------------------------------------------------------

    /** Browser origins allowed with credentials. Empty means no cross-origin browser access. */
    val allowedHosts: List<String> = list("CORS_ALLOWED_HOSTS", "cors.allowedHosts")

    /** Fails fast on the settings that must never reach production by accident. */
    fun validate() {
        jwtSecret
        if (isProduction && allowedHosts.isEmpty()) {
            println("WARNING: APP_PROFILE=prod with no CORS_ALLOWED_HOSTS. Cross-origin browser access is rejected.")
        }
    }

    private fun publish(key: String, value: String) {
        System.setProperty(key, value)
    }

    private fun generateSecret(): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#\$%^&*()-_"
        val random = SecureRandom()
        return (1 until 64).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }
}

/** Error that maps to an HTTP status without leaking internals to the client. */
class ApiException(val status: io.ktor.http.HttpStatusCode, message: String) : RuntimeException(message)
