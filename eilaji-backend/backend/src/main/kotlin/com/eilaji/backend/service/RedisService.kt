package com.eilaji.backend.service

import io.lettuce.core.RedisClient
import io.lettuce.core.api.async.RedisAsyncCommands
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
    
    private val redisClient: RedisClient = RedisClient.create(redisUrl)
    
    suspend fun setOnlineStatus(userId: String, isOnline: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                val key = "$PRESENCE_KEY_PREFIX$userId"
                
                if (isOnline) {
                    // Set with TTL - will auto-expire if heartbeat stops
                    connection.setex(key, PRESENCE_TTL_SECONDS, "true").get()
                    logger.debug("Set user $userId as online")
                } else {
                    // Remove the key immediately
                    connection.del(key).get()
                    logger.debug("Set user $userId as offline")
                }
                
                connection.close()
            } catch (e: Exception) {
                logger.error("Error updating presence for user $userId: ${e.message}", e)
            }
        }
    }
    
    suspend fun refreshOnlineStatus(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                val key = "$PRESENCE_KEY_PREFIX$userId"
                
                // Refresh TTL only if key exists (user is online)
                val exists = connection.exists(key).get()
                if (exists > 0) {
                    connection.expire(key, PRESENCE_TTL_SECONDS).get()
                    logger.trace("Refreshed presence TTL for user $userId")
                }
                
                connection.close()
            } catch (e: Exception) {
                logger.debug("Could not refresh presence for user $userId (may be offline): ${e.message}")
            }
        }
    }
    
    suspend fun isUserOnline(userId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                val key = "$PRESENCE_KEY_PREFIX$userId"
                val exists = connection.exists(key).get()
                connection.close()
                exists > 0
            } catch (e: Exception) {
                logger.error("Error checking presence for user $userId: ${e.message}", e)
                false
            }
        }
    }
    
    suspend fun getOnlineUsers(): Set<String> {
        return withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                val keys = connection.keys("$PRESENCE_KEY_PREFIX*").get()
                connection.close()
                
                keys.mapNotNull { key ->
                    key.removePrefix(PRESENCE_KEY_PREFIX)
                }.toSet()
            } catch (e: Exception) {
                logger.error("Error getting online users: ${e.message}", e)
                emptySet()
            }
        }
    }
    
    suspend fun storeInCache(key: String, value: String, ttlSeconds: Long = 3600) {
        withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                connection.setex(key, ttlSeconds, value).get()
                connection.close()
            } catch (e: Exception) {
                logger.error("Error storing in cache: ${e.message}", e)
            }
        }
    }
    
    suspend fun getFromCache(key: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                val value = connection.get(key).get()
                connection.close()
                value
            } catch (e: Exception) {
                logger.error("Error getting from cache: ${e.message}", e)
                null
            }
        }
    }
    
    suspend fun deleteFromCache(key: String) {
        withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                connection.del(key).get()
                connection.close()
            } catch (e: Exception) {
                logger.error("Error deleting from cache: ${e.message}", e)
            }
        }
    }
    
    // Retry queue operations using Redis lists
    suspend fun addToRetryQueue(queueName: String, itemId: String) {
        withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                connection.lpush("retry:$queueName", itemId).get()
                connection.close()
            } catch (e: Exception) {
                logger.error("Error adding to retry queue: ${e.message}", e)
            }
        }
    }
    
    suspend fun getFromRetryQueue(queueName: String, count: Int = 10): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                val items = connection.lrange("retry:$queueName", 0, (count - 1).toLong()).get()
                connection.close()
                items.toList()
            } catch (e: Exception) {
                logger.error("Error getting from retry queue: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    suspend fun removeFromRetryQueue(queueName: String, itemId: String) {
        withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                connection.lrem("retry:$queueName", 1, itemId).get()
                connection.close()
            } catch (e: Exception) {
                logger.error("Error removing from retry queue: ${e.message}", e)
            }
        }
    }
    
    suspend fun incrementKey(key: String): Long {
        return withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                val count = connection.incr(key).get()
                connection.close()
                count
            } catch (e: Exception) {
                logger.error("Error incrementing key: ${e.message}", e)
                0L
            }
        }
    }
    
    suspend fun expireKey(key: String, seconds: Long) {
        withContext(Dispatchers.IO) {
            try {
                val connection = redisClient.connect().async()
                connection.expire(key, seconds).get()
                connection.close()
            } catch (e: Exception) {
                logger.error("Error setting expiration on key: ${e.message}", e)
            }
        }
    }
    
    fun close() {
        try {
            redisClient.shutdown()
        } catch (e: Exception) {
            logger.error("Error shutting down Redis client: ${e.message}", e)
        }
    }
}
