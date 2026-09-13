package dev.anonymous.eilaji.ui.other.sub_categories

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.adapters.SubCategoriesAdapter
import dev.anonymous.eilaji.databinding.FragmentSubCategoriesBinding
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.models.server.SubCategory
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.CategoryDto
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PaginatedResult
import dev.anonymous.eilaji.utils.LoadingDialog
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SubCategoriesFragment : Fragment() {
    private var _binding: FragmentSubCategoriesBinding? = null
    private val binding get() = _binding!!
    private val bindingOrNull get() = _binding
    private lateinit var viewModel: SubCategoriesViewModel
    private lateinit var apiService: ApiService
    private val loadingDialog = LoadingDialog()
    private lateinit var categoryId: String
    private lateinit var categoryTitle: String
    private var selectedSubcategoryId: String? = null
    private var medicinesCall: Call<ApiResponse<PaginatedResult<MedicineDto>>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            try {
                val args = SubCategoriesFragmentArgs.fromBundle(arguments)
                categoryId = args.categoryId ?: ""
                categoryTitle = args.categoryTitle ?: "Category"
            } catch (_: Exception) {
                categoryId = arguments.getString("categoryId") ?: ""
                categoryTitle = arguments.getString("categoryTitle") ?: "Category"
            }
        } else {
            categoryId = ""
            categoryTitle = "Category"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSubCategoriesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SubCategoriesViewModel::class.java]
        try { apiService = NetworkModule.provideApiService(requireContext()) } catch (_: Exception) { }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try { binding.includeAppBarLayoutAlternatives.toolbarApp.title = categoryTitle } catch (_: Exception) {}
        displaySubCategories()
        displayMedicines()
        // Fetch subcategories first; medicines load filtered once first sub is known.
        // No unfiltered fetch here — avoids "all 36 at once" flash.
        fetchSubCategories()
        view.postDelayed({
            try { loadingDialog.dismiss() } catch (_: Exception) {}
        }, 8000)
    }

    override fun onDestroyView() {
        try { medicinesCall?.cancel() } catch (_: Exception) {}
        try { loadingDialog.dismiss() } catch (_: Exception) {}
        _binding = null
        super.onDestroyView()
    }

    private fun setupSubCategoriesRecycler(subCategoriesList: ArrayList<SubCategory>) {
        val b = bindingOrNull ?: return
        if (!isAdded) return
        // "All" pill at position 0 -> unfiltered view of the whole category
        val withAll = ArrayList<SubCategory>()
        withAll.add(SubCategory("", categoryId, "", "All"))
        withAll.addAll(subCategoriesList)
        with(b.recyclerSubCategories) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            // Highlight first real subcategory (index 1, after "All") to match auto-filter
            adapter = SubCategoriesAdapter(withAll, onSelect = { subId ->
                selectedSubcategoryId = subId.ifBlank { null }
                fetchMedicines()
            }, initialSelected = if (withAll.size > 1) 1 else 0)
            itemAnimator = null
        }
    }

    private fun setupMedicinesAdapter(medicinesList: ArrayList<Medicine>) {
        val b = bindingOrNull ?: return
        if (!isAdded) return
        with(b.recSubCategoriesMedicines) {
            setHasFixedSize(false)
            try { layoutManager = androidx.recyclerview.widget.GridLayoutManager(activity, 2) } catch (_: Exception) {}
            adapter = MedicinesAdapter(medicinesList, onItemClick = { med ->
                try {
                    val intent = android.content.Intent(requireContext(), dev.anonymous.eilaji.ui.other.base.AlternativesActivity::class.java)
                    intent.putExtra("fragmentType", dev.anonymous.eilaji.storage.enums.FragmentsKeys.medicine.name)
                    intent.putExtra("medicineId", med.id)
                    startActivity(intent)
                } catch (_: Exception) {}
            })
        }
    }

    private fun fetchSubCategories() {
        if (!::apiService.isInitialized) return
        apiService.getCategories().enqueue(object : Callback<ApiResponse<List<CategoryDto>>> {
            override fun onResponse(call: Call<ApiResponse<List<CategoryDto>>>, response: Response<ApiResponse<List<CategoryDto>>>) {
                if (!isAdded || _binding == null) return
                if (response.isSuccessful && response.body()?.success == true) {
                    val categories = response.body()?.data ?: emptyList()
                    val target = categories.find { it.id == categoryId }
                    val subs = target?.subcategories ?: emptyList()
                    try { viewModel.setSubCategoriesList(subs) } catch (_: Exception) {}
                    if (subs.isEmpty()) {
                        // Category has no subcategories — fall back to unfiltered list once
                        selectedSubcategoryId = null
                        fetchMedicines()
                    }
                } else {
                    Log.e("SubCategoriesFragment", "fetchSubCategories: error ${response.body()?.error} code ${response.code()}")
                    try {
                        val b = bindingOrNull ?: return
                        com.google.android.material.snackbar.Snackbar.make(b.root, "Failed to load — retry", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                            .setAction("Retry") { fetchSubCategories() }.show()
                    } catch (_: Exception) {}
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<CategoryDto>>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                if (call.isCanceled) return
                Log.e("SubCategoriesFragment", "fetchSubCategories: failure", t)
                try {
                    val b = bindingOrNull ?: return
                    com.google.android.material.snackbar.Snackbar.make(b.root, "Network error — retry", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction("Retry") { fetchSubCategories() }.show()
                } catch (_: Exception) {}
            }
        })
    }

    private fun fetchMedicines() {
        try { medicinesCall?.cancel() } catch (_: Exception) {}
        if (!isAdded || _binding == null) return
        if (!::apiService.isInitialized) return
        try { bindingOrNull?.recSubCategoriesMedicines?.alpha = 0.4f } catch (_: Exception) {}
        val call = apiService.getMedicines(subcategoryId = selectedSubcategoryId, pageSize = 50)
        medicinesCall = call
        call.enqueue(object : Callback<ApiResponse<PaginatedResult<MedicineDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<PaginatedResult<MedicineDto>>>) {
                if (!isAdded || _binding == null) return
                try { bindingOrNull?.recSubCategoriesMedicines?.alpha = 1f } catch (_: Exception) {}
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data?.items ?: emptyList()
                    try { viewModel.setMedicineList(items) } catch (_: Exception) {}
                } else {
                    Log.e("SubCategoriesFragment", "fetchMedicines: error ${response.body()?.error} code ${response.code()}")
                    try {
                        val b = bindingOrNull ?: return
                        com.google.android.material.snackbar.Snackbar.make(b.root, response.body()?.error ?: "Failed to load", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
            }

            override fun onFailure(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, t: Throwable) {
                if (!isAdded || _binding == null) return
                if (call.isCanceled) return
                try { bindingOrNull?.recSubCategoriesMedicines?.alpha = 1f } catch (_: Exception) {}
                Log.e("SubCategoriesFragment", "fetchMedicines: failure", t)
                try {
                    val b = bindingOrNull ?: return
                    com.google.android.material.snackbar.Snackbar.make(b.root, "Network error — retry", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction("Retry") { fetchMedicines() }.show()
                } catch (_: Exception) {}
            }
        })
    }

    private fun displaySubCategories() {
        viewModel.subCategoriesList.observe(viewLifecycleOwner) { dtoList ->
            if (!isAdded || _binding == null) return@observe
            val uiList = ArrayList(dtoList.map { dto ->
                SubCategory(dto.id, categoryId, dto.iconUrl ?: "", dto.nameEn.ifBlank { dto.nameAr })
            })
            setupSubCategoriesRecycler(uiList)
            if (selectedSubcategoryId == null) {
                // Default to first real subcategory (filtered), not "All"
                selectedSubcategoryId = uiList.firstOrNull()?.id
                fetchMedicines()
            }
        }
    }

    private fun displayMedicines() {
        viewModel.medicineList.observe(viewLifecycleOwner) { dtoList ->
            if (!isAdded || _binding == null) return@observe
            val uiList = ArrayList(dtoList.map { dto ->
                Medicine(dto.id, dto.imageUrl ?: "", dto.titleEn.ifBlank { dto.titleAr }, dto.price ?: 0.0, dto.descriptionEn ?: "", ArrayList(), categoryId, dto.subcategoryNameEn ?: selectedSubcategoryId ?: "", false)
            })
            try { setupMedicinesAdapter(uiList) } catch (_: Exception) {}
            try {
                bindingOrNull?.emptySubMedicines?.visibility = if (uiList.isEmpty()) View.VISIBLE else View.GONE
            } catch (_: Exception) {}
        }
    }
}
