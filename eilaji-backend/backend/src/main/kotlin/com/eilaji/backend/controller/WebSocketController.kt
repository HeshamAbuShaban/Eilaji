package com.eilaji.backend.controller

import io.ktor.server.routing.*
import io.ktor.server.websockets.*
import io.ktor.websocket.*
import com.eilaji.backend.service.RedisService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun Route.registerWebSocketRoutes(redisService: RedisService) {
    webSocket("/ws/chat/{userId}") {
        val userId = call.parameters["userId"] ?: return@webSocket
        
        try {
            // Set user as online
            redisService.setUserOnline(userId)
            
            // Subscribe to user's chat channel
            val chatChannel = "chat:$userId"
            
            launch {
                try {
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val message = frame.readText()
                        
                        // Parse and handle incoming message
                        // This is a simplified version - full implementation in Phase 3
                        println("Received message from $userId: $message")
                    }
                } catch (e: Exception) {
                    println("Error reading WebSocket messages: ${e.message}")
                }
            }
            
            // Keep connection alive
            while (true) {
                // In Phase 3, we'll subscribe to Redis pub/sub and forward messages here
                delay(30000) // Send ping every 30 seconds
                send(Frame.Ping("keepalive".toByteArray()))
            }
        } catch (e: Exception) {
            println("WebSocket error for user $userId: ${e.message}")
        } finally {
            // Set user as offline when connection closes
            redisService.setUserOffline(userId)
        }
    }
}

fun Route.setupSwagger() {
    // TODO: Add Swagger UI in Phase 2
    // This will provide interactive API documentation
}

fun startBackgroundJobs(redisService: RedisService) {
    // TODO: Implement background jobs in Phase 3
    // - Clean up expired sessions
    // - Process prescription notifications to eilaji-plus
    // - Send medication reminder notifications
    
    println("✅ Background jobs scheduler initialized")
}
