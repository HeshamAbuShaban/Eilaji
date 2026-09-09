package dev.anonymous.eilaji.ui.other.favorite

import android.app.Activity
import android.content.Context
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
import dev.anonymous.eilaji.utils.UtilsScreen

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
    }

    fun updateList(entities: List<FavoriteEntity>) {
        val medicines = entities.mapNotNull { e ->
            e.medicineId?.let { Medicine(it, "", e.medicineTitleEn ?: e.medicineTitleAr ?: "", 0.0, "", ArrayList(), "", "", true) }
        }
        adapter?.let { a ->
            try {
                val f = a.javaClass.getDeclaredField("medicineModels")
                f.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                (f.get(a) as ArrayList<Medicine>).apply { clear(); addAll(medicines) }
                a.notifyDataSetChanged()
            } catch (_: Exception) {}
        }
        binding.progressFavorites.visibility = android.view.View.GONE
    }

    private fun toggleFavorite(med: Medicine) { repo.syncDelete(med.id) }

    fun setToolBarTitle(context: Context) {
        binding.includeAppBarLayoutAlternatives.toolbarApp.title = context.getString(R.string.favorite)
    }
}
