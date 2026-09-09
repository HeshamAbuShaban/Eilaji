package dev.anonymous.eilaji.ui.other.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.adapters.PharmaciesLocationsAdapter
import dev.anonymous.eilaji.databinding.FragmentSearchBinding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PaginatedResult
import dev.anonymous.eilaji.network.PharmacyDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private lateinit var _binding: FragmentSearchBinding
    private val binding get() = _binding

    private lateinit var searchViewModel: SearchViewModel

    private val apiService by lazy { NetworkModule.provideApiService(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(layoutInflater)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fetchMedicinesData()
        displayMedicines()
        fetchPharmaciesData()
        displayPharmacies()
        setupSearchListener()
    }

    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim().orEmpty()
                if (q.isEmpty()) {
                    fetchMedicinesData()
                } else {
                    searchMedicines(q)
                }
            }
        })
    }

    private fun setupMedicinesAdapter(medicinesList: ArrayList<Medicine>) {
        with(binding.recVSearchMedicines) {
            adapter = MedicinesAdapter(medicinesList)
        }
    }

    private fun setupPharmaciesAdapter(pharmacy: ArrayList<Pharmacy>) {
        with(binding.recVSearchPharmacies) {
            adapter = PharmaciesLocationsAdapter(pharmacy,
                navigateToChat = {
                    val action = SearchFragmentDirections.actionNavigationSearchToNavigationMessaging(
                        null,
                        it.uid,
                        it.pharmacy_name,
                        it.pharmacy_image_url,
                        it.token,
                        null,
                        null
                    )
                    findNavController().navigate(action)
            })
        }
    }

    private fun fetchMedicinesData() {
        apiService.getMedicines(page = 0, pageSize = 50).enqueue(object : Callback<ApiResponse<PaginatedResult<MedicineDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<PaginatedResult<MedicineDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val dtos = response.body()?.data?.items ?: emptyList()
                    val medicines = ArrayList(dtos.map { it.toMedicine() })
                    searchViewModel.setMedicinesData(medicines)
                } else {
                    val msg = response.body()?.error ?: "Failed to load medicines"
                    Log.e("SearchFragment", "fetchMedicines: $msg")
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, t: Throwable) {
                Log.e("SearchFragment", "fetchMedicines: network error", t)
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchMedicines(query: String) {
        apiService.searchMedicines(query, page = 0, pageSize = 50).enqueue(object : Callback<ApiResponse<PaginatedResult<MedicineDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<PaginatedResult<MedicineDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val dtos = response.body()?.data?.items ?: emptyList()
                    val medicines = ArrayList(dtos.map { it.toMedicine() })
                    searchViewModel.setMedicinesData(medicines)
                } else {
                    val msg = response.body()?.error ?: "Search failed"
                    Log.e("SearchFragment", "searchMedicines: $msg")
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, t: Throwable) {
                Log.e("SearchFragment", "searchMedicines: network error", t)
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayMedicines(){
        searchViewModel.medicineData.observe(viewLifecycleOwner) {
            setupMedicinesAdapter(it)
        }
    }

    private fun fetchPharmaciesData() {
        apiService.getPharmacies(page = 0, pageSize = 50).enqueue(object : Callback<ApiResponse<PaginatedResult<PharmacyDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<PharmacyDto>>>, response: Response<ApiResponse<PaginatedResult<PharmacyDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val dtos = response.body()?.data?.items ?: emptyList()
                    val pharmacies = ArrayList(dtos.map { it.toPharmacy() })
                    searchViewModel.setPharmaciesDataData(pharmacies)
                } else {
                    val msg = response.body()?.error ?: "Failed to load pharmacies"
                    Log.e("SearchFragment", "fetchPharmacies: $msg")
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<PharmacyDto>>>, t: Throwable) {
                Log.e("SearchFragment", "fetchPharmacies: network error", t)
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayPharmacies(){
        searchViewModel.pharmaciesData.observe(viewLifecycleOwner) {
            setupPharmaciesAdapter(it)
        }
    }

    private fun MedicineDto.toMedicine(): Medicine {
        val title = titleEn.ifBlank { titleAr }
        val details = descriptionEn ?: descriptionAr ?: ""
        return Medicine(
            id = id,
            imageUrl = imageUrl ?: "",
            title = title,
            price = price ?: 0.0,
            details = details,
            alternativesMedicine = ArrayList(),
            idCategory = "",
            idSubCategory = "",
            isFavorite = false
        )
    }

    private fun PharmacyDto.toPharmacy(): Pharmacy {
        return Pharmacy(
            uid = id,
            pharmacy_image_url = imageUrl ?: "",
            pharmacy_name = name,
            phone = phone ?: "",
            address = address,
            lat = latitude,
            lng = longitude,
            token = ""
        )
    }
}
