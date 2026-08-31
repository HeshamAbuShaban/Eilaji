package com.eilaji.backend.dto

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// UUID serializer
object UuidSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID")
    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

@Serializable
data class EilajiPlusPrescriptionRequest(
    @Contextual(UuidSerializer::class) val prescriptionId: UUID,
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
    @Contextual(UuidSerializer::class) val prescriptionId: UUID? = null,
    @Contextual(UuidSerializer::class) val pharmacyId: UUID? = null
)

@Serializable
data class MarkAsReadRequest(
    @Contextual(UuidSerializer::class) val lastReadMessageId: UUID? = null
)
