package com.eilaji.backend.dto

import kotlinx.serialization.Serializable
import java.time.Instant
import kotlinx.serialization.Contextual
import java.util.UUID

// ===== Auth DTOs =====

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val phone: String? = null,
    val role: String = "PATIENT"
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserDto
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class UserDto(
    @Contextual val id: String,  // Users.id is varchar("id", 255)
    val email: String,
    val fullName: String,
    val phone: String?,
    val avatarUrl: String?,
    val role: String,
    val isVerified: Boolean,
    val createdAt: @Contextual Instant
)

// ===== Medicine DTOs =====

@Serializable
data class MedicineDto(
    val id: Int,  // Medicines.id is integer("id").autoIncrement()
    val titleAr: String,
    val titleEn: String,
    val descriptionAr: String?,
    val descriptionEn: String?,
    val imageUrl: String?,
    val price: Double?,
    val manufacturer: String?,
    val requiresPrescription: Boolean,
    val isActive: Boolean,
    val subcategoryNameAr: String? = null,
    val subcategoryNameEn: String? = null
)

@Serializable
data class CategoryDto(
    @Contextual val id: Int,  // Categories.id is integer("id").autoIncrement()
    val nameAr: String,
    val nameEn: String,
    val iconUrl: String?,
    val subcategories: List<SubcategoryDto> = emptyList()
)

@Serializable
data class SubcategoryDto(
    @Contextual val id: Int,  // Categories.id (self-reference)
    val nameAr: String,
    val nameEn: String,
    val iconUrl: String?
)

// ===== Pharmacy DTOs =====

@Serializable
data class PharmacyDto(
    val id: Int,  // Pharmacies.id is integer("id").autoIncrement()
    val name: String,
    val description: String?,
    val imageUrl: String?,
    val address: String,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val isOpen: Boolean,
    val ratingAvg: Double,
    val totalRatings: Int,
    val isVerified: Boolean,
    val distanceKm: Double? = null
)

@Serializable
data class CreatePharmacyRequest(
    val name: String,
    val description: String?,
    val address: String,
    val city: String?,
    val latitude: Double,
    val longitude: Double,
    val phone: String,
    val licenseNumber: String?
)

// ===== Prescription DTOs =====

@Serializable
data class PrescriptionDto(
    val id: Int,  // Prescriptions.id is integer("id").autoIncrement()
    val userId: String,  // Users.id is varchar
    val pharmacyId: Int?,  // Pharmacies.id is integer
    val pharmacyName: String? = null,
    val imageUrl: String,
    val notes: String?,
    val status: String,
    val quotedPrice: Double?,
    val pharmacistNotes: String?,
    val eilajiPlusRef: String?,
    val eilajiPlusStatus: String?,
    val createdAt: @Contextual Instant,
    val updatedAt: @Contextual Instant
)

@Serializable
data class CreatePrescriptionRequest(
    val notes: String? = null,
    val pharmacyId: Int? = null
)

@Serializable
data class UpdatePrescriptionStatusRequest(
    val status: String,
    val quotedPrice: Double? = null,
    val pharmacistNotes: String? = null
)

// ===== Chat DTOs =====

@Serializable
data class ChatDto(
    val id: Long,  // Chats.id is long("id").autoIncrement()
    val prescriptionId: Int? = null,  // Prescriptions.id is integer
    val pharmacyId: Int? = null,  // Pharmacies.id is integer
    val userId: String,  // Users.id is varchar
    val pharmacyName: String? = null,
    val lastMessage: String?,
    val lastMessageAt: @Contextual Instant?,
    val createdAt: @Contextual Instant
)

// ===== Message DTOs =====

@Serializable
data class MessageDto(
    val id: Long,  // Messages.id is long("id").autoIncrement()
    val chatId: Long,  // Chats.id is long
    val senderId: String,  // Users.id is varchar
    val senderName: String? = null,
    val content: String?,
    val messageType: String? = null,
    val attachmentUrl: String? = null,
    val isRead: Boolean = false,
    val readAt: @Contextual Instant? = null,
    val createdAt: @Contextual Instant
)

@Serializable
data class SendMessageRequest(
    val chatId: Long,
    val messageText: String? = null,
    val messageImageUrl: String? = null
)

// ===== Order DTOs =====

@Serializable
data class OrderDto(
    val id: Int,  // Orders.id is integer("id").autoIncrement()
    val prescriptionId: Int,  // Prescriptions.id is integer
    val patientId: String,  // Users.id is varchar
    val pharmacyId: Int,  // Pharmacies.id is integer
    val pharmacyName: String? = null,
    val status: String,
    val totalAmount: Double,
    val paymentMethod: String?,
    val paymentStatus: String,
    val deliveryAddress: String?,
    val deliveryNotes: String?,
    val createdAt: @Contextual Instant,
    val updatedAt: @Contextual Instant
)

// ===== Favorites DTOs =====

@Serializable
data class FavoriteDto(
    @Contextual val id: UUID,
    val type: String, // MEDICINE or PHARMACY
    val medicineId: @Contextual UUID? = null,
    val medicineTitleAr: String? = null,
    val medicineTitleEn: String? = null,
    val pharmacyId: @Contextual UUID? = null,
    val pharmacyName: String? = null,
    val createdAt: @Contextual Instant
)

// ===== Reminder DTOs =====

@Serializable
data class MedicationReminderDto(
    @Contextual val id: UUID,
    val medicineName: String,
    val dosage: String?,
    val frequency: String,
    val scheduleTime: String,
    val customDays: List<Int>?,
    val notes: String?,
    val isActive: Boolean,
    val startDate: String,
    val endDate: String?
)

@Serializable
data class CreateReminderRequest(
    val medicineName: String,
    val dosage: String? = null,
    val frequency: String,
    val scheduleTime: String,
    val customDays: List<Int>? = null,
    val notes: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

// ===== Rating DTOs =====

@Serializable
data class RatingDto(
    @Contextual val id: UUID,
    val userId: @Contextual UUID,
    val userName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: @Contextual Instant
)

@Serializable
data class CreateRatingRequest(
    val pharmacyId: @Contextual UUID,
    val rating: Int,
    val comment: String? = null
)

// ===== WebSocket DTOs =====

@Serializable
data class WebSocketMessage(
    val type: String,
    val chatId: Long? = null,
    val userId: String? = null,
    val message: MessageDto? = null,
    val timestamp: @Contextual Instant? = null,
    val isOnline: Boolean? = null,
    val content: String? = null
)

// ===== Generic Response =====

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val message: String? = null
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> {
            return ApiResponse(success = true, data = data, message = message)
        }

        fun <T> error(error: String, message: String? = null): ApiResponse<T> {
            return ApiResponse(success = false, error = error, message = message)
        }
    }
}

// ===== Paginated Result =====

@Serializable
data class PaginatedResult<T>(
    val items: List<T>,
    val total: Long,
    val page: Int = 0,
    val pageSize: Int = 20,
    val totalPages: Int = 0
)
