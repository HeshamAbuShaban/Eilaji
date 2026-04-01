package com.eilaji.backend.websocket

import com.eilaji.backend.dto.MessageDto
import com.eilaji.backend.dto.WebSocketMessage
import com.eilaji.backend.service.ChatService
import com.eilaji.backend.service.MessageService
import com.eilaji.backend.service.RedisService
import io.ktor.server.auth.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class WebSocketSessionManager(
    private val messageService: MessageService,
    private val chatService: ChatService,
    private val redisService: RedisService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketSessionManager::class.java)
    }
    
    // Map of userId to their active sessions
    private val userSessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()
    
    // Map of chatId to subscribed user IDs
    private val chatSubscribers = ConcurrentHashMap<Long, MutableSet<String>>()
    
    // Mutex for thread-safe operations
    private val sessionMutex = Mutex()
    
    suspend fun addSession(userId: String, session: DefaultWebSocketServerSession) {
        sessionMutex.withLock {
            userSessions.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
            
            // Update presence in Redis
            redisService.setOnlineStatus(userId, true)
            
            // Broadcast presence update
            broadcastPresence(userId, true)
            
            logger.info("User $userId connected. Total sessions: ${userSessions[userId]?.size}")
        }
    }
    
    suspend fun removeSession(userId: String, session: DefaultWebSocketServerSession) {
        sessionMutex.withLock {
            userSessions[userId]?.remove(session)
            
            if (userSessions[userId].isNullOrEmpty()) {
                userSessions.remove(userId)
                
                // Update presence in Redis (will expire after TTL)
                redisService.setOnlineStatus(userId, false)
                
                // Broadcast presence update
                broadcastPresence(userId, false)
            }
            
            // Remove from all chat subscriptions
            chatSubscribers.values.forEach { it.remove(userId) }
            
            logger.info("User $userId disconnected. Remaining sessions: ${userSessions[userId]?.size ?: 0}")
        }
    }
    
    suspend fun joinChat(userId: String, chatId: Long) {
        sessionMutex.withLock {
            chatSubscribers.computeIfAbsent(chatId) { ConcurrentHashMap.newKeySet() }.add(userId)
            logger.info("User $userId joined chat $chatId")
        }
    }
    
    suspend fun leaveChat(userId: String, chatId: Long) {
        sessionMutex.withLock {
            chatSubscribers[chatId]?.remove(userId)
            if (chatSubscribers[chatId].isNullOrEmpty()) {
                chatSubscribers.remove(chatId)
            }
            logger.info("User $userId left chat $chatId")
        }
    }
    
    suspend fun sendMessage(userId: String, chatId: Long, content: String, messageType: String = "TEXT", attachmentUrl: String? = null): MessageDto? {
        return try {
            // Save message to database
            val message = messageService.createMessage(chatId, userId, 
                com.eilaji.backend.dto.SendMessageRequest(content, messageType, attachmentUrl))
            
            // Update chat's last message
            chatService.updateLastMessage(chatId, content)
            
            // Broadcast to all subscribers
            val wsMessage = WebSocketMessage(
                type = "MESSAGE",
                chatId = chatId,
                userId = userId,
                message = message,
                timestamp = message.createdAt
            )
            
            broadcastToChat(chatId, wsMessage)
            
            message
        } catch (e: Exception) {
            logger.error("Error sending message: ${e.message}", e)
            null
        }
    }
    
    suspend fun markAsRead(userId: String, chatId: Long, messageIds: List<Long>? = null) {
        try {
            val markedMessages = if (messageIds != null) {
                messageService.markMessagesAsRead(messageIds, userId)
            } else {
                messageService.markChatAsRead(chatId, userId)
                messageService.getMessagesForChat(chatId).items.filter { !it.isRead }
            }
            
            // Broadcast read receipt to chat participants
            val wsMessage = WebSocketMessage(
                type = "READ",
                chatId = chatId,
                userId = userId,
                timestamp = java.time.Instant.now()
            )
            
            broadcastToChat(chatId, wsMessage)
        } catch (e: Exception) {
            logger.error("Error marking messages as read: ${e.message}", e)
        }
    }
    
    private suspend fun broadcastToChat(chatId: Long, message: WebSocketMessage) {
        sessionMutex.withLock {
            val subscribers = chatSubscribers[chatId] ?: return
            val json = Json.encodeToString(WebSocketMessage.serializer(), message)
            
            subscribers.forEach { userId ->
                userSessions[userId]?.forEach { session ->
                    try {
                        session.send(Frame.Text(json))
                    } catch (e: Exception) {
                        logger.warn("Failed to send message to user $userId: ${e.message}")
                    }
                }
            }
        }
    }
    
    private suspend fun broadcastPresence(userId: String, isOnline: Boolean) {
        val message = WebSocketMessage(
            type = "PRESENCE",
            userId = userId,
            isOnline = isOnline,
            timestamp = java.time.Instant.now()
        )
        
        sessionMutex.withLock {
            val json = Json.encodeToString(WebSocketMessage.serializer(), message)
            
            // Send to all sessions except the user's own sessions
            userSessions.forEach { (otherUserId, sessions) ->
                if (otherUserId != userId) {
                    sessions.forEach { session ->
                        try {
                            session.send(Frame.Text(json))
                        } catch (e: Exception) {
                            logger.warn("Failed to send presence update: ${e.message}")
                        }
                    }
                }
            }
        }
    }
    
    suspend fun getOnlineUsers(): Set<String> {
        return sessionMutex.withLock {
            userSessions.keys.toSet()
        }
    }
    
    fun getUserSessions(userId: String): Int {
        return userSessions[userId]?.size ?: 0
    }
}
