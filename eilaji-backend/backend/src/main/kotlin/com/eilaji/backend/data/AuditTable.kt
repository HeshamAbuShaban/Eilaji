package com.eilaji.backend.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp as jdaTimestamp
import java.time.Instant
import java.util.UUID

object AuditLogs : Table("audit_logs") {
    val id = uuid("id").autoGenerate()
    val eventType = varchar("event_type", 50)
    val userId = varchar("user_id", 36).nullable()
    val ipAddress = varchar("ip_address", 45).nullable() // Support IPv6
    val userAgent = text("user_agent").nullable()
    val resourceType = varchar("resource_type", 50).nullable()
    val resourceId = varchar("resource_id", 36).nullable()
    val description = text("description").nullable()
    val metadata = text("metadata").default("")
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
