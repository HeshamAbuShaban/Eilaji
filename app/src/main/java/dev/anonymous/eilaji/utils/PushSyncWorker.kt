package dev.anonymous.eilaji.utils

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PendingNotificationDto
import dev.anonymous.eilaji.network.websocket.PushPayload
import dev.anonymous.eilaji.storage.AppSharedPreferences
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Offline fallback for push. When the foreground socket is closed (battery saver, Doze, or the
 * user swiped the app away), this worker asks the backend for everything that was parked and
 * posts it as normal notifications. Runs at most every 15 minutes, which is the floor Android
 * allows for periodic work.
 */
class PushSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (AppSharedPreferences.getInstance(context).getToken().isNullOrBlank()) return Result.success()

        return try {
            val call = NetworkModule.provideApiService(context).getPendingNotifications()
            val response = call.execute()
            val items = response.body()?.data ?: emptyList()
            items.forEach { dto ->
                PushNotifications.show(context, PushPayload(dto.id, dto.title, dto.body, dto.route))
            }
            Result.success()
        } catch (e: Exception) {
            // Retry later; nothing is lost because the backend keeps the queue until we drain it.
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "eilaji_push_sync"

        fun schedule(context: Context) {
            try {
                val request = PeriodicWorkRequest.Builder(PushSyncWorker::class.java, 15, java.util.concurrent.TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                    .build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            } catch (_: Exception) {
            }
        }

        fun cancel(context: Context) {
            try {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            } catch (_: Exception) {
            }
        }
    }
}