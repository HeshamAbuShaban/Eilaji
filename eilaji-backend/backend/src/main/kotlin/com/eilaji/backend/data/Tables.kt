package com.eilaji.backend.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp as jdaTimestamp
import java.time.Instant

object Users : Table("users") {
    val id = varchar("id", 255)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 255)
    val phone = varchar("phone", 20).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val role = varchar("role", 50) // ADMIN, USER, PHARMACIST
    val isVerified = bool("is_verified").default(false)
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Categories : Table("categories") {
    val id = integer("id").autoIncrement()
    val nameEn = varchar("name_en", 255)
    val nameAr = varchar("name_ar", 255)
    val description = text("description").nullable()
    val parentId = reference("parent_id", id).nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Medicines : Table("medicines") {
    val id = integer("id").autoIncrement()
    val titleEn = varchar("title_en", 255)
    val titleAr = varchar("title_ar", 255)
    val description = text("description").nullable()
    val categoryId = reference("category_id", Categories.id)
    val subcategoryId = reference("subcategory_id", Categories.id).nullable()
    val manufacturer = varchar("manufacturer", 255).nullable()
    val requiresPrescription = bool("requires_prescription").default(false)
    val price = decimal("price", 10, 2).nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        index("medicines_search_idx", false, titleEn, titleAr, description)
    }
}

object Pharmacies : Table("pharmacies") {
    val id = integer("id").autoIncrement()
    val ownerId = reference("owner_id", Users.id)
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
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Prescriptions : Table("prescriptions") {
    val id = integer("id").autoIncrement()
    val userId = reference("user_id", Users.id)
    val pharmacyId = reference("pharmacy_id", Pharmacies.id).nullable()
    val imageUrl = varchar("image_url", 500)
    val notes = text("notes").nullable()
    val status = varchar("status", 50).default("PENDING") // PENDING, SENT_TO_PHARMACY, RECEIVED_QUOTE, ACCEPTED, REJECTED, COMPLETED
    val quotedPrice = decimal("quoted_price", 10, 2).nullable()
    val pharmacistNotes = text("pharmacist_notes").nullable()
    val eilajiPlusRef = varchar("eilaji_plus_ref", 255).nullable()
    val eilajiPlusStatus = varchar("eilaji_plus_status", 50).nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Chats : Table("chats") {
    val id = long("id").autoIncrement()
    val prescriptionId = reference("prescription_id", Prescriptions.id).nullable()
    val pharmacyId = reference("pharmacy_id", Pharmacies.id).nullable()
    val userId = reference("user_id", Users.id)
    val lastMessageAt = jdaTimestamp("last_message_at").nullable()
    val lastMessage = text("last_message").nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Messages : Table("messages") {
    val id = long("id").autoIncrement()
    val chatId = reference("chat_id", Chats.id)
    val senderId = reference("sender_id", Users.id)
    val content = text("content")
    val messageType = varchar("message_type", 50).default("TEXT") // TEXT, IMAGE, FILE
    val attachmentUrl = varchar("attachment_url", 500).nullable()
    val isRead = bool("is_read").default(false)
    val readAt = jdaTimestamp("read_at").nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object EilajiPlusSync : Table("eilaji_plus_sync") {
    val id = long("id").autoIncrement()
    val prescriptionId = reference("prescription_id", Prescriptions.id)
    val status = varchar("status", 50) // PENDING, SENT, CONFIRMED, FAILED
    val retryCount = integer("retry_count").default(0)
    val lastAttempt = jdaTimestamp("last_attempt").nullable()
    val errorMessage = text("error_message").nullable()
    val eilajiPlusRef = varchar("eilaji_plus_ref", 255).nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Orders : Table("orders") {
    val id = integer("id").autoIncrement()
    val prescriptionId = reference("prescription_id", Prescriptions.id)
    val patientId = reference("patient_id", Users.id)
    val pharmacyId = reference("pharmacy_id", Pharmacies.id)
    val status = varchar("status", 50) // PENDING, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    val totalAmount = decimal("total_amount", 12, 2)
    val paymentMethod = varchar("payment_method", 50).nullable()
    val paymentStatus = varchar("payment_status", 50) // PENDING, PAID, FAILED, REFUNDED
    val deliveryAddress = text("delivery_address").nullable()
    val deliveryNotes = text("delivery_notes").nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
