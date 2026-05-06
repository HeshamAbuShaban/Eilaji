package com.eilaji.backend.service

import redis.clients.jedis.JedisPool
import redis.clients.jedis.Jedis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.time.Duration

class RedisService(
    private val redisUrl: String = "redis://localhost:6379"
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RedisService::class.java)
        private const val PRESENCE_KEY_PREFIX = "user:online:"
        private const val PRESENCE_TTL_SECONDS = 30L
        private const val HEARTBEAT_INTERVAL_MS = 10000L // 10 seconds
    }

    private val jedisPool: JedisPool = parseRedisUrl(redisUrl)

    private fun parseRedisUrl(url: String): JedisPool {
        val parsed = if (url.startsWith("redis://")) url.substring(8) else url
        val parts = parsed.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toInt() else 6379
        return JedisPool(host, port)
    }

    private suspend fun <T> withJedis(block: (Jedis) -> T): T = withContext(Dispatchers.IO) {
        jedisPool.resource.use { jedis -> block(jedis) }
    }

    suspend fun setOnlineStatus(userId: String, isOnline: Boolean) {
        withJedis { jedis ->
            val key = "$PRESENCE_KEY_PREFIX$userId"

            if (isOnline) {
                // Set with TTL - will auto-expire if heartbeat stops
                jedis.setex(key, PRESENCE_TTL_SECONDS, "true")
                logger.debug("Set user $userId as online")
            } else {
                // Remove the key immediately
                jedis.del(key)
                logger.debug("Set user $userId as offline")
            }
        }
    }

    suspend fun refreshOnlineStatus(userId: String) {
        withJedis { jedis ->
            val key = "$PRESENCE_KEY_PREFIX$userId"

            // Refresh TTL only if key exists (user is online)
            val exists = jedis.exists(key)
            if (exists) {
                jedis.expire(key, PRESENCE_TTL_SECONDS)
                logger.trace("Refreshed presence TTL for user $userId")
            }
        }
    }

    suspend fun isUserOnline(userId: String): Boolean {
        return try {
            withJedis { jedis ->
                val key = "$PRESENCE_KEY_PREFIX$userId"
                jedis.exists(key)
            }
        } catch (e: Exception) {
            logger.error("Error checking presence for user $userId: ${e.message}", e)
            false
        }
    }

    suspend fun getOnlineUsers(): Set<String> {
        return try {
            withJedis { jedis ->
                val keys = jedis.keys("$PRESENCE_KEY_PREFIX*")

                keys.mapNotNull { key ->
                    key.removePrefix(PRESENCE_KEY_PREFIX)
                }.toSet()
            }
        } catch (e: Exception) {
            logger.error("Error getting online users: ${e.message}", e)
            emptySet()
        }
    }

    suspend fun storeInCache(key: String, value: String, ttlSeconds: Long = 3600) {
        withJedis { jedis ->
            jedis.setex(key, ttlSeconds, value)
        }
    }

    suspend fun getFromCache(key: String): String? {
        return try {
            withJedis { jedis ->
                jedis.get(key)
            }
        } catch (e: Exception) {
            logger.error("Error getting from cache: ${e.message}", e)
            null
        }
    }

    suspend fun deleteFromCache(key: String) {
        withJedis { jedis ->
            jedis.del(key)
        }
    }

    // Retry queue operations using Redis lists
    suspend fun addToRetryQueue(queueName: String, itemId: String) {
        withJedis { jedis ->
            jedis.lpush("retry:$queueName", itemId)
        }
    }

    suspend fun getFromRetryQueue(queueName: String, count: Int = 10): List<String> {
        return try {
            withJedis { jedis ->
                jedis.lrange("retry:$queueName", 0, (count - 1).toLong())
            }
        } catch (e: Exception) {
            logger.error("Error getting from retry queue: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun removeFromRetryQueue(queueName: String, itemId: String) {
        withJedis { jedis ->
            jedis.lrem("retry:$queueName", 1, itemId)
        }
    }

    suspend fun incrementKey(key: String): Long {
        return try {
            withJedis { jedis ->
                jedis.incr(key)
            }
        } catch (e: Exception) {
            logger.error("Error incrementing key: ${e.message}", e)
            0L
        }
    }

    suspend fun expireKey(key: String, seconds: Long) {
        withJedis { jedis ->
            jedis.expire(key, seconds)
        }
    }

    fun close() {
        try {
            jedisPool.close()
        } catch (e: Exception) {
            logger.error("Error shutting down Redis client: ${e.message}", e)
        }
    }
}
