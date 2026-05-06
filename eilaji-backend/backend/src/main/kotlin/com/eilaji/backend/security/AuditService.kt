package com.eilaji.backend.security

import com.eilaji.backend.data.AuditLogs
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

object AuditService {

    enum class EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        PASSWORD_CHANGE,
        PASSWORD_RESET,
        PROFILE_UPDATE,
        USER_CREATED,
        USER_UPDATED,
        USER_DEACTIVATED,
        ORDER_CREATED,
        ORDER_UPDATED,
        PRESCRIPTION_UPLOADED,
        DATA_ACCESS,
        PERMISSION_DENIED,
        SUSPICIOUS_ACTIVITY
    }

    fun logEvent(
        eventType: EventType,
        userId: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
        resourceType: String? = null,
        resourceId: String? = null,
        description: String? = null,
        metadata: Map<String, Any?>? = null
    ) {
        try {
            transaction {
                AuditLogs.insert {
                    it[id] = UUID.randomUUID()
                    it[AuditLogs.eventType] = eventType.name
                    it[AuditLogs.userId] = userId
                    it[AuditLogs.ipAddress] = ipAddress
                    it[AuditLogs.userAgent] = userAgent
                    it[AuditLogs.resourceType] = resourceType
                    it[AuditLogs.resourceId] = resourceId
                    it[AuditLogs.description] = description
                    it[AuditLogs.metadata] = metadata?.toString() ?: ""
                    it[createdAt] = java.sql.Timestamp.from(Instant.now())
                }
            }
        } catch (e: Exception) {
            // Log to console as fallback
            println("AUDIT LOG FAILED: $eventType - ${e.message}")
        }
    }

    fun logLoginSuccess(userId: String, ipAddress: String?, userAgent: String?) {
        logEvent(
            eventType = EventType.LOGIN_SUCCESS,
            userId = userId,
            ipAddress = ipAddress,
            userAgent = userAgent,
            description = "User logged in successfully"
        )
    }

    fun logLoginFailure(email: String?, ipAddress: String?, reason: String?) {
        logEvent(
            eventType = EventType.LOGIN_FAILURE,
            ipAddress = ipAddress,
            description = "Failed login attempt for email: $email - Reason: $reason"
        )
    }

    fun logDataAccess(userId: String, resourceType: String, resourceId: String, ipAddress: String?) {
        logEvent(
            eventType = EventType.DATA_ACCESS,
            userId = userId,
            ipAddress = ipAddress,
            resourceType = resourceType,
            resourceId = resourceId,
            description = "User accessed $resourceType with ID: $resourceId"
        )
    }

    fun logPermissionDenied(userId: String?, resourceType: String, resourceId: String?, ipAddress: String?) {
        logEvent(
            eventType = EventType.PERMISSION_DENIED,
            userId = userId,
            ipAddress = ipAddress,
            resourceType = resourceType,
            resourceId = resourceId,
            description = "Permission denied for $resourceType"
        )
    }

    fun logSuspiciousActivity(userId: String?, description: String, ipAddress: String?) {
        logEvent(
            eventType = EventType.SUSPICIOUS_ACTIVITY,
            userId = userId,
            ipAddress = ipAddress,
            description = description
        )
    }
}
