package dev.anonymous.eilaji.network

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // Authentication endpoints
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<ApiResponse<LoginResponse>>

    @POST("auth/logout")
    fun logout(): Call<ApiResponse<Any>>

    @GET("auth/verify")
    fun verifyToken(): Call<ApiResponse<UserDto>>

    // User endpoints
    @GET("users/me")
    fun getCurrentUser(): Call<ApiResponse<UserDto>>

    @PUT("users/me")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<ApiResponse<UserDto>>

    @PUT("users/change-password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ApiResponse<Any>>

    // Medicines endpoints
    @GET("medicines")
    fun getMedicines(
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20,
        @Query("categoryId") categoryId: Int? = null,
        @Query("subcategoryId") subcategoryId: Int? = null,
        @Query("requiresPrescription") requiresPrescription: Boolean? = null
    ): Call<ApiResponse<PaginatedResult<MedicineDto>>>

    @GET("medicines/{id}")
    fun getMedicine(@Path("id") id: Int): Call<ApiResponse<MedicineDto>>

    @GET("medicines/search")
    fun searchMedicines(
        @Query("q") query: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20
    ): Call<ApiResponse<PaginatedResult<MedicineDto>>>

    // Categories endpoints
    @GET("medicines/categories")
    fun getCategories(): Call<ApiResponse<List<CategoryDto>>>

    // Pharmacies endpoints
    @GET("pharmacies")
    fun getPharmacies(
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20,
        @Query("city") city: String? = null,
        @Query("isOpen") isOpen: Boolean? = null,
        @Query("isVerified") isVerified: Boolean? = null
    ): Call<ApiResponse<PaginatedResult<PharmacyDto>>>

    @GET("pharmacies/{id}")
    fun getPharmacy(@Path("id") id: Int): Call<ApiResponse<PharmacyDto>>

    // Prescriptions endpoints
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
    fun getPrescription(@Path("id") id: Int): Call<ApiResponse<PrescriptionDto>>

    // Orders endpoints
    @POST("orders")
    fun createOrder(@Body request: CreateOrderRequest): Call<ApiResponse<OrderDto>>

    @GET("orders")
    fun getOrders(): Call<ApiResponse<List<OrderDto>>>

    @GET("orders/{id}")
    fun getOrder(@Path("id") id: Int): Call<ApiResponse<OrderDto>>

    @PUT("orders/{id}/status")
    fun updateOrderStatus(
        @Path("id") id: Int,
        @Body request: UpdateOrderStatusRequest
    ): Call<ApiResponse<OrderDto>>

    // Chat endpoints
    @GET("chats")
    fun getChats(): Call<ApiResponse<List<ChatDto>>>

    @POST("chats")
    fun createChat(@Body request: CreateChatRequest): Call<ApiResponse<ChatDto>>

    @GET("chats/{chatId}/messages")
    fun getMessages(
        @Path("chatId") chatId: Long,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 50
    ): Call<ApiResponse<PaginatedResult<MessageDto>>>

    @POST("chats/{chatId}/read")
    fun markAsRead(
        @Path("chatId") chatId: Long,
        @Body request: MarkAsReadRequest
    ): Call<ApiResponse<Map<String, Int>>>

    // Search endpoint
    @GET("search")
    fun globalSearch(
        @Query("q") query: String,
        @Query("type") type: String? = null
    ): Call<ApiResponse<SearchResultDto>>
}

// Request/Response data classes
data class RegisterRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("role") val role: String = "USER"
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
    @SerializedName("prescriptionId") val prescriptionId: Int,
    @SerializedName("pharmacyId") val pharmacyId: Int,
    @SerializedName("notes") val notes: String? = null
)

data class UpdateOrderStatusRequest(
    @SerializedName("status") val status: String
)

data class CreateChatRequest(
    @SerializedName("participantId") val participantId: String
)

data class MarkAsReadRequest(
    @SerializedName("messageIds") val messageIds: List<Long>
)

// DTOs (simplified versions - expand as needed)
data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("fullName") val fullName: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("role") val role: String,
    @SerializedName("isVerified") val isVerified: Boolean,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("imageUrl") val imageUrl: String?
)

data class MedicineDto(
    @SerializedName("id") val id: Int,
    @SerializedName("scientificName") val scientificName: String,
    @SerializedName("commercialName") val commercialName: String?,
    @SerializedName("categoryId") val categoryId: Int?,
    @SerializedName("requiresPrescription") val requiresPrescription: Boolean,
    @SerializedName("imageUrl") val imageUrl: String?
)

data class PharmacyDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("address") val address: String,
    @SerializedName("city") val city: String,
    @SerializedName("phone") val phone: String?,
    @SerializedName("isVerified") val isVerified: Boolean,
    @SerializedName("isOpen") val isOpen: Boolean
)

data class PrescriptionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("userId") val userId: String,
    @SerializedName("pharmacyId") val pharmacyId: Int?,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("status") val status: String,
    @SerializedName("notes") val notes: String?,
    @SerializedName("createdAt") val createdAt: String
)

data class OrderDto(
    @SerializedName("id") val id: Int,
    @SerializedName("userId") val userId: String,
    @SerializedName("pharmacyId") val pharmacyId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("totalAmount") val totalAmount: Double,
    @SerializedName("createdAt") val createdAt: String
)

data class ChatDto(
    @SerializedName("id") val id: Long,
    @SerializedName("user1Id") val user1Id: String,
    @SerializedName("user2Id") val user2Id: String,
    @SerializedName("lastMessage") val lastMessage: MessageDto?
)

data class MessageDto(
    @SerializedName("id") val id: Long,
    @SerializedName("chatId") val chatId: Long,
    @SerializedName("senderId") val senderId: String,
    @SerializedName("content") val content: String,
    @SerializedName("type") val type: String,
    @SerializedName("isRead") val isRead: Boolean,
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

// Generic API response
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: String? = null,
    @SerializedName("message") val message: String? = null
)