package dev.anonymous.eilaji.ui.other.checkout

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.CreateOrderRequest
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.OrderDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckoutViewModel : ViewModel() {
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    private val _orderResult = MutableLiveData<Result<OrderDto>>()
    val orderResult: LiveData<Result<OrderDto>> = _orderResult
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun placeOrder(context: Context, prescriptionId: String, pharmacyId: String, totalAmount: Double?, paymentMethod: String, deliveryAddress: String?, notes: String? = null) {
        _loading.value = true
        val req = CreateOrderRequest(
            prescriptionId = prescriptionId,
            pharmacyId = pharmacyId,
            totalAmount = totalAmount,
            paymentMethod = paymentMethod,
            deliveryAddress = deliveryAddress,
            notes = notes
        )
        NetworkModule.provideApiService(context).createOrder(req).enqueue(object : Callback<ApiResponse<OrderDto>> {
            override fun onResponse(call: Call<ApiResponse<OrderDto>>, response: Response<ApiResponse<OrderDto>>) {
                _loading.value = false
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    _orderResult.value = Result.success(response.body()!!.data!!)
                } else {
                    _error.value = response.body()?.error ?: response.body()?.message ?: "Order failed"
                }
            }
            override fun onFailure(call: Call<ApiResponse<OrderDto>>, t: Throwable) {
                _loading.value = false
                _error.value = t.message ?: "Network error"
            }
        })
    }
}
