package dev.anonymous.eilaji.ui.main.guard.login

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.favorite_system.repository.FavoriteSyncRepository
import dev.anonymous.eilaji.network.LoginRequest
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.storage.AppSharedPreferences
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginViewModel : ViewModel() {
    private lateinit var apiService: dev.anonymous.eilaji.network.ApiService
    private lateinit var sharedPreferences: AppSharedPreferences
    private var appContext: Context? = null
    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> get() = _loginResult

    fun init(context: Context) {
        apiService = NetworkModule.provideApiService(context)
        sharedPreferences = AppSharedPreferences.getInstance(context)
        appContext = context.applicationContext
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginResult.value = LoginResult.Error("Email and password are required")
            return
        }
        val request = LoginRequest(email.trim().lowercase(), password)
        apiService.login(request).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.LoginResponse>> {
            override fun onResponse(call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.LoginResponse>>, response: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.LoginResponse>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val loginResponse = response.body()?.data
                    if (loginResponse != null) {
                        sharedPreferences.putToken(loginResponse.accessToken)
                        sharedPreferences.putFullName(loginResponse.user.fullName)
                        sharedPreferences.putUserId(loginResponse.user.id)
                        sharedPreferences.putRole(loginResponse.user.role)
                        sharedPreferences.putIsVerified(loginResponse.user.isVerified)
                        sharedPreferences.putIsActive(loginResponse.user.isActive)
                        appContext?.let { ctx ->
                            try {
                                val repo = FavoriteSyncRepository(ctx)
                                repo.syncFetch()
                                repo.syncPending()
                            } catch (_: Exception) {}
                        }
                        _loginResult.value = LoginResult.Success(loginResponse.user.fullName, loginResponse.user.imageUrl ?: "")
                    } else {
                        _loginResult.value = LoginResult.Error("Invalid response from server")
                    }
                } else {
                    val errorMessage = response.body()?.error ?: "Login failed. Please check your credentials."
                    _loginResult.value = LoginResult.Error(errorMessage)
                }
            }
            override fun onFailure(call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.LoginResponse>>, t: Throwable) {
                _loginResult.value = LoginResult.Error("Network error: ${t.message}")
            }
        })
    }

    fun logout() { sharedPreferences.clearAll() }
}

sealed class LoginResult {
    data class Success(val fullName: String, val imageUrl: String) : LoginResult()
    data class Error(val errorMessage: String) : LoginResult()
}
