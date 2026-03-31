package com.eilaji.backend.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp as jdaTimestamp
import java.time.Instant

object Pharmacies : Table("pharmacies") {
    val id = uuid("id").autoGenerate()
    val ownerUserId = reference("owner_user_id", Users.id).onDeleteCascade()
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val address = text("address")
    val city = varchar("city", 100).nullable()
    val latitude = double("latitude")
    val longitude = double("longitude")
    val phone = varchar("phone", 20)
    val licenseNumber = varchar("license_number", 100).nullable()
    val isOpen = bool("is_open").default(true)
    val openingHours = jsonb("opening_hours").nullable()
    val ratingAvg = decimal("rating_avg", 3, 2).default(0.0)
    val totalRatings = integer("total_ratings").default(0)
    val isVerified = bool("is_verified").default(false)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }
    
    override val primaryKey = PrimaryKey(id)
}
