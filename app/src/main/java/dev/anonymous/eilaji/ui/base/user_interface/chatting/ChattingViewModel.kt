package dev.anonymous.eilaji.ui.base.user_interface.chatting

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.models.ChatModel
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.storage.AppSharedPreferences
import java.time.Instant

class ChattingViewModel : ViewModel() {
    private var userUid: String? = null
    private var prefs: AppSharedPreferences? = null
    private val _chats = MutableLiveData<List<ChatModel>>()
    val chats: LiveData<List<ChatModel>> = _chats
    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun init(context: Context) {
        prefs = AppSharedPreferences.getInstance(context)
        userUid = prefs?.getUserId()
    }

    fun getUserUid(): String = userUid ?: ""

    fun isLoggedIn(): Boolean = !prefs?.getToken().isNullOrBlank() && !userUid.isNullOrBlank()

    fun loadChats(context: Context) {
        if (prefs == null) init(context)
        if (!isLoggedIn()) { _isEmpty.value = true; _isLoading.value = false; return }
        _isLoading.value = true
        val api = NetworkModule.provideApiService(context)
        api.getChats().enqueue(object : retrofit2.Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.ChatDto>>> {
            override fun onResponse(call: retrofit2.Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.ChatDto>>>, response: retrofit2.Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.ChatDto>>>) {
                _isLoading.value = false
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    val dtos = response.body()!!.data!!.items
                    if (dtos.isEmpty()) { _chats.value = emptyList(); _isEmpty.value = true }
                    else {
                        val mapped = dtos.map { dto ->
                            val ts = parseTime(dto.lastMessageAt ?: dto.createdAt)
                            ChatModel(dto.id, dto.lastMessage, null, dto.userId, dto.pharmacyName ?: dto.userName ?: "Chat", "default", dto.pharmacyId ?: dto.userId, ts)
                        }
                        _chats.value = mapped
                        _isEmpty.value = false
                    }
                } else { _chats.value = emptyList(); _isEmpty.value = true; _error.value = response.body()?.error }
            }
            override fun onFailure(call: retrofit2.Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.ChatDto>>>, t: Throwable) {
                _isLoading.value = false; _isEmpty.value = true; _error.value = t.message; _chats.value = emptyList()
            }
        })
    }

    private fun parseTime(s: String?): Long {
        if (s == null) return System.currentTimeMillis()
        return try { Instant.parse(s).toEpochMilli() } catch (e: Exception) { try { s.toLong() } catch (_: Exception) { System.currentTimeMillis() } }
    }

    @Deprecated("Use isLoggedIn and chats")
    fun getCurrentUser(): MutableLiveData<com.google.firebase.auth.FirebaseUser?> { return MutableLiveData(null) }
}
