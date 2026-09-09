package com.eilaji.backend.dto

import kotlinx.serialization.Serializable

@Serializable
data class EilajiPlusPrescriptionRequest(
    val prescriptionId: String,
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
    val prescriptionId: String? = null,
    val pharmacyId: String? = null
)

@Serializable
data class MarkAsReadRequest(
    val lastReadMessageId: String? = null
)
