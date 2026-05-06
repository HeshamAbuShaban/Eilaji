package dev.anonymous.eilaji.ui.other.medicine

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MedicineViewModel : ViewModel() {

    private lateinit var apiService: ApiService

    private val _medicines = MutableLiveData<List<MedicineDto>>()
    val medicines: LiveData<List<MedicineDto>> get() = _medicines

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    fun init(context: Context) {
        apiService = NetworkModule.provideApiService(context)
    }

    fun loadMedicines(categoryId: Int? = null, subcategoryId: Int? = null) {
        if (!::apiService.isInitialized) {
            _error.value = "API service not initialized"
            return
        }

        val call = if (categoryId != null) {
            apiService.getMedicines(categoryId = categoryId, subcategoryId = subcategoryId)
        } else {
            apiService.getMedicines()
        }

        call.enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.MedicineDto>>> {
            override fun onResponse(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.MedicineDto>>>,
                response: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.MedicineDto>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    _medicines.value = response.body()?.data?.items ?: emptyList()
                } else {
                    _error.value = response.body()?.error ?: "Failed to load medicines"
                }
            }

            override fun onFailure(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.MedicineDto>>>,
                t: Throwable
            ) {
                _error.value = "Network error: ${t.message}"
            }
        })
    }

    fun searchMedicines(query: String) {
        if (!::apiService.isInitialized) return

        apiService.searchMedicines(query).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.MedicineDto>>> {
            override fun onResponse(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.MedicineDto>>>,
                response: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.MedicineDto>>>
            ) {
                if (response.isSuccessful && response.body()?.success == true) {
                    _medicines.value = response.body()?.data?.items ?: emptyList()
                } else {
                    _error.value = response.body()?.error ?: "Search failed"
                }
            }

            override fun onFailure(
                call: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<dev.anonymous.eilaji.network.MedicineDto>>>,
                t: Throwable
            ) {
                _error.value = "Network error: ${t.message}"
            }
        })
    }
}
