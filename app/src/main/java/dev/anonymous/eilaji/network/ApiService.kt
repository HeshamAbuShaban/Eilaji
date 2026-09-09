package dev.anonymous.eilaji.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/logout")
    fun logout(): Call<ApiResponse<Any>>

    @GET("user")
    fun getCurrentUser(): Call<ApiResponse<UserDto>>

    @PUT("user")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<ApiResponse<UserDto>>

    @PUT("users/change-password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ApiResponse<Any>>

    @GET("medicines")
    fun getMedicines(
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20,
        @Query("subcategoryId") subcategoryId: String? = null
    ): Call<ApiResponse<PaginatedResult<MedicineDto>>>

    @GET("medicines/{id}")
    fun getMedicine(@Path("id") id: String): Call<ApiResponse<MedicineDto>>

    @GET("medicines/search")
    fun searchMedicines(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20
    ): Call<ApiResponse<PaginatedResult<MedicineDto>>>

    @GET("medicines/categories")
    fun getCategories(): Call<ApiResponse<List<CategoryDto>>>

    @GET("pharmacies")
    fun getPharmacies(
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20,
        @Query("city") city: String? = null,
        @Query("isOpen") isOpen: Boolean? = null,
        @Query("isVerified") isVerified: Boolean? = null
    ): Call<ApiResponse<PaginatedResult<PharmacyDto>>>

    @GET("pharmacies/nearby")
    fun getNearbyPharmacies(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 10.0
    ): Call<ApiResponse<List<PharmacyDto>>>

    @GET("pharmacies/{id}")
    fun getPharmacy(@Path("id") id: String): Call<ApiResponse<PharmacyDto>>

    @Multipart
    @POST("prescriptions")
    fun uploadPrescription(
        @Part image: MultipartBody.Part,
        @Part("notes") notes: RequestBody?,
        @Part("selectedPharmacyId") pharmacyId: RequestBody?
    ): Call<ApiResponse<PrescriptionDto>>

    @GET("prescriptions")
    fun getPrescriptions(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20
    ): Call<ApiResponse<PaginatedResult<PrescriptionDto>>>

    @GET("prescriptions/{id}")
    fun getPrescription(@Path("id") id: String): Call<ApiResponse<PrescriptionDto>>

    @POST("orders")
    fun createOrder(@Body request: CreateOrderRequest): Call<ApiResponse<OrderDto>>

    @GET("orders")
    fun getOrders(): Call<ApiResponse<List<OrderDto>>>

    @GET("orders/{id}")
    fun getOrder(@Path("id") id: String): Call<ApiResponse<OrderDto>>

    @PUT("orders/{id}/status")
    fun updateOrderStatus(
        @Path("id") id: String,
        @Body request: UpdateOrderStatusRequest
    ): Call<ApiResponse<OrderDto>>

    @GET("chats")
    fun getChats(): Call<ApiResponse<PaginatedResult<ChatDto>>>

    @POST("chats")
    fun createChat(@Body request: CreateChatRequest): Call<ApiResponse<ChatDto>>

    @GET("chats/{chatId}/messages")
    fun getMessages(
        @Path("chatId") chatId: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 50
    ): Call<ApiResponse<PaginatedResult<MessageDto>>>

    @POST("chats/{chatId}/read")
    fun markAsRead(
        @Path("chatId") chatId: String,
        @Body request: MarkAsReadRequest
    ): Call<ApiResponse<Map<String, Int>>>

    @GET("presence/online")
    fun getOnlineUsers(): Call<ApiResponse<Map<String, List<String>>>>

    @GET("presence/{userId}")
    fun getPresence(@Path("userId") userId: String): Call<ApiResponse<Map<String, Any>>>

    @GET("reminders")
    fun getReminders(): Call<ApiResponse<List<MedicationReminderDto>>>

    @POST("reminders")
    fun createReminder(@Body request: CreateReminderRequest): Call<ApiResponse<MedicationReminderDto>>

    @PUT("reminders/{id}")
    fun updateReminder(@Path("id") id: String, @Body request: UpdateReminderRequest): Call<ApiResponse<MedicationReminderDto>>

    @DELETE("reminders/{id}")
    fun deleteReminder(@Path("id") id: String): Call<ApiResponse<Any>>

    @GET("reminders/{id}")
    fun getReminder(@Path("id") id: String): Call<ApiResponse<MedicationReminderDto>>
}

data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String = "PATIENT"
)

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String,
    @SerializedName("expiresIn") val expiresIn: Long,
    @SerializedName("user") val user: UserDto
)

data class UpdateProfileRequest(
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("address") val address: String? = null
)

data class ChangePasswordRequest(
    @SerializedName("oldPassword") val oldPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

data class CreateOrderRequest(
    @SerializedName("prescriptionId") val prescriptionId: String,
    @SerializedName("pharmacyId") val pharmacyId: String,
    @SerializedName("totalAmount") val totalAmount: Double? = null,
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("deliveryAddress") val deliveryAddress: String? = null,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateOrderStatusRequest(
    @SerializedName("status") val status: String,
    @SerializedName("paymentStatus") val paymentStatus: String? = null
)

data class CreateChatRequest(
    @SerializedName("prescriptionId") val prescriptionId: String? = null,
    @SerializedName("pharmacyId") val pharmacyId: String? = null
)

data class MarkAsReadRequest(
    @SerializedName("messageIds") val messageIds: List<String>? = null,
    @SerializedName("chatId") val chatId: String? = null
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("role") val role: String,
    @SerializedName("isVerified") val isVerified: Boolean,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("avatarUrl") val imageUrl: String?,
    @SerializedName("createdAt") val createdAt: String?
)

data class MedicineDto(
    @SerializedName("id") val id: String,
    @SerializedName("titleEn") val titleEn: String,
    @SerializedName("titleAr") val titleAr: String,
    @SerializedName("descriptionEn") val descriptionEn: String?,
    @SerializedName("descriptionAr") val descriptionAr: String?,
    @SerializedName("manufacturer") val manufacturer: String?,
    @SerializedName("requiresPrescription") val requiresPrescription: Boolean,
    @SerializedName("price") val price: Double?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("subcategoryNameEn") val subcategoryNameEn: String?,
    @SerializedName("subcategoryNameAr") val subcategoryNameAr: String?
)

data class CategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("nameEn") val nameEn: String,
    @SerializedName("nameAr") val nameAr: String,
    @SerializedName("iconUrl") val iconUrl: String?,
    @SerializedName("displayOrder") val displayOrder: Int,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("subcategories") val subcategories: List<SubcategoryDto> = emptyList()
)

data class SubcategoryDto(
    @SerializedName("id") val id: String,
    @SerializedName("nameEn") val nameEn: String,
    @SerializedName("nameAr") val nameAr: String,
    @SerializedName("iconUrl") val iconUrl: String?
)

data class PharmacyDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("imageUrl") val imageUrl: String?,
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String?,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("phone") val phone: String?,
    @SerializedName("isVerified") val isVerified: Boolean,
    @SerializedName("isOpen") val isOpen: Boolean,
    @SerializedName("ratingAvg") val ratingAvg: Double = 0.0,
    @SerializedName("totalRatings") val totalRatings: Int = 0,
    @SerializedName("distanceKm") val distanceKm: Double? = null
)

data class PrescriptionDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("pharmacyId") val pharmacyId: String?,
    @SerializedName("pharmacyName") val pharmacyName: String?,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("quotedPrice") val quotedPrice: Double?,
    @SerializedName("pharmacistNotes") val pharmacistNotes: String?,
    @SerializedName("eilajiPlusRef") val eilajiPlusRef: String?,
    @SerializedName("eilajiPlusStatus") val eilajiPlusStatus: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class OrderDto(
    @SerializedName("id") val id: String,
    @SerializedName("prescriptionId") val prescriptionId: String,
    @SerializedName("patientId") val patientId: String,
    @SerializedName("pharmacyId") val pharmacyId: String,
    @SerializedName("pharmacyName") val pharmacyName: String?,
    @SerializedName("status") val status: String,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("paymentMethod") val paymentMethod: String?,
    @SerializedName("paymentStatus") val paymentStatus: String,
    @SerializedName("deliveryAddress") val deliveryAddress: String?,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("updatedAt") val updatedAt: String
)

data class ChatDto(
    @SerializedName("id") val id: String,
    @SerializedName("prescriptionId") val prescriptionId: String?,
    @SerializedName("pharmacyId") val pharmacyId: String?,
    @SerializedName("pharmacyName") val pharmacyName: String?,
    @SerializedName("userId") val userId: String,
    @SerializedName("userName") val userName: String?,
    @SerializedName("lastMessage") val lastMessage: String?,
    @SerializedName("lastMessageAt") val lastMessageAt: String?,
    @SerializedName("unreadCount") val unreadCount: Int = 0,
    @SerializedName("createdAt") val createdAt: String
)

data class MessageDto(
    @SerializedName("id") val id: String,
    @SerializedName("chatId") val chatId: String,
    @SerializedName("senderId") val senderId: String,
    @SerializedName("senderName") val senderName: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("messageType") val messageType: String?,
    @SerializedName("attachmentUrl") val attachmentUrl: String?,
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("readAt") val readAt: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class SearchResultDto(
    @SerializedName("medicines") val medicines: List<MedicineDto>?,
    @SerializedName("pharmacies") val pharmacies: List<PharmacyDto>?
)

data class PaginatedResult<T>(
    @SerializedName("items") val items: List<T>,
    @SerializedName("total") val total: Long,
    @SerializedName("page") val page: Int,
    @SerializedName("pageSize") val pageSize: Int,
    @SerializedName("totalPages") val totalPages: Int
)

data class MedicationReminderDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("medicineName") val medicineName: String,
    @SerializedName("dosage") val dosage: String? = null,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("scheduleTime") val scheduleTime: String,
    @SerializedName("customDays") val customDays: List<String> = emptyList(),
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null
)

data class CreateReminderRequest(
    @SerializedName("medicineName") val medicineName: String,
    @SerializedName("dosage") val dosage: String? = null,
    @SerializedName("frequency") val frequency: String,
    @SerializedName("scheduleTime") val scheduleTime: String,
    @SerializedName("customDays") val customDays: List<String> = emptyList(),
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("isActive") val isActive: Boolean = true,
    @SerializedName("startDate") val startDate: String? = null,
    @SerializedName("endDate") val endDate: String? = null
)

data class UpdateReminderRequest(
    @SerializedName("medicineName") val medicineName: String? = null,
    @SerializedName("dosage") val dosage: String? = null,
    @SerializedName("frequency") val frequency: String? = null,
    @SerializedName("scheduleTime") val scheduleTime: String? = null,
    @SerializedName("customDays") val customDays: List<String>? = null,
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("isActive") val isActive: Boolean? = null,
    @SerializedName("endDate") val endDate: String? = null
)

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null
)
