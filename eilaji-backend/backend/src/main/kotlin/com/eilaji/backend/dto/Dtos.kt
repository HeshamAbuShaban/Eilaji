package com.eilaji.backend.dto

import kotlinx.serialization.Serializable

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
    val user: UserDto?
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val fullName: String,
    val phone: String?,
    val avatarUrl: String?,
    val role: String,
    val isVerified: Boolean,
    val createdAt: String
)

@Serializable
data class MedicineDto(
    val id: String,
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
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val iconUrl: String?,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val subcategories: List<SubcategoryDto> = emptyList()
)

@Serializable
data class SubcategoryDto(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val iconUrl: String?
)

@Serializable
data class PharmacyDto(
    val id: String,
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

@Serializable
data class PrescriptionDto(
    val id: String,
    val userId: String,
    val pharmacyId: String?,
    val pharmacyName: String? = null,
    val imageUrl: String,
    val notes: String?,
    val status: String,
    val quotedPrice: Double?,
    val pharmacistNotes: String?,
    val eilajiPlusRef: String?,
    val eilajiPlusStatus: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreatePrescriptionRequest(
    val notes: String? = null,
    val pharmacyId: String? = null
)

@Serializable
data class UpdatePrescriptionStatusRequest(
    val status: String,
    val quotedPrice: Double? = null,
    val pharmacistNotes: String? = null
)

@Serializable
data class ChatDto(
    val id: String,
    val prescriptionId: String? = null,
    val pharmacyId: String? = null,
    val pharmacyName: String? = null,
    val userId: String,
    val userName: String? = null,
    val lastMessage: String?,
    val lastMessageAt: String?,
    val unreadCount: Int = 0,
    val createdAt: String
)

@Serializable
data class MessageDto(
    val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String? = null,
    val content: String?,
    val messageType: String? = null,
    val attachmentUrl: String? = null,
    val isRead: Boolean = false,
    val readAt: String? = null,
    val createdAt: String
)

@Serializable
data class SendMessageRequest(
    val chatId: String,
    val messageText: String? = null,
    val messageImageUrl: String? = null
)

@Serializable
data class OrderDto(
    val id: String,
    val prescriptionId: String,
    val patientId: String,
    val pharmacyId: String,
    val pharmacyName: String? = null,
    val status: String,
    val totalAmount: Double,
    val paymentMethod: String?,
    val paymentStatus: String,
    val deliveryAddress: String?,
    val deliveryNotes: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class FavoriteDto(
    val id: String,
    val type: String,
    val medicineId: String? = null,
    val medicineTitleAr: String? = null,
    val medicineTitleEn: String? = null,
    val pharmacyId: String? = null,
    val pharmacyName: String? = null,
    val createdAt: String
)

@Serializable
data class MedicationReminderDto(
    val id: String,
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

@Serializable
data class CreateFavoriteRequest(
    val medicineId: String? = null,
    val pharmacyId: String? = null
)

@Serializable
data class RatingDto(
    val id: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String
)

@Serializable
data class CreateRatingRequest(
    val pharmacyId: String,
    val rating: Int,
    val comment: String? = null
)

@Serializable
data class WebSocketMessage(
    val type: String,
    val chatId: String? = null,
    val userId: String? = null,
    val message: MessageDto? = null,
    val timestamp: String? = null,
    val isOnline: Boolean? = null,
    val content: String? = null
)

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

@Serializable
data class PaginatedResult<T>(
    val items: List<T>,
    val total: Long,
    val page: Int = 0,
    val pageSize: Int = 20,
    val totalPages: Int = 0
)

@Serializable
data class UpdateUserRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

@Serializable
data class CreateMedicineRequest(
    val titleEn: String,
    val titleAr: String,
    val descriptionEn: String? = null,
    val descriptionAr: String? = null,
    val imageUrl: String? = null,
    val price: Double? = null,
    val manufacturer: String? = null,
    val requiresPrescription: Boolean = false,
    val isActive: Boolean = true,
    val subcategoryId: String? = null
)

@Serializable
data class UpdateMedicineRequest(
    val titleEn: String? = null,
    val titleAr: String? = null,
    val descriptionEn: String? = null,
    val descriptionAr: String? = null,
    val imageUrl: String? = null,
    val price: Double? = null,
    val manufacturer: String? = null,
    val requiresPrescription: Boolean? = null,
    val isActive: Boolean? = null,
    val subcategoryId: String? = null
)

@Serializable
data class CreateCategoryRequest(
    val nameEn: String,
    val nameAr: String,
    val iconUrl: String? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

@Serializable
data class UpdateCategoryRequest(
    val nameEn: String? = null,
    val nameAr: String? = null,
    val iconUrl: String? = null,
    val displayOrder: Int? = null,
    val isActive: Boolean? = null
)
