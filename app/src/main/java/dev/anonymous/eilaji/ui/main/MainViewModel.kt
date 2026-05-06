package dev.anonymous.eilaji.ui.main

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.storage.AppSharedPreferences
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : ViewModel() {

    private val _navController = MutableLiveData<NavController?>()
    val navController: LiveData<NavController?> = _navController

    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: AppSharedPreferences

    private val _userData = MutableLiveData<UserData?>()
    val userData: LiveData<UserData?> = _userData

    fun init(context: Context) {
        apiService = NetworkModule.provideApiService(context)
        sharedPreferences = AppSharedPreferences.Instance(context)
    }

    fun setNavController(navController: NavController) {
        _navController.value = navController
    }

    fun loadUserData() {
        apiService.getCurrentUser().enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.UserDto>> {
            override fun onResponse(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.UserDto>>,
                response: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.UserDto>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val user = response.body()?.data
                    if (user != null) {
                        val userData = UserData(
                            fullName = user.fullName,
                            email = user.email,
                            phone = user.phone ?: "",
                            role = user.role,
                            isVerified = user.isVerified,
                            imageUrl = user.imageUrl ?: ""
                        )
                        _userData.value = userData

                        // Update shared preferences
                        sharedPreferences.putFullName(user.fullName)
                        sharedPreferences.putRole(user.role)
                        sharedPreferences.putIsVerified(user.isVerified)
                    }
                } else {
                    // Token might be invalid, clear it
                    sharedPreferences.clearAll()
                }
            }

            override fun onFailure(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.UserDto>>,
                t: Throwable
            ) {
                // Network error - keep existing data
            }
        })
    }

    fun logout() {
        sharedPreferences.clearAll()
        _userData.value = null
    }
}

data class UserData(
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String,
    val isVerified: Boolean,
    val imageUrl: String
)