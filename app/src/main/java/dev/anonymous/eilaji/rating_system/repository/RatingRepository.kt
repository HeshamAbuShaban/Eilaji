package dev.anonymous.eilaji.rating_system.repository

import android.content.Context
import android.content.SharedPreferences
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.CreateRatingRequest
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.storage.AppSharedPreferences
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RatingRepository(private val context: Context) {
    private val api: ApiService = NetworkModule.provideApiService(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("rating_cache", Context.MODE_PRIVATE)

    fun postRating(pharmacyId: String, rating: Int, comment: String?, onResult: (Boolean, Double?) -> Unit = { _, _ -> }) {
        if (rating !in 1..5) { onResult(false, null); return }
        val prefsCache = prefs
        val oldAvg = prefsCache.getFloat("avg_$pharmacyId", 0f).toDouble()
        val oldCount = prefsCache.getInt("count_$pharmacyId", 0)
        val optimisticAvg = if (oldCount == 0) rating.toDouble() else (oldAvg * oldCount + rating) / (oldCount + 1)
        prefsCache.edit().putFloat("optimistic_avg_$pharmacyId", optimisticAvg.toFloat()).apply()
        api.createRating(CreateRatingRequest(pharmacyId, rating, comment)).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.RatingDto>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.RatingDto>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.RatingDto>>) {
                if (r.isSuccessful && r.body()?.success == true) {
                    fetchRatings(pharmacyId) { list ->
                        val avg = list?.map { it.rating.toDouble() }?.average() ?: optimisticAvg
                        val count = list?.size ?: (oldCount + 1)
                        prefsCache.edit().putFloat("avg_$pharmacyId", avg.toFloat()).putInt("count_$pharmacyId", count).remove("optimistic_avg_$pharmacyId").apply()
                        onResult(true, avg)
                    }
                } else {
                    prefsCache.edit().remove("optimistic_avg_$pharmacyId").apply()
                    onResult(false, null)
                }
            }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.RatingDto>>, t: Throwable) {
                prefsCache.edit().remove("optimistic_avg_$pharmacyId").apply()
                onResult(false, null)
            }
        })
    }

    fun fetchRatings(pharmacyId: String, onResult: (List<dev.anonymous.eilaji.network.RatingDto>?) -> Unit = {}) {
        api.getPharmacyRatings(pharmacyId).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.RatingDto>>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.RatingDto>>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.RatingDto>>>) {
                if (r.isSuccessful && r.body()?.success == true) {
                    val list = r.body()?.data
                    if (list != null) {
                        val avg = if (list.isNotEmpty()) list.map { it.rating.toDouble() }.average() else 0.0
                        prefs.edit().putFloat("avg_$pharmacyId", avg.toFloat()).putInt("count_$pharmacyId", list.size).apply()
                    }
                    onResult(list)
                } else onResult(null)
            }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.RatingDto>>>, t: Throwable) { onResult(null) }
        })
    }

    fun getCachedAvg(pharmacyId: String): Double {
        val opt = prefs.getFloat("optimistic_avg_$pharmacyId", -1f)
        if (opt != -1f) return opt.toDouble()
        return prefs.getFloat("avg_$pharmacyId", 0f).toDouble()
    }

    fun getCachedCount(pharmacyId: String): Int = prefs.getInt("count_$pharmacyId", 0)
}
