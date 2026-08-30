package com.eilaji.backend.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant
import java.util.UUID

@Serializable
data class EilajiPlusPrescriptionRequest(
    val prescriptionId: UUID,
    val userId: String,
    val imageUrl: String,
    val notes: String?,
    val patientName: String,
    val patientPhone: String?
)

@Serializable
data class EilajiPlusPrescriptionResponse(
    val eilajiPlusRef: String,
    val status: String,
    val quotedPrice: Double?,
    val message: String?
)

@Serializable
data class EilajiPlusWebhookRequest(
    val eilajiPlusRef: String,
    val status: String,
    val quotedPrice: Double?,
    val message: String?
)

@Serializable
data class CreateChatRequest(
    val prescriptionId: UUID? = null,
    val pharmacyId: UUID? = null
)

@Serializable
data class MarkAsReadRequest(
    val lastReadMessageId: UUID? = null
)
