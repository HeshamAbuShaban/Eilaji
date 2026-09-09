package dev.anonymous.eilaji.ui.main.guard.signUp

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.RegisterRequest
import dev.anonymous.eilaji.storage.AppSharedPreferences
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignUpViewModel : ViewModel() {

    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: AppSharedPreferences

    private val _signUpResult = MutableLiveData<SignUpResult>()
    val signUpResult: LiveData<SignUpResult> get() = _signUpResult

    fun init(context: Context) {
        apiService = NetworkModule.provideApiService(context)
        sharedPreferences = AppSharedPreferences.getInstance(context)
    }

    fun signUp(fullName: String, email: String, password: String, phone: String? = null) {
        // Validate input
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _signUpResult.value = SignUpResult.Error("Full name, email, and password are required")
            return
        }

        if (password.length < 12) {
            _signUpResult.value = SignUpResult.Error("Password must be at least 12 characters long")
            return
        }

        val request = RegisterRequest(
            email = email.trim().lowercase(),
            password = password,
            fullName = fullName.trim(),
            phone = phone?.trim()
        )

        apiService.register(request).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.LoginResponse>> {
            override fun onResponse(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.LoginResponse>>,
                response: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.LoginResponse>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val loginResponse = response.body()?.data
                    if (loginResponse != null) {
                        // Save tokens and user data
                        sharedPreferences.putToken(loginResponse.accessToken)
                        sharedPreferences.putFullName(loginResponse.user.fullName)
                        sharedPreferences.putUserId(loginResponse.user.id)
                        sharedPreferences.putRole(loginResponse.user.role)
                        sharedPreferences.putIsVerified(loginResponse.user.isVerified)
                        sharedPreferences.putIsActive(loginResponse.user.isActive)

                        _signUpResult.value = SignUpResult.Success(loginResponse.user.fullName)
                    } else {
                        _signUpResult.value = SignUpResult.Error("Invalid response from server")
                    }
                } else {
                    val errorMessage = when {
                        response.body()?.error != null -> response.body()?.error
                        response.code() == 409 -> "Email already registered"
                        response.code() == 400 -> "Invalid input data"
                        else -> "Registration failed. Please try again."
                    }
                    _signUpResult.value = SignUpResult.Error(errorMessage ?: "Registration failed")
                }
            }

            override fun onFailure(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.LoginResponse>>,
                t: Throwable
            ) {
                _signUpResult.value = SignUpResult.Error("Network error: ${t.message}")
            }
        })
    }
}

sealed class SignUpResult {
    data class Success(val fullName: String) : SignUpResult()
    data class Error(val errorMessage: String) : SignUpResult()
}
