package com.eilaji.backend.dto

import kotlinx.serialization.Serializable
import java.time.Instant

// User DTOs
@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String?,
    val role: String,
    val createdAt: Instant
)

@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String? = null,
    val role: String = "USER"
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDto
)

// Category DTOs
@Serializable
data class CategoryDto(
    val id: Int,
    val nameEn: String,
    val nameAr: String,
    val description: String?,
    val parentId: Int?,
    val subcategories: List<CategoryDto> = emptyList(),
    val createdAt: Instant
)

@Serializable
data class CreateCategoryRequest(
    val nameEn: String,
    val nameAr: String,
    val description: String? = null,
    val parentId: Int? = null
)

// Medicine DTOs
@Serializable
data class MedicineDto(
    val id: Int,
    val titleEn: String,
    val titleAr: String,
    val description: String?,
    val categoryId: Int,
    val categoryName: String?,
    val subcategoryId: Int?,
    val subcategoryName: String?,
    val manufacturer: String?,
    val requiresPrescription: Boolean,
    val price: Double?,
    val imageUrl: String?,
    val isActive: Boolean,
    val createdAt: Instant
)

@Serializable
data class CreateMedicineRequest(
    val titleEn: String,
    val titleAr: String,
    val description: String? = null,
    val categoryId: Int,
    val subcategoryId: Int? = null,
    val manufacturer: String? = null,
    val requiresPrescription: Boolean = false,
    val price: Double? = null,
    val imageUrl: String? = null
)

// Pharmacy DTOs
@Serializable
data class PharmacyDto(
    val id: Int,
    val ownerId: String,
    val ownerName: String?,
    val name: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val isVerified: Boolean,
    val isOpen: Boolean,
    val openingHours: String?,
    val licenseNumber: String?,
    val distanceKm: Double? = null,
    val isOnline: Boolean = false,
    val createdAt: Instant
)

@Serializable
data class CreatePharmacyRequest(
    val name: String,
    val address: String,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val openingHours: String? = null,
    val licenseNumber: String? = null
)

@Serializable
data class NearbyPharmaciesRequest(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double = 10.0
)

// Prescription DTOs
@Serializable
data class PrescriptionDto(
    val id: Int,
    val userId: String,
    val pharmacyId: Int?,
    val pharmacyName: String?,
    val imageUrl: String,
    val notes: String?,
    val status: String,
    val quotedPrice: Double?,
    val pharmacistNotes: String?,
    val eilajiPlusRef: String?,
    val eilajiPlusStatus: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

@Serializable
data class CreatePrescriptionRequest(
    val notes: String? = null,
    val selectedPharmacyId: Int? = null
)

@Serializable
data class UpdatePrescriptionStatusRequest(
    val status: String,
    val quotedPrice: Double? = null,
    val pharmacistNotes: String? = null
)

// Chat DTOs
@Serializable
data class ChatDto(
    val id: Long,
    val prescriptionId: Int?,
    val pharmacyId: Int?,
    val pharmacyName: String?,
    val userId: String,
    val userName: String?,
    val lastMessageAt: Instant?,
    val lastMessage: String?,
    val unreadCount: Int = 0,
    val createdAt: Instant
)

@Serializable
data class CreateChatRequest(
    val prescriptionId: Int? = null,
    val pharmacyId: Int? = null,
    val userId: String? = null
)

// Message DTOs
@Serializable
data class MessageDto(
    val id: Long,
    val chatId: Long,
    val senderId: String,
    val senderName: String?,
    val content: String,
    val messageType: String,
    val attachmentUrl: String?,
    val isRead: Boolean,
    val readAt: Instant?,
    val createdAt: Instant
)

@Serializable
data class SendMessageRequest(
    val content: String,
    val messageType: String = "TEXT",
    val attachmentUrl: String? = null
)

@Serializable
data class MarkAsReadRequest(
    val messageIds: List<Long>? = null,
    val chatId: Long? = null
)

// WebSocket messages
@Serializable
data class WebSocketMessage(
    val type: String, // JOIN, LEAVE, MESSAGE, READ, PRESENCE, ERROR
    val chatId: Long? = null,
    val userId: String? = null,
    val content: String? = null,
    val message: MessageDto? = null,
    val isOnline: Boolean? = null,
    val timestamp: Instant = Instant.now()
)

// EilajiPlus DTOs
@Serializable
data class EilajiPlusPrescriptionRequest(
    val prescriptionId: Int,
    val userId: String,
    val imageUrl: String,
    val notes: String?,
    val patientName: String,
    val patientPhone: String?
)

@Serializable
data class EilajiPlusPrescriptionResponse(
    val success: Boolean,
    val eilajiPlusRef: String?,
    val message: String?
)

@Serializable
data class EilajiPlusWebhookRequest(
    val eilajiPlusRef: String,
    val status: String,
    val message: String? = null,
    val quotedPrice: Double? = null,
    val timestamp: Instant = Instant.now()
)

// Paginated response
@Serializable
data class PaginatedResult<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

// Generic response
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)
