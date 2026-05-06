package dev.anonymous.eilaji.ui.base.user_interface.categories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.CategoryDto
import dev.anonymous.eilaji.network.NetworkModule
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CategoriesViewModel : ViewModel() {

    private lateinit var apiService: ApiService

    private val _categoryList = MutableLiveData<List<CategoryDto>>()
    val categoryList: LiveData<List<CategoryDto>> get() = _categoryList

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun init(context: Context) {
        apiService = NetworkModule.provideApiService(context)
    }

    fun loadCategories() {
        if (!::apiService.isInitialized) {
            _error.value = "API service not initialized"
            return
        }

        apiService.getCategories().enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.CategoryDto>>> {
            override fun onResponse(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.CategoryDto>>>,
                response: Response<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.CategoryDto>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    _categoryList.value = response.body()?.data ?: emptyList()
                } else {
                    _error.value = response.body()?.error ?: "Failed to load categories"
                }
            }

            override fun onFailure(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.CategoryDto>>>,
                t: Throwable
            ) {
                _error.value = "Network error: ${t.message}"
            }
        })
    }
}
