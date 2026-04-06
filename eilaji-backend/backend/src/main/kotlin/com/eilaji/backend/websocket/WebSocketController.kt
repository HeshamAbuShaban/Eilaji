package com.eilaji.backend.websocket

import com.eilaji.backend.dto.WebSocketMessage
import com.eilaji.backend.service.ChatService
import com.eilaji.backend.service.MessageService
import com.eilaji.backend.service.RedisService
import io.ktor.server.auth.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class WebSocketController(
    private val sessionManager: WebSocketSessionManager,
    private val redisService: RedisService,
    private val messageService: MessageService
) {
    companion object {
        private val logger = LoggerFactory.getLogger(WebSocketController::class.java)
        private const val HEARTBEAT_INTERVAL_MS = 10000L // 10 seconds
    }
    
    suspend fun handleWebSocketSession(session: DefaultWebSocketServerSession, userId: String) {
        val heartbeatJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                redisService.refreshOnlineStatus(userId)
            }
        }
        
        try {
            // Add session to manager
            sessionManager.addSession(userId, session)
            
            // Send welcome message
            session.send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(), 
                WebSocketMessage(type = "CONNECTED", userId = userId))))
            
            logger.info("WebSocket connection established for user $userId")
            
            // Process incoming messages
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        try {
                            val message = Json.decodeFromString<WebSocketMessage>(frame.readText())
                            handleMessage(session, userId, message)
                        } catch (e: Exception) {
                            logger.error("Error parsing WebSocket message: ${e.message}", e)
                            sendError(session, "Invalid message format")
                        }
                    }
                    is Frame.Close -> {
                        logger.info("WebSocket close frame received for user $userId")
                        break
                    }
                    else -> {
                        logger.debug("Received unknown frame type for user $userId")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket error for user $userId: ${e.message}", e)
        } finally {
            // Clean up
            heartbeatJob.cancel()
            sessionManager.removeSession(userId, session)
            
            try {
                session.close(CloseReason(CloseReason.Codes.NORMAL, "Connection closed"))
            } catch (e: Exception) {
                logger.warn("Error closing WebSocket session: ${e.message}")
            }
        }
    }
    
    private suspend fun handleMessage(
        session: DefaultWebSocketServerSession, 
        userId: String, 
        message: WebSocketMessage
    ) {
        when (message.type.uppercase()) {
            "JOIN" -> {
                val chatId = message.chatId ?: return sendError(session, "chatId required for JOIN")
                sessionManager.joinChat(userId, chatId)
                session.send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(),
                    WebSocketMessage(type = "JOINED", chatId = chatId, userId = userId))))
            }
            
            "LEAVE" -> {
                val chatId = message.chatId ?: return sendError(session, "chatId required for LEAVE")
                sessionManager.leaveChat(userId, chatId)
                session.send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(),
                    WebSocketMessage(type = "LEFT", chatId = chatId, userId = userId))))
            }
            
            "MESSAGE" -> {
                val chatId = message.chatId ?: return sendError(session, "chatId required for MESSAGE")
                val content = message.content ?: return sendError(session, "content required for MESSAGE")
                
                val sentMessage = sessionManager.sendMessage(
                    userId = userId,
                    chatId = chatId,
                    content = content,
                    messageType = message.message?.messageType ?: "TEXT",
                    attachmentUrl = message.message?.attachmentUrl
                )
                
                if (sentMessage != null) {
                    session.send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(),
                        WebSocketMessage(type = "MESSAGE_SENT", chatId = chatId, message = sentMessage))))
                } else {
                    sendError(session, "Failed to send message")
                }
            }
            
            "READ" -> {
                val chatId = message.chatId ?: return sendError(session, "chatId required for READ")
                sessionManager.markAsRead(userId, chatId)
            }
            
            "PING" -> {
                session.send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(),
                    WebSocketMessage(type = "PONG", timestamp = java.time.Instant.now()))))
            }
            
            else -> {
                sendError(session, "Unknown message type: ${message.type}")
            }
        }
    }
    
    private suspend fun sendError(session: DefaultWebSocketServerSession, errorMessage: String) {
        try {
            session.send(Frame.Text(Json.encodeToString(WebSocketMessage.serializer(),
                WebSocketMessage(type = "ERROR", content = errorMessage))))
        } catch (e: Exception) {
            logger.error("Error sending error message: ${e.message}")
        }
    }
}
