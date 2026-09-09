package dev.anonymous.eilaji.reminder_system.repository

import android.content.Context
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.util.ReminderMapper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReminderSyncRepository(private val context: Context) {
    private val api: ApiService = NetworkModule.provideApiService(context)
    fun syncFetch(onResult: (List<Reminder>?) -> Unit) {
        api.getReminders().enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.MedicationReminderDto>>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.MedicationReminderDto>>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.MedicationReminderDto>>>) {
                if (r.isSuccessful && r.body()?.success == true) {
                    val list = r.body()?.data?.map { ReminderMapper.fromDto(it) }
                    onResult(list)
                } else if (r.code() == 404) onResult(null) else onResult(null)
            }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.MedicationReminderDto>>>, t: Throwable) { onResult(null) }
        })
    }
    fun syncCreate(rem: Reminder, cb: (Boolean) -> Unit = {}) {
        api.createReminder(ReminderMapper.toCreate(rem)).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.MedicationReminderDto>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.MedicationReminderDto>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.MedicationReminderDto>>) { cb(r.isSuccessful && r.body()?.success == true) }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.MedicationReminderDto>>, t: Throwable) { cb(false) }
        })
    }
    fun syncUpdate(rem: Reminder, cb: (Boolean) -> Unit = {}) {
        val id = rem.backendId ?: rem.id
        api.updateReminder(id, ReminderMapper.toUpdate(rem)).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.MedicationReminderDto>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.MedicationReminderDto>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.MedicationReminderDto>>) { if (r.code()==404) cb(false) else cb(r.isSuccessful) }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.MedicationReminderDto>>, t: Throwable) { cb(false) }
        })
    }
    fun syncDelete(rem: Reminder, cb: (Boolean) -> Unit = {}) {
        val id = rem.backendId ?: rem.id
        api.deleteReminder(id).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<Any>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<Any>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<Any>>) { if (r.code()==404) cb(false) else cb(r.isSuccessful) }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<Any>>, t: Throwable) { cb(false) }
        })
    }
}
