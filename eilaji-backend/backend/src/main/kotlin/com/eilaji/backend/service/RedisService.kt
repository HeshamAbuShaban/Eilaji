package com.eilaji.backend.service

import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * Redis access: presence, cache, retry queue, rate limiting counters and revoked tokens.
 *
 * The URL supports credentials: redis://:password@host:port/0
 */
class RedisService(
    private val redisUrl: String = "redis://localhost:6379"
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisService::class.java)
        private const val PRESENCE_KEY_PREFIX = "user:online:"
        private const val PRESENCE_TTL_SECONDS = 30L
        private const val REVOKED_PREFIX = "auth:revoked:"
    }

    private val jedisPool: JedisPool = parseRedisUrl(redisUrl)

    private fun parseRedisUrl(url: String): JedisPool {
        var rest = url.removePrefix("redis://").removePrefix("rediss://")
        var password: String? = null
        var database = 0
        // redis://:password@host:port/db  |  redis://user:password@host:port/db
        val at = rest.lastIndexOf('@')
        if (at > 0) {
            val credentials = rest.substring(0, at)
            rest = rest.substring(at + 1)
            password = credentials.substringAfter(':', "").ifEmpty { null }
        }
        val dbIndex = rest.indexOf('/')
        if (dbIndex >= 0) {
            database = rest.substring(dbIndex + 1).toIntOrNull() ?: 0
            rest = rest.substring(0, dbIndex)
        }
        val parts = rest.split(":")
        val host = parts[0].ifEmpty { "localhost" }
        val port = if (parts.size > 1) parts[1].toIntOrNull() ?: 6379 else 6379
        val poolConfig = JedisPoolConfig().apply {
            maxTotal = 16
            maxIdle = 8
            minIdle = 2
            testOnBorrow = true
        }
        val useTls = url.startsWith("rediss://")
        return if (password != null) {
            if (useTls) JedisPool(poolConfig, host, port, 2000, 2000, password, database, null, true)
            else JedisPool(poolConfig, host, port, 2000, 2000, password, database, null, false)
        } else {
            if (useTls) JedisPool(poolConfig, host, port, 2000, 2000, null, database, null, true)
            else JedisPool(poolConfig, host, port, 2000, 2000, null, database, null, false)
        }
    }

    private suspend fun <T> withJedis(block: (redis.clients.jedis.Jedis) -> T): T = withContext(Dispatchers.IO) {
        jedisPool.resource.use { jedis -> block(jedis) }
    }

    /** True when Redis answers a PING. Used to decide fail-open vs fail-closed behaviour. */
    fun isAvailable(): Boolean = try {
        jedisPool.resource.use { it.ping() != null }
    } catch (e: Exception) {
        logger.warn("Redis unavailable: ${e.message}")
        false
    }

    suspend fun setOnlineStatus(userId: String, isOnline: Boolean) {
        withJedis { jedis ->
            val key = "$PRESENCE_KEY_PREFIX$userId"
            if (isOnline) {
                jedis.setex(key, PRESENCE_TTL_SECONDS, "true")
                logger.debug("Set user $userId as online")
            } else {
                jedis.del(key)
                logger.debug("Set user $userId as offline")
            }
        }
    }

    suspend fun refreshOnlineStatus(userId: String) {
        withJedis { jedis ->
            val key = "$PRESENCE_KEY_PREFIX$userId"
            if (jedis.exists(key)) {
                jedis.expire(key, PRESENCE_TTL_SECONDS)
                logger.trace("Refreshed presence TTL for user $userId")
            }
        }
    }

    suspend fun isUserOnline(userId: String): Boolean = try {
        withJedis { jedis -> jedis.exists("$PRESENCE_KEY_PREFIX$userId") }
    } catch (e: Exception) {
        logger.error("Error checking presence for user $userId: ${e.message}", e)
        false
    }

    suspend fun getOnlineUsers(): Set<String> = try {
        withJedis { jedis ->
            jedis.keys("$PRESENCE_KEY_PREFIX*").mapNotNull { it.removePrefix(PRESENCE_KEY_PREFIX) }.toSet()
        }
    } catch (e: Exception) {
        logger.error("Error getting online users: ${e.message}", e)
        emptySet()
    }

    suspend fun storeInCache(key: String, value: String, ttlSeconds: Long = 3600) {
        withJedis { it.setex(key, ttlSeconds, value) }
    }

    suspend fun getFromCache(key: String): String? = try {
        withJedis { it.get(key) }
    } catch (e: Exception) {
        logger.error("Error getting from cache: ${e.message}", e)
        null
    }

    suspend fun deleteFromCache(key: String) {
        withJedis { it.del(key) }
    }

    suspend fun addToRetryQueue(queueName: String, itemId: String) {
        withJedis { it.lpush("retry:$queueName", itemId) }
    }

    suspend fun getFromRetryQueue(queueName: String, count: Int = 10): List<String> = try {
        withJedis { it.lrange("retry:$queueName", 0, (count - 1).toLong()) }
    } catch (e: Exception) {
        logger.error("Error getting from retry queue: ${e.message}", e)
        emptyList()
    }

    suspend fun removeFromRetryQueue(queueName: String, itemId: String) {
        withJedis { it.lrem("retry:$queueName", 1, itemId) }
    }

    suspend fun incrementKey(key: String): Long = withJedis { it.incr(key) }

    suspend fun expireKey(key: String, seconds: Long) {
        withJedis { it.expire(key, seconds) }
    }

    // ---- Revoked tokens (logout) -------------------------------------------------

    /** Blocking variant used from the synchronous JWT validation path. Returns false when Redis is down. */
    fun revokeTokenSync(jti: String, ttlSeconds: Long): Boolean = try {
        jedisPool.resource.use { it.setex("$REVOKED_PREFIX$jti", ttlSeconds, "1") }
        true
    } catch (e: Exception) {
        logger.warn("Could not store token revocation: ${e.message}")
        false
    }

    fun isTokenRevokedSync(jti: String): Boolean = try {
        jedisPool.resource.use { it.exists("$REVOKED_PREFIX$jti") }
    } catch (e: Exception) {
        // Fail open: an unreachable Redis must not lock every user out.
        false
    }

    fun close() {
        try {
            jedisPool.close()
        } catch (e: Exception) {
            logger.error("Error shutting down Redis client: ${e.message}", e)
        }
    }
}
