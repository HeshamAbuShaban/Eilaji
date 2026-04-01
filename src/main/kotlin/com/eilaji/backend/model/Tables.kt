package com.eilaji.backend.model

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp

// Users table
object Users : IdTable<String>("users") {
    override val id: Column<EntityID<String>> = varchar("id", 255).entityId()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 255)
    val phone = varchar("phone", 20).nullable()
    val role = varchar("role", 50) // ADMIN, USER, PHARMACIST
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { java.time.Instant.now() }
}

// Categories table
object Categories : IdTable<Int>("categories") {
    override val id: EntityID<Int> = integer("id").autoIncrement().entityId()
    val nameEn = varchar("name_en", 255)
    val nameAr = varchar("name_ar", 255)
    val description = text("description").nullable()
    val parentId = reference("parent_id", Categories).nullable()
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { java.time.Instant.now() }
}

// Medicines table
object Medicines : IdTable<Int>("medicines") {
    override val id: EntityID<Int> = integer("id").autoIncrement().entityId()
    val titleEn = varchar("title_en", 255)
    val titleAr = varchar("title_ar", 255)
    val description = text("description").nullable()
    val categoryId = reference("category_id", Categories)
    val subcategoryId = reference("sub_category_id", Categories).nullable()
    val manufacturer = varchar("manufacturer", 255).nullable()
    val requiresPrescription = bool("requires_prescription").default(false)
    val price = decimal("price", 10, 2).nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { java.time.Instant.now() }
    
    init {
        index("medicines_search_idx", false, "title_en", "title_ar", "description")
    }
}

// Pharmacies table
object Pharmacies : IdTable<Int>("pharmacies") {
    override val id: EntityID<Int> = integer("id").autoIncrement().entityId()
    val ownerId = reference("owner_id", Users)
    val name = varchar("name", 255)
    val address = text("address")
    val city = varchar("city", 100)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val phone = varchar("phone", 20)
    val isVerified = bool("is_verified").default(false)
    val isOpen = bool("is_open").default(true)
    val openingHours = varchar("opening_hours", 255).nullable()
    val licenseNumber = varchar("license_number", 100).nullable()
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { java.time.Instant.now() }
}

// Prescriptions table
object Prescriptions : IdTable<Int>("prescriptions") {
    override val id: EntityID<Int> = integer("id").autoIncrement().entityId()
    val userId = reference("user_id", Users)
    val pharmacyId = reference("pharmacy_id", Pharmacies).nullable()
    val imageUrl = varchar("image_url", 500)
    val notes = text("notes").nullable()
    val status = varchar("status", 50).default("PENDING") // PENDING, SENT_TO_PHARMACY, RECEIVED_QUOTE, ACCEPTED, REJECTED, COMPLETED
    val quotedPrice = decimal("quoted_price", 10, 2).nullable()
    val pharmacistNotes = text("pharmacist_notes").nullable()
    val eilajiPlusRef = varchar("eilaji_plus_ref", 255).nullable()
    val eilajiPlusStatus = varchar("eilaji_plus_status", 50).nullable()
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { java.time.Instant.now() }
}

// Chats table
object Chats : IdTable<Long>("chats") {
    override val id: EntityID<Long> = long("id").autoIncrement().entityId()
    val prescriptionId = reference("prescription_id", Prescriptions).nullable()
    val pharmacyId = reference("pharmacy_id", Pharmacies).nullable()
    val userId = reference("user_id", Users)
    val lastMessageAt = timestamp("last_message_at").nullable()
    val lastMessage = text("last_message").nullable()
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { java.time.Instant.now() }
}

// Messages table
object Messages : IdTable<Long>("messages") {
    override val id: EntityID<Long> = long("id").autoIncrement().entityId()
    val chatId = reference("chat_id", Chats)
    val senderId = reference("sender_id", Users)
    val content = text("content")
    val messageType = varchar("message_type", 50).default("TEXT") // TEXT, IMAGE, FILE
    val attachmentUrl = varchar("attachment_url", 500).nullable()
    val isRead = bool("is_read").default(false)
    val readAt = timestamp("read_at").nullable()
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
}

// EilajiPlusSync table for tracking sync status
object EilajiPlusSync : IdTable<Long>("eilaji_plus_sync") {
    override val id: EntityID<Long> = long("id").autoIncrement().entityId()
    val prescriptionId = reference("prescription_id", Prescriptions)
    val status = varchar("status", 50) // PENDING, SENT, CONFIRMED, FAILED
    val retryCount = integer("retry_count").default(0)
    val lastAttempt = timestamp("last_attempt").nullable()
    val errorMessage = text("error_message").nullable()
    val eilajiPlusRef = varchar("eilaji_plus_ref", 255).nullable()
    val createdAt = timestamp("created_at").clientDefault { java.time.Instant.now() }
}
