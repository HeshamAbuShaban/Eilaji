package dev.anonymous.eilaji.favorite_system.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.anonymous.eilaji.favorite_system.repository.FavoriteSyncRepository

class FavoriteSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val repo = FavoriteSyncRepository(applicationContext)
            repo.syncPending()
            repo.syncFetch()
            Result.success()
        } catch (_: Exception) { Result.retry() }
    }
}
