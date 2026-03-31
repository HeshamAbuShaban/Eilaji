package com.eilaji.backend.service

import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPubSub
import com.typesafe.config.ConfigFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChatMessage(
    val chatId: String,
    val senderId: String,
    val messageText: String?,
    val messageImageUrl: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class PresenceUpdate(
    val userId: String,
    val isOnline: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class RedisService {
    private val config = ConfigFactory.load()
    
    private val host: String = config.getString("redis.host")
    private val port: Int = config.getInt("redis.port")
    private val password: String? = config.getString("redis.password").takeIf { it.isNotEmpty() }
    private val database: Int = config.getInt("redis.database")
    
    private val jedis: Jedis
    
    init {
        jedis = if (password != null && password.isNotEmpty()) {
            Jedis(host, port).apply {
                auth(password)
                select(database)
            }
        } else {
            Jedis(host, port).apply {
                select(database)
            }
        }
        
        println("✅ Redis connection initialized: $host:$port")
    }
    
    // ===== Pub/Sub for Real-time Messaging =====
    
    suspend fun publishMessage(channel: String, message: ChatMessage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonMessage = Json.encodeToString(ChatMessage.serializer(), message)
            jedis.publish(channel, jsonMessage)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun subscribeToChannel(channel: String, callback: (ChatMessage) -> Unit): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            jedis.subscribe(object : JedisPubSub() {
                override fun onMessage(channel: String?, message: String?) {
                    message?.let {
                        try {
                            val chatMessage = Json.decodeFromString(ChatMessage.serializer(), it)
                            callback(chatMessage)
                        } catch (e: Exception) {
                            println("❌ Error parsing message: ${e.message}")
                        }
                    }
                }
            }, channel)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Presence Management (Online/Offline Status) =====
    
    suspend fun setUserOnline(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val presence = PresenceUpdate(userId, true)
            val jsonPresence = Json.encodeToString(PresenceUpdate.serializer(), presence)
            jedis.setex("presence:$userId", 300, jsonPresence) // 5 minutes TTL
            jedis.publish("presence:updates", jsonPresence)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun setUserOffline(userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val presence = PresenceUpdate(userId, false)
            val jsonPresence = Json.encodeToString(PresenceUpdate.serializer(), presence)
            jedis.del("presence:$userId")
            jedis.publish("presence:updates", jsonPresence)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isUserOnline(userId: String): Boolean = withContext(Dispatchers.IO) {
        jedis.exists("presence:$userId")
    }
    
    // ===== Caching =====
    
    suspend fun cacheValue(key: String, value: String, ttlSeconds: Int = 3600): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            jedis.setex(key, ttlSeconds, value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCachedValue(key: String): String? = withContext(Dispatchers.IO) {
        jedis.get(key)
    }
    
    suspend fun deleteCache(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            jedis.del(key)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Rate Limiting =====
    
    suspend fun checkRateLimit(key: String, maxRequests: Int = 10, windowSeconds: Int = 1): Boolean = withContext(Dispatchers.IO) {
        val currentCount = jedis.incr(key)
        if (currentCount == 1L) {
            jedis.expire(key, windowSeconds)
        }
        currentCount <= maxRequests
    }
    
    // ===== Session Management =====
    
    suspend fun storeSession(sessionId: String, userId: String, ttlSeconds: Int = 86400): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            jedis.setex("session:$sessionId", ttlSeconds, userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getSessionUserId(sessionId: String): String? = withContext(Dispatchers.IO) {
        jedis.get("session:$sessionId")
    }
    
    suspend fun invalidateSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            jedis.del("session:$sessionId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ===== Close Connection =====
    
    fun close() {
        jedis.close()
        println("Redis connection closed")
    }
}
