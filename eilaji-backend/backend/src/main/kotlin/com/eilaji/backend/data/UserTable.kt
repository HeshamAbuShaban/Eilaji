package com.eilaji.backend.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.javatime.timestamp as jdaTimestamp
import java.time.Instant

object Users : Table("users") {
    val id = uuid("id").autoGenerate()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 255)
    val phone = varchar("phone", 20).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val fcmToken = varchar("fcm_token", 500).nullable()
    val role = enumerationByName("role", 20, UserRole::class)
    val isVerified = bool("is_verified").default(false)
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }
    
    override val primaryKey = PrimaryKey(id)
}

enum class UserRole {
    PATIENT,
    PHARMACIST,
    DOCTOR,
    ADMIN
}
