package com.eilaji.backend.service

import com.eilaji.backend.data.Users
import com.eilaji.backend.dto.PendingNotificationDto
import com.eilaji.backend.dto.PushPayloadDto
import com.eilaji.backend.dto.WebSocketMessage
import com.eilaji.backend.websocket.WebSocketSessionManager
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Self-hosted push delivery. No Firebase, no third party.
 *
 * A notification goes out over the app's own chat WebSocket while the device is connected. When
 * the socket is not alive the payload is parked in Redis (`push:pending:<userId>`) and the app
 * drains it on its next connect or via its periodic fallback worker. This trades FCM's privileged
 * background channel for a connection the app controls; a medication reminder stays a local alarm.
 */
class PushService(
    private val redisService: RedisService,
    private val sessionManager: WebSocketSessionManager
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PushService::class.java)
        private const val PENDING_QUEUE = "push:pending"
        private const val MAX_PENDING = 30
        /** FCM's payload limit; ours is a small JSON frame, so this is generous. */
        private const val MAX_BODY = 512
    }

    data class Payload(
        val title: String,
        val body: String,
        val route: String? = null,
        val targetUserId: String
    )

    /** Registers the device token so later messages know where to go. */
    fun registerToken(userId: String, token: String): Boolean {
        if (token.isBlank() || token.length > 500) return false
        return try {
            transaction {
                Users.update({ Users.id eq java.util.UUID.fromString(userId) }) {
                    it[Users.fcmToken] = token
                }
            }
            true
        } catch (e: Exception) {
            logger.warn("Could not store push token for $userId: ${e.message}")
            false
        }
    }

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    /**
     * Fire-and-forget: delivers now, or parks it for the next connection. Safe to call from a
     * plain request handler; delivery happens on the service scope. Never throws.
     */
    fun send(payload: Payload) {
        scope.launch { deliver(payload) }
    }

    private suspend fun deliver(payload: Payload) {
        val title = payload.title.take(80)
        val body = payload.body.take(MAX_BODY)
        val dto = PushPayloadDto(id = UUID.randomUUID().toString(), title = title, body = body, route = payload.route)
        val frame = WebSocketMessage(type = "PUSH", userId = payload.targetUserId, push = dto, timestamp = Instant.now().toString())

        val delivered = sessionManager.sendToUser(payload.targetUserId, frame)
        if (delivered) {
            logger.debug("Push delivered live to ${payload.targetUserId}")
        } else {
            park(payload.targetUserId, PendingNotificationDto(dto.id, title, body, payload.route, frame.timestamp!!))
            logger.debug("Push queued for ${payload.targetUserId} (device offline)")
        }
    }

    /** Returns and clears everything parked for this user. */
    suspend fun drain(userId: String): List<PendingNotificationDto> = try {
        val items = redisService.getFromRetryQueue(PENDING_QUEUE, MAX_PENDING).mapNotNull { raw ->
            try { Json.decodeFromString<PendingNotificationDto>(raw) } catch (_: Exception) { null }
        }
        items.forEach { redisService.removeFromRetryQueue(PENDING_QUEUE, Json.encodeToString(PendingNotificationDto.serializer(), it)) }
        items
    } catch (e: Exception) {
        logger.warn("Could not drain pending push for $userId: ${e.message}")
        emptyList()
    }

    private suspend fun park(userId: String, dto: PendingNotificationDto) {
        try {
            redisService.addToRetryQueue("$PENDING_QUEUE:$userId", Json.encodeToString(PendingNotificationDto.serializer(), dto))
        } catch (e: Exception) {
            logger.warn("Could not queue push for $userId: ${e.message}")
        }
    }
}