package dev.anonymous.eilaji.ui.other.favorite

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.databinding.FragmentFavoritesBinding
import dev.anonymous.eilaji.models.MedicineModel
import dev.anonymous.eilaji.utils.DummyData
import dev.anonymous.eilaji.utils.UtilsScreen

class FavoritesFragment : Fragment() {
    private lateinit var _binding: FragmentFavoritesBinding
    private val binding get() = _binding

    private lateinit var viewModel: FavoriteViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]

        binding.includeAppBarLayoutAlternatives.toolbarApp.title = getString(R.string.favorite)

        setupFavoritesRecycler()

        return binding.root
    }

    private fun setupFavoritesRecycler() {
        val halfScreenWidth: Int = UtilsScreen.screenWidth / 2
        val medicinesAdapter = MedicinesAdapter(arrayListOf(), true, halfScreenWidth)

        with(binding.recyclerFavorites) {
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(context, 2)
            adapter = medicinesAdapter
        }

        Thread {
            SystemClock.sleep(1500)
            activity?.runOnUiThread {
                val list: ArrayList<MedicineModel> = ArrayList()
                for (medicine in DummyData.listMedicineModels) {
                    if (medicine.isFavorite) {
                        list.add(medicine)
                    }
                }
                binding.progressFavorites.visibility = View.GONE
                medicinesAdapter.setListMedicines(list)
            }
        }.start()
    }
}