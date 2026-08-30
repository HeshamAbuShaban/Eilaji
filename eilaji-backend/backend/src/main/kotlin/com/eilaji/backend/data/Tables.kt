package com.eilaji.backend.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp as jdaTimestamp
import java.time.Instant
import java.util.UUID

object Users : Table("users") {
    val id = uuid("id").autoGenerate()
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 255)
    val phone = varchar("phone", 20).nullable()
    val avatarUrl = varchar("avatar_url", 500).nullable()
    val fcmToken = varchar("fcm_token", 500).nullable()
    val role = varchar("role", 50) // ADMIN, PHARMACIST, PATIENT, DOCTOR
    val isVerified = bool("is_verified").default(false)
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Categories : Table("categories") {
    val id = uuid("id").autoGenerate()
    val nameEn = varchar("name_en", 255)
    val nameAr = varchar("name_ar", 255)
    val iconUrl = varchar("icon_url", 500).nullable()
    val parentId = reference("parent_id", id).nullable()
    val displayOrder = integer("display_order").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Subcategories : Table("subcategories") {
    val id = uuid("id").autoGenerate()
    val categoryId = reference("category_id", Categories.id)
    val nameEn = varchar("name_en", 255)
    val nameAr = varchar("name_ar", 255)
    val iconUrl = varchar("icon_url", 500).nullable()
    val displayOrder = integer("display_order").default(0)
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Medicines : Table("medicines") {
    val id = uuid("id").autoGenerate()
    val titleEn = varchar("title_en", 255)
    val titleAr = varchar("title_ar", 255)
    val descriptionEn = text("description_en").nullable()
    val descriptionAr = text("description_ar").nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val price = decimal("price", 10, 2).nullable()
    val subcategoryId = reference("subcategory_id", Subcategories.id).nullable()
    val manufacturer = varchar("manufacturer", 255).nullable()
    val requiresPrescription = bool("requires_prescription").default(false)
    val alternativesIds = text("alternatives_ids").default("[]")
    val isActive = bool("is_active").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        index("medicines_search_idx", false, titleEn, titleAr, descriptionEn, descriptionAr)
    }
}

object Pharmacies : Table("pharmacies") {
    val id = uuid("id").autoGenerate()
    val ownerUserId = reference("owner_user_id", Users.id)
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val imageUrl = varchar("image_url", 500).nullable()
    val address = text("address")
    val city = varchar("city", 100)
    val latitude = double("latitude")
    val longitude = double("longitude")
    val phone = varchar("phone", 20)
    val licenseNumber = varchar("license_number", 100).nullable()
    val isOpen = bool("is_open").default(true)
    val openingHours = varchar("opening_hours", 255).nullable()
    val ratingAvg = decimal("rating_avg", 3, 2).default(0.0.toBigDecimal())
    val totalRatings = integer("total_ratings").default(0)
    val isVerified = bool("is_verified").default(false)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object PharmacyMedicines : Table("pharmacy_medicines") {
    val id = uuid("id").autoGenerate()
    val pharmacyId = reference("pharmacy_id", Pharmacies.id)
    val medicineId = reference("medicine_id", Medicines.id)
    val price = decimal("price", 10, 2).nullable()
    val stockQuantity = integer("stock_quantity").default(0)
    val isAvailable = bool("is_available").default(true)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("ux_pharmacy_medicine", pharmacyId, medicineId)
    }
}

object Prescriptions : Table("prescriptions") {
    val id = uuid("id").autoGenerate()
    val patientUserId = reference("patient_user_id", Users.id)
    val imageUrl = varchar("image_url", 500)
    val notes = text("notes").nullable()
    val status = varchar("status", 50).default("PENDING") // PENDING, SENT_TO_PHARMACY, RECEIVED_QUOTE, ACCEPTED, REJECTED, COMPLETED, CANCELLED
    val selectedPharmacyId = reference("selected_pharmacy_id", Pharmacies.id).nullable()
    val quotedPrice = decimal("quoted_price", 10, 2).nullable()
    val pharmacistNotes = text("pharmacist_notes").nullable()
    val sentToEilajiDoctor = bool("sent_to_eilaji_doctor").default(false)
    val eilajiDoctorPrescriptionId = uuid("eilaji_doctor_prescription_id").nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Chats : Table("chats") {
    val id = uuid("id").autoGenerate()
    val patientUserId = reference("patient_user_id", Users.id)
    val pharmacyUserId = reference("pharmacy_user_id", Users.id)
    val prescriptionId = reference("prescription_id", Prescriptions.id).nullable()
    val lastMessageText = text("last_message_text").nullable()
    val lastMessageImageUrl = varchar("last_message_image_url", 500).nullable()
    val lastMessageSenderId = reference("last_message_sender_id", Users.id).nullable()
    val lastMessageAt = jdaTimestamp("last_message_at").nullable()
    val unreadCountPatient = integer("unread_count_patient").default(0)
    val unreadCountPharmacy = integer("unread_count_pharmacy").default(0)
    val isArchived = bool("is_archived").default(false)
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("ux_chat_participants_prescription", patientUserId, pharmacyUserId, prescriptionId)
    }
}

object Messages : Table("messages") {
    val id = uuid("id").autoGenerate()
    val chatId = reference("chat_id", Chats.id)
    val senderUserId = reference("sender_user_id", Users.id)
    val messageText = text("message_text").nullable()
    val messageImageUrl = varchar("message_image_url", 500).nullable()
    val medicineName = varchar("medicine_name", 255).nullable()
    val messageType = varchar("message_type", 50).default("TEXT") // TEXT, IMAGE, MEDICINE_INFO
    val isRead = bool("is_read").default(false)
    val readAt = jdaTimestamp("read_at").nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object Favorites : Table("favorites") {
    val id = uuid("id").autoGenerate()
    val userId = reference("user_id", Users.id)
    val medicineId = reference("medicine_id", Medicines.id).nullable()
    val pharmacyId = reference("pharmacy_id", Pharmacies.id).nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("ux_user_medicine_pharmacy", userId, medicineId, pharmacyId)
    }
}

object Ratings : Table("ratings") {
    val id = uuid("id").autoGenerate()
    val userId = reference("user_id", Users.id)
    val pharmacyId = reference("pharmacy_id", Pharmacies.id)
    val rating = integer("rating")
    val comment = text("comment").nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("ux_user_pharmacy_rating", userId, pharmacyId)
    }
}

object MedicationReminders : Table("medication_reminders") {
    val id = uuid("id").autoGenerate()
    val userId = reference("user_id", Users.id)
    val medicineName = varchar("medicine_name", 255)
    val dosage = varchar("dosage", 100).nullable()
    val frequency = varchar("frequency", 50) // DAILY, WEEKLY, CUSTOM
    val scheduleTime = varchar("schedule_time", 8) // HH:mm:ss format
    val customDays = text("custom_days").default("[]")
    val notes = text("notes").nullable()
    val isActive = bool("is_active").default(true)
    val startDate = jdaTimestamp("start_date").clientDefault { Instant.now() }
    val endDate = jdaTimestamp("end_date").nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}

object EilajiPlusSync : Table("eilaji_plus_sync") {
    val id = uuid("id").autoGenerate()
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
    val id = uuid("id").autoGenerate()
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

object AuditLogs : Table("audit_logs") {
    val id = uuid("id").autoGenerate()
    val eventType = varchar("event_type", 50)
    val userId = varchar("user_id", 36).nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = text("user_agent").nullable()
    val resourceType = varchar("resource_type", 50).nullable()
    val resourceId = varchar("resource_id", 36).nullable()
    val description = text("description").nullable()
    val metadata = text("metadata").default("")
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(id)
}
