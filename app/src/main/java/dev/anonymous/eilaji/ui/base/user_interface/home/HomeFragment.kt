package dev.anonymous.eilaji.ui.base.user_interface.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.AdsAdapter
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.adapters.PharmaciesLocationsAdapter
import dev.anonymous.eilaji.adapters.server.CategoryAdapter
import dev.anonymous.eilaji.databinding.FragmentHomeBinding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.models.server.Ad
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.CategoryDto
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PaginatedResult
import dev.anonymous.eilaji.network.PharmacyDto
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
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
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var timeoutFired = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        setupSwipeRefresh()
        setupQuickActions()
        setupRemindersTeaser()
        hideShimmerIfDataPresent()
        displayAds()
        displayCategories()
        displayBestSellers()
        displayNearby()
        fetchAds()
        fetchCategories()
        fetchBestSellers()
        fetchNearbyPharmacies()
        scheduleShimmerTimeout()
    }

    override fun onStart() {
        super.onStart()
        startShimmers()
        hideShimmerIfDataPresent()
        scheduleShimmerTimeout()
    }

    private fun hideShimmerIfDataPresent() {
        if (homeViewModel.adsList.value != null) removeAdsShimmer(homeViewModel.adsList.value?.isNotEmpty() == true)
        if (homeViewModel.categories.value != null) {
            binding.shimmerCategoriesPharmaceuticalsContainer.stopShimmer()
            binding.shimmerCategoriesPharmaceuticalsContainer.visibility = View.GONE
            binding.recyclerCategoriesPharmaceuticals.visibility = if (homeViewModel.categories.value!!.isEmpty()) View.GONE else View.VISIBLE
        }
        if (homeViewModel.bestSellers.value != null) {
            binding.shimmerMedContainer.stopShimmer()
            binding.shimmerMedContainer.visibility = View.GONE
            binding.recyclerBestSeller.visibility = if (homeViewModel.bestSellers.value!!.isEmpty()) View.GONE else View.VISIBLE
        }
        if (homeViewModel.nearbyPharmacies.value != null) {
            binding.shimmerNearbyContainer.stopShimmer()
            binding.shimmerNearbyContainer.visibility = View.GONE
            val list = homeViewModel.nearbyPharmacies.value!!
            binding.recyclerNearbyPharmacies.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            binding.tvNearbyEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
        if (hasAllData()) cancelTimeout()
    }

    private fun hasAllData(): Boolean {
        return homeViewModel.categories.value != null && homeViewModel.bestSellers.value != null && homeViewModel.adsList.value != null
    }

    private fun scheduleShimmerTimeout() {
        cancelTimeout()
        timeoutFired = false
        timeoutRunnable = Runnable {
            timeoutFired = true
            hideShimmerAndShowEmpty()
            binding.swipeRefresh.isRefreshing = false
        }
        handler.postDelayed(timeoutRunnable!!, 8000)
    }

    private fun cancelTimeout() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null
    }

    private fun hideShimmerAndShowEmpty() {
        with(binding) {
            if (shimmerAdContainer.visibility == View.VISIBLE) {
                shimmerAdContainer.stopShimmer()
                shimmerAdContainer.visibility = View.GONE
            }
            if (shimmerCategoriesPharmaceuticalsContainer.visibility == View.VISIBLE) {
                shimmerCategoriesPharmaceuticalsContainer.stopShimmer()
                shimmerCategoriesPharmaceuticalsContainer.visibility = View.GONE
                if (homeViewModel.categories.value == null) recyclerCategoriesPharmaceuticals.visibility = View.GONE
            }
            if (shimmerMedContainer.visibility == View.VISIBLE) {
                shimmerMedContainer.stopShimmer()
                shimmerMedContainer.visibility = View.GONE
                if (homeViewModel.bestSellers.value == null) recyclerBestSeller.visibility = View.GONE
            }
            if (shimmerNearbyContainer.visibility == View.VISIBLE) {
                shimmerNearbyContainer.stopShimmer()
                shimmerNearbyContainer.visibility = View.GONE
                if (homeViewModel.nearbyPharmacies.value == null) {
                    recyclerNearbyPharmacies.visibility = View.GONE
                    tvNearbyEmpty.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startShimmers() {
        val shouldShow = homeViewModel.categories.value == null && homeViewModel.bestSellers.value == null && homeViewModel.adsList.value == null
        if (!shouldShow || timeoutFired) return
        with(binding) {
            if (homeViewModel.adsList.value == null) {
                shimmerAdContainer.startShimmer()
                shimmerAdContainer.visibility = View.VISIBLE
                pagerAds.visibility = View.GONE
                indicatorAds.visibility = View.GONE
            }
            if (homeViewModel.categories.value == null) {
                shimmerCategoriesPharmaceuticalsContainer.startShimmer()
                shimmerCategoriesPharmaceuticalsContainer.visibility = View.VISIBLE
                recyclerCategoriesPharmaceuticals.visibility = View.GONE
            }
            if (homeViewModel.bestSellers.value == null) {
                shimmerMedContainer.startShimmer()
                shimmerMedContainer.visibility = View.VISIBLE
                recyclerBestSeller.visibility = View.GONE
            }
            if (homeViewModel.nearbyPharmacies.value == null) {
                shimmerNearbyContainer.startShimmer()
                shimmerNearbyContainer.visibility = View.VISIBLE
                recyclerNearbyPharmacies.visibility = View.GONE
                tvNearbyEmpty.visibility = View.GONE
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            cancelTimeout()
            timeoutFired = false
            fetchAds()
            fetchCategories()
            fetchBestSellers()
            fetchNearbyPharmacies()
            scheduleShimmerTimeout()
            handler.postDelayed({ if (hasAllData()) binding.swipeRefresh.isRefreshing = false }, 2000)
        }
    }

    private fun setupQuickActions() {
        binding.cardUploadPrescription.setOnClickListener {
            try { requireActivity().findViewById<View>(R.id.navigation_send_prescription)?.performClick() } catch (_: Exception) {
                startActivity(Intent(requireContext(), AlternativesActivity::class.java).apply { putExtra("fragmentType", "send_prescription") })
            }
        }
        binding.cardFindPharmacy.setOnClickListener {
            try {
                val intent = Intent(requireContext(), AlternativesActivity::class.java)
                intent.putExtra("fragmentType", "map")
                startActivity(intent)
            } catch (_: Exception) {}
        }
        binding.cardChat.setOnClickListener {
            try { requireActivity().findViewById<View>(R.id.navigation_chatting)?.performClick() } catch (_: Exception) {}
        }
        binding.cardReminders.setOnClickListener {
            startActivity(Intent(requireContext(), AlternativesActivity::class.java).apply { putExtra("fragmentType", "reminders_list") })
            try {
                val intent = Intent(requireContext(), AlternativesActivity::class.java)
                intent.putExtra("fragmentType", FragmentsKeys.reminder.name)
                startActivity(intent)
            } catch (_: Exception) {}
        }
        binding.buShowAllReminders.setOnClickListener { binding.cardReminders.performClick() }
        binding.cardRemindersTeaser.setOnClickListener { binding.cardReminders.performClick() }
    }

    private fun setupRemindersTeaser() {
        try {
            val vm = ViewModelProvider(requireActivity())[ReminderDatabaseViewModel::class.java]
            vm.allReminders.observe(viewLifecycleOwner) { list ->
                val count = list?.size ?: 0
                binding.tvReminderCount.text = if (count == 0) "2 reminders today" else "$count reminders today"
            }
        } catch (_: Exception) {
            binding.tvReminderCount.text = "2 reminders today"
        }
    }

    private fun setupListeners() {
        binding.buShowAllBestSeller.setOnClickListener {
            startActivity(Intent(requireContext(), AlternativesActivity::class.java).apply { putExtra("fragmentType", FragmentsKeys.medicine.name) })
        }
        binding.buShowAllCategoriesPharmaceuticals.setOnClickListener {
            try { requireActivity().findViewById<View>(R.id.navigation_categories)?.performClick() } catch (_: Exception) {}
        }
        binding.buShowAllNearby.setOnClickListener { binding.cardFindPharmacy.performClick() }
    }

    private fun displayAds() {
        homeViewModel.adsList.observe(viewLifecycleOwner) { adsList ->
            if (adsList != null) setupAdsPager(adsList)
            if (hasAllData()) { cancelTimeout(); binding.swipeRefresh.isRefreshing = false }
        }
    }

    private fun displayCategories() {
        if (homeViewModel.categories.value != null) {
            binding.shimmerCategoriesPharmaceuticalsContainer.stopShimmer()
            binding.shimmerCategoriesPharmaceuticalsContainer.visibility = View.GONE
        }
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
            if (hasAllData()) { cancelTimeout(); binding.swipeRefresh.isRefreshing = false }
        }
    }

    private fun displayBestSellers() {
        if (homeViewModel.bestSellers.value != null) {
            binding.shimmerMedContainer.stopShimmer()
            binding.shimmerMedContainer.visibility = View.GONE
        }
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
            if (hasAllData()) { cancelTimeout(); binding.swipeRefresh.isRefreshing = false }
        }
    }

    private fun displayNearby() {
        if (homeViewModel.nearbyPharmacies.value != null) {
            binding.shimmerNearbyContainer.stopShimmer()
            binding.shimmerNearbyContainer.visibility = View.GONE
        }
        homeViewModel.nearbyPharmacies.observe(viewLifecycleOwner) { list ->
            if (list == null) return@observe
            binding.shimmerNearbyContainer.stopShimmer()
            binding.shimmerNearbyContainer.visibility = View.GONE
            if (list.isEmpty()) {
                binding.recyclerNearbyPharmacies.visibility = View.GONE
                binding.tvNearbyEmpty.visibility = View.VISIBLE
            } else {
                binding.tvNearbyEmpty.visibility = View.GONE
                binding.recyclerNearbyPharmacies.visibility = View.VISIBLE
                binding.recyclerNearbyPharmacies.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                val pharmacies = ArrayList(list.map { dto ->
                    Pharmacy(dto.id, dto.imageUrl ?: "", dto.name, dto.phone ?: "", dto.address, dto.latitude, dto.longitude, "")
                })
                binding.recyclerNearbyPharmacies.adapter = PharmaciesLocationsAdapter(pharmacies) { model ->
                    val intent = Intent(requireContext(), AlternativesActivity::class.java)
                    intent.putExtra("fragmentType", "map")
                    intent.putExtra("pharmacyId", model.uid)
                    startActivity(intent)
                }
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

    private fun fetchNearbyPharmacies() {
        val lat = 31.5
        val lng = 34.46
        val api = NetworkModule.provideApiService(requireContext())
        api.getNearbyPharmacies(lat, lng, 10.0).enqueue(object : Callback<ApiResponse<List<PharmacyDto>>> {
            override fun onResponse(call: Call<ApiResponse<List<PharmacyDto>>>, response: Response<ApiResponse<List<PharmacyDto>>>) {
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null && response.body()?.data!!.isNotEmpty()) {
                    homeViewModel.setNearbyPharmacies(response.body()?.data!!)
                } else {
                    fetchAllPharmaciesFallback()
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<PharmacyDto>>>, t: Throwable) {
                fetchAllPharmaciesFallback()
            }
        })
    }

    private fun fetchAllPharmaciesFallback() {
        NetworkModule.provideApiService(requireContext()).getPharmacies(page = 0, pageSize = 10).enqueue(object : Callback<ApiResponse<PaginatedResult<PharmacyDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<PharmacyDto>>>, response: Response<ApiResponse<PaginatedResult<PharmacyDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    homeViewModel.setNearbyPharmacies(response.body()?.data?.items ?: emptyList())
                } else homeViewModel.setNearbyPharmacies(emptyList())
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<PharmacyDto>>>, t: Throwable) {
                homeViewModel.setNearbyPharmacies(emptyList())
            }
        })
    }

    override fun onStop() {
        super.onStop()
        binding.shimmerAdContainer.stopShimmer()
        binding.shimmerMedContainer.stopShimmer()
        binding.shimmerCategoriesPharmaceuticalsContainer.stopShimmer()
        try { binding.shimmerNearbyContainer.stopShimmer() } catch (_: Exception) {}
        cancelTimeout()
    }
}
