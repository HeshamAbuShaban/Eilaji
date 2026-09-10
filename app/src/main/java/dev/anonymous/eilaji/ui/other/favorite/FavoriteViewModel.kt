package dev.anonymous.eilaji.ui.other.favorite

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.databinding.FragmentFavoritesBinding
import dev.anonymous.eilaji.favorite_system.database.db.FavoriteDatabase
import dev.anonymous.eilaji.favorite_system.database.entity.FavoriteEntity
import dev.anonymous.eilaji.favorite_system.repository.FavoriteSyncRepository
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.utils.UtilsScreen
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.concurrent.atomic.AtomicInteger

class FavoriteViewModel : ViewModel() {
    private lateinit var _binding: FragmentFavoritesBinding
    private val binding: FragmentFavoritesBinding get() = _binding
    private lateinit var repo: FavoriteSyncRepository
    private var adapter: MedicinesAdapter? = null
    fun setBindingObj(binding: FragmentFavoritesBinding) { this._binding = binding }
    fun initRepo(context: Context) { repo = FavoriteSyncRepository(context) }

    fun favoritesLive(context: Context): LiveData<List<FavoriteEntity>> {
        return FavoriteDatabase.getDatabase(context).favoriteDao().getAllLive()
    }

    fun syncOnStart() { try { repo.syncFetch(); repo.syncPending() } catch (_: Exception) {} }

    fun refreshFromNetwork() { repo.syncFetch() }

    fun removeFavorite(id: String) { repo.syncDelete(id) }

    fun addFavorite(medId: String?, pharmId: String?) { repo.syncCreateLocalFirst(medId, pharmId) }

    fun setupFavoritesRecycler(context: Activity) {
        val halfScreenWidth: Int = UtilsScreen.screenWidth / 2
        adapter = MedicinesAdapter(arrayListOf(), true, halfScreenWidth) { med -> toggleFavorite(med) }
        with(binding.recyclerFavorites) {
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@FavoriteViewModel.adapter
        }
        binding.btnBrowseCategories.setOnClickListener {
            try {
                val act = context
                act.finish()
                val intent = android.content.Intent(act, dev.anonymous.eilaji.ui.base.BaseActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                act.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun updateList(entities: List<FavoriteEntity>) {
        val ctx = try { binding.root.context } catch (_: Exception) { null }
        if (entities.isEmpty()) {
            adapter?.updateList(emptyList())
            binding.progressFavorites.visibility = View.GONE
            binding.recyclerFavorites.visibility = View.GONE
            binding.emptyFavoritesView.visibility = View.VISIBLE
            return
        }
        val medEntities = entities.filter { it.medicineId != null }
        if (medEntities.isEmpty()) {
            adapter?.updateList(emptyList())
            binding.progressFavorites.visibility = View.GONE
            binding.recyclerFavorites.visibility = View.GONE
            binding.emptyFavoritesView.visibility = View.VISIBLE
            return
        }
        binding.emptyFavoritesView.visibility = View.GONE
        binding.recyclerFavorites.visibility = View.VISIBLE
        binding.progressFavorites.visibility = View.VISIBLE
        if (ctx == null) {
            val medicines = medEntities.map { e -> Medicine(e.medicineId!!, "", e.medicineTitleEn ?: e.medicineTitleAr ?: "", 0.0, "", ArrayList(), "", "", true) }
            applyMedicines(medicines)
            return
        }
        val api = NetworkModule.provideApiService(ctx)
        val results = mutableListOf<Medicine>()
        val remaining = AtomicInteger(medEntities.size)
        val titleMap = medEntities.associate { it.medicineId!! to (it.medicineTitleEn ?: it.medicineTitleAr ?: "") }
        for (entity in medEntities) {
            val medId = entity.medicineId!!
            api.getMedicine(medId).enqueue(object : Callback<ApiResponse<MedicineDto>> {
                override fun onResponse(call: Call<ApiResponse<MedicineDto>>, response: Response<ApiResponse<MedicineDto>>) {
                    val dto = if (response.isSuccessful && response.body()?.success == true) response.body()?.data else null
                    val med = if (dto != null) dto.toMedicine() else Medicine(medId, "", titleMap[medId] ?: "", 0.0, "", ArrayList(), "", "", true)
                    synchronized(results) { results.add(med) }
                    if (remaining.decrementAndGet() == 0) applyMedicines(results)
                }
                override fun onFailure(call: Call<ApiResponse<MedicineDto>>, t: Throwable) {
                    val med = Medicine(medId, "", titleMap[medId] ?: "", 0.0, "", ArrayList(), "", "", true)
                    synchronized(results) { results.add(med) }
                    if (remaining.decrementAndGet() == 0) applyMedicines(results)
                }
            })
        }
    }

    private fun applyMedicines(medicines: List<Medicine>) {
        try { binding.root.post { doApply(medicines) } } catch (_: Exception) { doApply(medicines) }
    }

    private fun doApply(medicines: List<Medicine>) {
        adapter?.updateList(medicines)
        binding.progressFavorites.visibility = View.GONE
        if (medicines.isEmpty()) {
            binding.recyclerFavorites.visibility = View.GONE
            binding.emptyFavoritesView.visibility = View.VISIBLE
        } else {
            binding.recyclerFavorites.visibility = View.VISIBLE
            binding.emptyFavoritesView.visibility = View.GONE
        }
    }

    private fun MedicineDto.toMedicine(): Medicine {
        val title = titleEn.ifBlank { titleAr }
        val details = descriptionEn ?: descriptionAr ?: ""
        return Medicine(id, imageUrl ?: "", title, price ?: 0.0, details, ArrayList(), "", "", true)
    }

    private fun toggleFavorite(med: Medicine) { repo.syncDelete(med.id) }

    fun setToolBarTitle(context: Context) {
        binding.includeAppBarLayoutAlternatives.toolbarApp.title = context.getString(R.string.favorite)
    }
}
