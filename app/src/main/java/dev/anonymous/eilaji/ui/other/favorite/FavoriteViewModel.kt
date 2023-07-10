package dev.anonymous.eilaji.ui.other.favorite

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.databinding.FragmentFavoritesBinding
import dev.anonymous.eilaji.utils.UtilsScreen

class FavoriteViewModel : ViewModel() {
    private lateinit var _binding: FragmentFavoritesBinding
    private val binding: FragmentFavoritesBinding get() = _binding
    fun setBindingObj(binding: FragmentFavoritesBinding) {
        this._binding = binding
    }

    fun setupFavoritesRecycler(context: Activity) {
        val halfScreenWidth: Int = UtilsScreen.screenWidth / 2
        val medicinesAdapter = MedicinesAdapter(arrayListOf(), true, halfScreenWidth)

        with(binding.recyclerFavorites) {
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(context, 2)
            adapter = medicinesAdapter
        }
    }

    fun setToolBarTitle(context: Context) {
        binding.includeAppBarLayoutAlternatives.toolbarApp.title =
            context.getString(R.string.favorite)
    }
}