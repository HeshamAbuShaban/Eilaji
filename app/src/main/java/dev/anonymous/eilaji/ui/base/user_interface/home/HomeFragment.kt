package dev.anonymous.eilaji.ui.base.user_interface.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.anonymous.eilaji.adapters.AdsAdapter
import dev.anonymous.eilaji.databinding.FragmentHomeBinding
import dev.anonymous.eilaji.models.server.Ad
import dev.anonymous.eilaji.network.ApiResponse
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
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        fetchAds()
        displayAds()
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
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding.buShowAllBestSeller.setOnClickListener {
            val intent = Intent(requireContext(), AlternativesActivity::class.java)
            intent.putExtra("fragmentType", FragmentsKeys.medicine.name)
            startActivity(intent)
        }
    }

    private fun displayAds() {
        homeViewModel.adsList.observe(viewLifecycleOwner) { adsList ->
            if (adsList != null) {
                setupAdsPager(adsList)
            }
        }
    }

    private fun removeAdsShimmer() {
        with(binding.shimmerAdContainer) {
            stopShimmer()
            val isVisible = isVisible
            visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }

    private fun setupAdsPager(adsList: ArrayList<Ad>) {
        with(binding.pagerAds) {
            val adsAdapter = AdsAdapter(ArrayList())
            adapter = adsAdapter
            setPageTransformer(DepthPageTransformer())
            adsAdapter.setListAds(adsList)
            binding.indicatorAds.setupViewPager2(this, adsList.size,0)
        }
    }

    private fun fetchAds() {
        val apiService = NetworkModule.provideApiService(requireContext())
        apiService.getMedicines(page = 0, pageSize = 5).enqueue(object : Callback<ApiResponse<PaginatedResult<MedicineDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<PaginatedResult<MedicineDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data?.items ?: emptyList()
                    if (items.isEmpty()) {
                        homeViewModel.setAdsList(ArrayList())
                        hideAdsSection()
                    } else {
                        val adList = ArrayList(items.map { dto ->
                            Ad(
                                id = dto.id,
                                imageUrl = dto.imageUrl ?: "",
                                title = dto.titleEn.ifBlank { dto.titleAr }
                            )
                        })
                        homeViewModel.setAdsList(adList)
                    }
                } else {
                    Log.e("HomeFragment", "fetchAds: ${response.body()?.error ?: response.message()}")
                    homeViewModel.setAdsList(ArrayList())
                    hideAdsSection()
                }
                removeAdsShimmer()
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, t: Throwable) {
                Log.e("HomeFragment", "fetchAds: network error", t)
                homeViewModel.setAdsList(ArrayList())
                hideAdsSection()
                removeAdsShimmer()
            }
        })
    }

    private fun hideAdsSection() {
        binding.pagerAds.visibility = View.GONE
        binding.indicatorAds.visibility = View.GONE
    }

    override fun onStop() {
        super.onStop()
        removeAdsListeners()
    }

    private fun removeAdsListeners() {
        homeViewModel.adsList.removeObservers(viewLifecycleOwner)
    }
}
