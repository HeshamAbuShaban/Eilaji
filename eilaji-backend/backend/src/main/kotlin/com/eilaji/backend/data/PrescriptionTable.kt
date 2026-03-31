package com.eilaji.backend.data

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp as jdaTimestamp
import java.time.Instant

enum class PrescriptionStatus {
    PENDING,
    SENT_TO_PHARMACY,
    RECEIVED_QUOTE,
    ACCEPTED,
    REJECTED,
    COMPLETED,
    CANCELLED
}

object Prescriptions : Table("prescriptions") {
    val id = uuid("id").autoGenerate()
    val patientUserId = reference("patient_user_id", Users.id).onDeleteCascade()
    val imageUrl = varchar("image_url", 500)
    val notes = text("notes").nullable()
    val status = enumerationByName("status", 30, PrescriptionStatus::class).default(PrescriptionStatus.PENDING)
    val selectedPharmacyId = reference("selected_pharmacy_id", Pharmacies.id).onDeleteSetNull().nullable()
    val quotedPrice = decimal("quoted_price", 10, 2).nullable()
    val pharmacistNotes = text("pharmacist_notes").nullable()
    val sentToEilajiPlus = bool("sent_to_eilaji_plus").default(false)
    val eilajiPlusPrescriptionId = uuid("eilaji_plus_prescription_id").nullable()
    val createdAt = jdaTimestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = jdaTimestamp("updated_at").clientDefault { Instant.now() }
    
    override val primaryKey = PrimaryKey(id)
}
