package com.eilaji.backend.dto

import kotlinx.serialization.Serializable
import java.time.Instant
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
    val id: UUID,
    val email: String,
    val fullName: String,
    val phone: String?,
    val avatarUrl: String?,
    val role: String,
    val isVerified: Boolean,
    val createdAt: Instant
)

// ===== Medicine DTOs =====

@Serializable
data class MedicineDto(
    val id: UUID,
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
    val id: UUID,
    val nameAr: String,
    val nameEn: String,
    val iconUrl: String?,
    val subcategories: List<SubcategoryDto> = emptyList()
)

@Serializable
data class SubcategoryDto(
    val id: UUID,
    val nameAr: String,
    val nameEn: String,
    val iconUrl: String?
)

// ===== Pharmacy DTOs =====

@Serializable
data class PharmacyDto(
    val id: UUID,
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
    val id: UUID,
    val imageUrl: String,
    val notes: String?,
    val status: String,
    val selectedPharmacyId: UUID?,
    val pharmacyName: String? = null,
    val quotedPrice: Double?,
    val pharmacistNotes: String?,
    val sentToEilajiPlus: Boolean,
    val createdAt: Instant
)

@Serializable
data class CreatePrescriptionRequest(
    val notes: String? = null,
    val selectedPharmacyId: UUID? = null
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
    val id: UUID,
    val otherUserId: UUID,
    val otherUserName: String,
    val otherUserAvatar: String?,
    val lastMessageText: String?,
    val lastMessageImageUrl: String?,
    val lastMessageAt: Instant?,
    val unreadCount: Int
)

@Serializable
data class MessageDto(
    val id: UUID,
    val senderId: UUID,
    val messageText: String?,
    val messageImageUrl: String?,
    val isRead: Boolean,
    val createdAt: Instant
)

@Serializable
data class SendMessageRequest(
    val chatId: UUID,
    val messageText: String? = null,
    val messageImageUrl: String? = null
)

// ===== Favorites DTOs =====

@Serializable
data class FavoriteDto(
    val id: UUID,
    val type: String, // MEDICINE or PHARMACY
    val medicineId: UUID? = null,
    val medicineTitleAr: String? = null,
    val medicineTitleEn: String? = null,
    val pharmacyId: UUID? = null,
    val pharmacyName: String? = null,
    val createdAt: Instant
)

// ===== Reminder DTOs =====

@Serializable
data class MedicationReminderDto(
    val id: UUID,
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
    val id: UUID,
    val userId: UUID,
    val userName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: Instant
)

@Serializable
data class CreateRatingRequest(
    val pharmacyId: UUID,
    val rating: Int,
    val comment: String? = null
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
