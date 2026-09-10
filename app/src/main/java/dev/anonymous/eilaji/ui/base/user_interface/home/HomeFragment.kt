package dev.anonymous.eilaji.ui.base.user_interface.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import dev.anonymous.eilaji.adapters.AdsAdapter
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.adapters.server.CategoryAdapter
import dev.anonymous.eilaji.databinding.FragmentHomeBinding
import dev.anonymous.eilaji.models.server.Ad
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.CategoryDto
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PaginatedResult
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.utils.DepthPageTransformer
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var _binding: FragmentHomeBinding
    private val binding get() = _binding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        displayAds()
        displayCategories()
        displayBestSellers()
        fetchAds()
        fetchCategories()
        fetchBestSellers()
    }

    override fun onStart() {
        super.onStart()
        startShimmers()
    }

    private fun startShimmers() {
        with(binding) {
            shimmerAdContainer.startShimmer()
            shimmerMedContainer.startShimmer()
            shimmerCategoriesPharmaceuticalsContainer.startShimmer()
            shimmerAdContainer.visibility = View.VISIBLE
            shimmerMedContainer.visibility = View.VISIBLE
            shimmerCategoriesPharmaceuticalsContainer.visibility = View.VISIBLE
            pagerAds.visibility = View.GONE
            indicatorAds.visibility = View.GONE
            recyclerCategoriesPharmaceuticals.visibility = View.GONE
            recyclerBestSeller.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.buShowAllBestSeller.setOnClickListener {
            startActivity(Intent(requireContext(), AlternativesActivity::class.java).apply { putExtra("fragmentType", FragmentsKeys.medicine.name) })
        }
        binding.buShowAllCategoriesPharmaceuticals.setOnClickListener {
            try { requireActivity().findViewById<View>(dev.anonymous.eilaji.R.id.navigation_categories)?.performClick() } catch (_: Exception) {}
        }
    }

    private fun displayAds() {
        homeViewModel.adsList.observe(viewLifecycleOwner) { adsList ->
            if (adsList != null) setupAdsPager(adsList)
        }
    }

    private fun displayCategories() {
        homeViewModel.categories.observe(viewLifecycleOwner) { list ->
            if (list == null) return@observe
            binding.shimmerCategoriesPharmaceuticalsContainer.stopShimmer()
            binding.shimmerCategoriesPharmaceuticalsContainer.visibility = View.GONE
            if (list.isEmpty()) {
                binding.recyclerCategoriesPharmaceuticals.visibility = View.GONE
            } else {
                binding.recyclerCategoriesPharmaceuticals.visibility = View.VISIBLE
                binding.recyclerCategoriesPharmaceuticals.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                binding.recyclerCategoriesPharmaceuticals.adapter = CategoryAdapter(list) { id, title ->
                    val intent = Intent(requireContext(), AlternativesActivity::class.java)
                    intent.putExtra("fragmentType", "subCategories")
                    intent.putExtra("categoryId", id)
                    intent.putExtra("categoryTitle", title)
                    startActivity(intent)
                }
            }
        }
    }

    private fun displayBestSellers() {
        homeViewModel.bestSellers.observe(viewLifecycleOwner) { dtoList ->
            if (dtoList == null) return@observe
            binding.shimmerMedContainer.stopShimmer()
            binding.shimmerMedContainer.visibility = View.GONE
            if (dtoList.isEmpty()) {
                binding.recyclerBestSeller.visibility = View.GONE
            } else {
                binding.recyclerBestSeller.visibility = View.VISIBLE
                val uiList = ArrayList(dtoList.map { dto ->
                    Medicine(dto.id, dto.imageUrl ?: "", dto.titleEn.ifBlank { dto.titleAr }, dto.price ?: 0.0, dto.descriptionEn ?: "", ArrayList(), "", dto.subcategoryNameEn ?: "", false)
                })
                binding.recyclerBestSeller.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                binding.recyclerBestSeller.adapter = MedicinesAdapter(uiList, onItemClick = { med ->
                    val intent = Intent(requireContext(), AlternativesActivity::class.java)
                    intent.putExtra("fragmentType", FragmentsKeys.medicine.name)
                    intent.putExtra("medicineId", med.id)
                    startActivity(intent)
                })
            }
        }
    }

    private fun removeAdsShimmer(show: Boolean) {
        with(binding.shimmerAdContainer) {
            stopShimmer()
            visibility = View.GONE
        }
        if (show) {
            binding.pagerAds.visibility = View.VISIBLE
            binding.indicatorAds.visibility = View.VISIBLE
        } else {
            binding.pagerAds.visibility = View.GONE
            binding.indicatorAds.visibility = View.GONE
        }
    }

    private fun setupAdsPager(adsList: ArrayList<Ad>) {
        if (adsList.isEmpty()) {
            removeAdsShimmer(false)
            return
        }
        with(binding.pagerAds) {
            val adsAdapter = AdsAdapter(ArrayList()) { ad ->
                val intent = Intent(requireContext(), AlternativesActivity::class.java)
                intent.putExtra("fragmentType", FragmentsKeys.medicine.name)
                intent.putExtra("medicineId", ad.id)
                startActivity(intent)
            }
            adapter = adsAdapter
            setPageTransformer(DepthPageTransformer())
            adsAdapter.setListAds(adsList)
            binding.indicatorAds.setupViewPager2(this, adsList.size, 0)
        }
        removeAdsShimmer(true)
    }

    private fun fetchAds() {
        NetworkModule.provideApiService(requireContext()).getMedicines(page = 0, pageSize = 5).enqueue(object : Callback<ApiResponse<PaginatedResult<MedicineDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<PaginatedResult<MedicineDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data?.items ?: emptyList()
                    if (items.isEmpty()) homeViewModel.setAdsList(ArrayList())
                    else homeViewModel.setAdsList(ArrayList(items.map { dto -> Ad(dto.id, dto.imageUrl ?: "", dto.titleEn.ifBlank { dto.titleAr }) }))
                } else {
                    Log.e("HomeFragment", "fetchAds: ${response.body()?.error}")
                    homeViewModel.setAdsList(ArrayList())
                }
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, t: Throwable) {
                Log.e("HomeFragment", "fetchAds: network error", t)
                homeViewModel.setAdsList(ArrayList())
            }
        })
    }

    private fun fetchCategories() {
        NetworkModule.provideApiService(requireContext()).getCategories().enqueue(object : Callback<ApiResponse<List<CategoryDto>>> {
            override fun onResponse(call: Call<ApiResponse<List<CategoryDto>>>, response: Response<ApiResponse<List<CategoryDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    homeViewModel.setCategories(response.body()?.data ?: emptyList())
                } else {
                    homeViewModel.setCategories(emptyList())
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<CategoryDto>>>, t: Throwable) {
                Log.e("HomeFragment", "fetchCategories fail", t)
                homeViewModel.setCategories(emptyList())
            }
        })
    }

    private fun fetchBestSellers() {
        NetworkModule.provideApiService(requireContext()).getMedicines(page = 0, pageSize = 10).enqueue(object : Callback<ApiResponse<PaginatedResult<MedicineDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<PaginatedResult<MedicineDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    homeViewModel.setBestSellers(response.body()?.data?.items ?: emptyList())
                } else homeViewModel.setBestSellers(emptyList())
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, t: Throwable) {
                Log.e("HomeFragment", "fetchBestSellers fail", t)
                homeViewModel.setBestSellers(emptyList())
            }
        })
    }

    override fun onStop() {
        super.onStop()
        binding.shimmerAdContainer.stopShimmer()
        binding.shimmerMedContainer.stopShimmer()
        binding.shimmerCategoriesPharmaceuticalsContainer.stopShimmer()
    }
}
