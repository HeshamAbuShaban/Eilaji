package dev.anonymous.eilaji.favorite_system.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dev.anonymous.eilaji.favorite_system.database.db.FavoriteDatabase
import dev.anonymous.eilaji.favorite_system.database.entity.FavoriteEntity
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.CreateFavoriteRequest
import dev.anonymous.eilaji.network.NetworkModule
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.UUID

class FavoriteSyncRepository(private val context: Context) {
    private val api: ApiService = NetworkModule.provideApiService(context)
    private val db = FavoriteDatabase.getDatabase(context)
    private val dao = db.favoriteDao()

    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun hasToken(): Boolean = try { dev.anonymous.eilaji.storage.AppSharedPreferences.getInstance(context).getToken() != null } catch (_: Exception) { false }

    fun syncFetch(onResult: ((List<FavoriteEntity>?) -> Unit)? = null) {
        if (!hasToken()) { onResult?.invoke(null); return }
        if (!isOnline()) { onResult?.invoke(null); return }
        api.getFavorites().enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.FavoriteDto>>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.FavoriteDto>>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.FavoriteDto>>>) {
                if (r.isSuccessful && r.body()?.success == true) {
                    val dtos = r.body()?.data ?: emptyList()
                    val entities = dtos.map { dto ->
                        FavoriteEntity(
                            id = dto.id,
                            medicineId = dto.medicineId,
                            pharmacyId = dto.pharmacyId,
                            type = dto.type,
                            medicineTitleEn = dto.medicineTitleEn,
                            medicineTitleAr = dto.medicineTitleAr,
                            pharmacyName = dto.pharmacyName,
                            createdAt = dto.createdAt,
                            backendId = dto.id,
                            syncStatus = "SYNCED"
                        )
                    }
                    Thread {
                        dao.clearAll()
                        if (entities.isNotEmpty()) dao.insertAll(entities)
                        onResult?.invoke(entities)
                    }.start()
                } else if (r.code() == 404 || r.code() == 401) {
                    onResult?.invoke(null)
                } else onResult?.invoke(null)
            }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<List<dev.anonymous.eilaji.network.FavoriteDto>>>, t: Throwable) { onResult?.invoke(null) }
        })
    }

    fun syncCreateLocalFirst(medicineId: String?, pharmacyId: String?, onDone: (Boolean) -> Unit = {}) {
        val localId = UUID.randomUUID().toString()
        val entity = FavoriteEntity(
            id = localId,
            medicineId = medicineId,
            pharmacyId = pharmacyId,
            type = when { medicineId != null && pharmacyId != null -> "BOTH"; medicineId != null -> "MEDICINE"; else -> "PHARMACY" },
            medicineTitleEn = null,
            medicineTitleAr = null,
            pharmacyName = null,
            createdAt = System.currentTimeMillis().toString(),
            backendId = null,
            syncStatus = "PENDING"
        )
        Thread { dao.insert(entity) }.start()
        if (!hasToken()) { onDone(false); return }
        if (!isOnline()) { onDone(false); enqueueWorker(); return }
        api.createFavorite(CreateFavoriteRequest(medicineId, pharmacyId)).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.FavoriteDto>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.FavoriteDto>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.FavoriteDto>>) {
                if (r.isSuccessful && r.body()?.success == true && r.body()?.data != null) {
                    val dto = r.body()!!.data!!
                    Thread {
                        dao.deleteById(localId)
                        dao.insert(FavoriteEntity(dto.id, dto.medicineId, dto.pharmacyId, dto.type, dto.medicineTitleEn, dto.medicineTitleAr, dto.pharmacyName, dto.createdAt, dto.id, "SYNCED"))
                    }.start()
                    onDone(true)
                } else if (r.code() == 404) {
                    onDone(false)
                } else {
                    enqueueWorker()
                    onDone(false)
                }
            }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<dev.anonymous.eilaji.network.FavoriteDto>>, t: Throwable) {
                enqueueWorker()
                onDone(false)
            }
        })
    }

    fun syncDelete(favoriteId: String, onDone: (Boolean) -> Unit = {}) {
        Thread { dao.deleteById(favoriteId) }.start()
        if (!hasToken()) { onDone(false); return }
        if (!isOnline()) { enqueueWorker(); onDone(false); return }
        val entity = try { dao.getById(favoriteId) } catch (_: Exception) { null }
        val remoteId = entity?.backendId ?: favoriteId
        api.deleteFavorite(remoteId).enqueue(object : Callback<dev.anonymous.eilaji.network.ApiResponse<Any>> {
            override fun onResponse(c: Call<dev.anonymous.eilaji.network.ApiResponse<Any>>, r: Response<dev.anonymous.eilaji.network.ApiResponse<Any>>) {
                if (r.isSuccessful) onDone(true) else if (r.code() == 404) onDone(false) else { enqueueWorker(); onDone(false) }
            }
            override fun onFailure(c: Call<dev.anonymous.eilaji.network.ApiResponse<Any>>, t: Throwable) { enqueueWorker(); onDone(false) }
        })
    }

    fun syncPending() {
        if (!hasToken() || !isOnline()) return
        Thread {
            val pending = dao.getPendingSync()
            for (e in pending) {
                try {
                    val resp = api.createFavorite(CreateFavoriteRequest(e.medicineId, e.pharmacyId)).execute()
                    if (resp.isSuccessful && resp.body()?.success == true && resp.body()?.data != null) {
                        val dto = resp.body()!!.data!!
                        dao.deleteById(e.id)
                        dao.insert(FavoriteEntity(dto.id, dto.medicineId, dto.pharmacyId, dto.type, dto.medicineTitleEn, dto.medicineTitleAr, dto.pharmacyName, dto.createdAt, dto.id, "SYNCED"))
                    }
                } catch (_: Exception) {}
            }
        }.start()
    }

    private fun enqueueWorker() {
        try {
            val req = androidx.work.OneTimeWorkRequestBuilder<dev.anonymous.eilaji.favorite_system.worker.FavoriteSyncWorker>().build()
            androidx.work.WorkManager.getInstance(context).enqueue(req)
        } catch (_: Exception) {}
    }

    fun isFavoriteLocal(medicineId: String?, pharmacyId: String?): Boolean {
        return try {
            if (medicineId != null && pharmacyId != null) dao.findByMedicineAndPharmacy(medicineId, pharmacyId) != null
            else if (medicineId != null) dao.findByMedicineId(medicineId) != null
            else if (pharmacyId != null) dao.findByPharmacyId(pharmacyId) != null
            else false
        } catch (_: Exception) { false }
    }
}
