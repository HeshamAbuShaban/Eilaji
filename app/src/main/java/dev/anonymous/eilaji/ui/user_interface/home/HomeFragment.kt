package dev.anonymous.eilaji.ui.user_interface.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.adapters.AdsAdapter
import dev.anonymous.eilaji.adapters.CategoriesPharmaceuticalsAdapter
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.databinding.FragmentHomeBinding
import dev.anonymous.eilaji.utils.DepthPageTransformer
import dev.anonymous.eilaji.utils.DummyData

class HomeFragment : Fragment() {
    private lateinit var _binding: FragmentHomeBinding
    // private late-init var homeViewModel: HomeViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdsPager()
        setupCategoriesPharmaceuticalsRecycler()
        setupBestSellerRecycler()
    }

    private fun setupAdsPager() {
        with(binding.pagerAds) {
            adapter = AdsAdapter(DummyData.listAdModels)
            setPageTransformer(DepthPageTransformer())
            binding.indicatorAds.setupViewPager2(this, 0)
        }
    }

    private fun setupCategoriesPharmaceuticalsRecycler() {
        with(binding.recyclerCategoriesPharmaceuticals) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = CategoriesPharmaceuticalsAdapter(DummyData.listCategoriesPharmaceuticalModels)
        }
    }

    private fun setupBestSellerRecycler() {
        with(binding.recyclerBestSeller) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = MedicinesAdapter(DummyData.listMedicineModels)
        }
    }
}




