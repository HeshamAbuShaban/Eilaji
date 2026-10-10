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

/**
 * Favorites screen logic.
 *
 * The view binding belongs to the fragment (set with [setBinding] and cleared in onDestroyView);
 * holding it here would keep a destroyed view tree alive across configuration changes.
 */
class FavoriteViewModel : ViewModel() {
    private var binding: FragmentFavoritesBinding? = null
    private lateinit var repo: FavoriteSyncRepository
    private var adapter: MedicinesAdapter? = null

    /** Set from the fragment's onViewCreated, cleared in onDestroyView. */
    fun setBinding(fragmentBinding: FragmentFavoritesBinding?) {
        binding = fragmentBinding
    }

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
        adapter = MedicinesAdapter(arrayListOf(), true, halfScreenWidth,
            onFavClick = { med -> toggleFavorite(med) },
            onItemClick = { med, sharedView ->
                try {
                    val intent = android.content.Intent(context, dev.anonymous.eilaji.ui.other.base.AlternativesActivity::class.java)
                    intent.putExtra("fragmentType", dev.anonymous.eilaji.storage.enums.FragmentsKeys.medicine.name)
                    intent.putExtra("medicineId", med.id)
                    val transitionName = try { sharedView.transitionName } catch (_: Exception) { "medicine_image_${med.id}" }
                    intent.putExtra("sharedTransitionName", transitionName)
                    try {
                        val opts = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                            context, sharedView, transitionName
                        )
                        context.startActivity(intent, opts.toBundle())
                    } catch (_: Exception) { context.startActivity(intent) }
                } catch (_: Exception) {}
            })
        val current = binding ?: return
        with(current.recyclerFavorites) {
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(context, 2)
            adapter = this@FavoriteViewModel.adapter
        }
        current.btnBrowseCategories.setOnClickListener {
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
        val current = binding ?: return
        val ctx = current.root.context
        adapter?.updateList(emptyList())
        current.progressFavorites.visibility = View.GONE
        val medEntities = entities.filter { it.medicineId != null }
        if (medEntities.isEmpty()) {
            current.recyclerFavorites.visibility = View.GONE
            current.emptyFavoritesView.visibility = View.VISIBLE
            return
        }
        current.emptyFavoritesView.visibility = View.GONE
        current.recyclerFavorites.visibility = View.VISIBLE
        current.progressFavorites.visibility = View.VISIBLE

        val api = NetworkModule.provideApiService(ctx)
        val results = mutableListOf<Medicine>()
        val remaining = AtomicInteger(medEntities.size)
        val titleMap = medEntities.associate { it.medicineId!! to (it.medicineTitleEn ?: it.medicineTitleAr ?: "") }
        for (entity in medEntities) {
            val medId = entity.medicineId!!
            api.getMedicine(medId).enqueue(object : Callback<ApiResponse<MedicineDto>> {
                override fun onResponse(call: Call<ApiResponse<MedicineDto>>, response: Response<ApiResponse<MedicineDto>>) {
                    val dto = if (response.isSuccessful && response.body()?.success == true) response.body()?.data else null
                    val med = if (dto != null) dto.toMedicine() else fallbackMedicine(medId, titleMap[medId])
                    synchronized(results) { results.add(med) }
                    if (remaining.decrementAndGet() == 0) applyMedicines(results)
                }

                override fun onFailure(call: Call<ApiResponse<MedicineDto>>, t: Throwable) {
                    synchronized(results) { results.add(fallbackMedicine(medId, titleMap[medId])) }
                    if (remaining.decrementAndGet() == 0) applyMedicines(results)
                }
            })
        }
    }

    private fun applyMedicines(medicines: List<Medicine>) {
        val current = binding ?: return
        current.root.post { doApply(medicines) }
    }

    private fun doApply(medicines: List<Medicine>) {
        val current = binding ?: return
        adapter?.updateList(medicines)
        current.progressFavorites.visibility = View.GONE
        if (medicines.isEmpty()) {
            current.recyclerFavorites.visibility = View.GONE
            current.emptyFavoritesView.visibility = View.VISIBLE
        } else {
            current.recyclerFavorites.visibility = View.VISIBLE
            current.emptyFavoritesView.visibility = View.GONE
        }
    }

    private fun fallbackMedicine(medId: String, title: String?): Medicine =
        Medicine(medId, "", title ?: "", 0.0, "", ArrayList(), "", "", true)

    private fun MedicineDto.toMedicine(): Medicine {
        val title = titleEn.ifBlank { titleAr }
        val details = descriptionEn ?: descriptionAr ?: ""
        return Medicine(id, imageUrl ?: "", title, price ?: 0.0, details, ArrayList(), "", "", true)
    }

    // Adapter already synced the repo; just drop the row from this list (no double delete)
    private fun toggleFavorite(med: Medicine) {
        val current = binding ?: return
        try {
            val list = adapter?.currentList()?.filterNot { it.id == med.id } ?: return
            adapter?.updateList(list)
            if (list.isEmpty()) {
                current.recyclerFavorites.visibility = View.GONE
                current.emptyFavoritesView.visibility = View.VISIBLE
            }
        } catch (_: Exception) {}
    }

    /** Refreshes the favourite flag for the rows already in the adapter. */
    fun reloadFavorites(context: Context) {
        try { adapter?.loadFavorites(context.applicationContext) } catch (_: Exception) {}
    }

    fun setToolBarTitle(context: Context) {
        binding?.includeAppBarLayoutAlternatives?.toolbarApp?.title = context.getString(R.string.favorite)
    }
}
