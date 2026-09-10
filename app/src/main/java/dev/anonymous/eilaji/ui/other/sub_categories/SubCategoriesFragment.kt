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
    private lateinit var binding: FragmentSubCategoriesBinding
    private lateinit var viewModel: SubCategoriesViewModel
    private lateinit var apiService: ApiService
    private val loadingDialog = LoadingDialog()
    private lateinit var categoryId: String
    private lateinit var categoryTitle: String
    private var selectedSubcategoryId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog.show(childFragmentManager, "FetchingData")
        val arguments = arguments
        if (arguments != null) {
            val args = SubCategoriesFragmentArgs.fromBundle(arguments)
            categoryId = args.categoryId
            categoryTitle = args.categoryTitle
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSubCategoriesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SubCategoriesViewModel::class.java]
        apiService = NetworkModule.provideApiService(requireContext())
        fetchMedicines()
        fetchSubCategories()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeAppBarLayoutAlternatives.toolbarApp.title = categoryTitle
        displaySubCategories()
        displayMedicines()
        loadingDialog.dismiss()
    }

    private fun setupSubCategoriesRecycler(subCategoriesList: ArrayList<SubCategory>) {
        with(binding.recyclerSubCategories) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = SubCategoriesAdapter(subCategoriesList) { subId ->
                selectedSubcategoryId = subId
                fetchMedicines()
            }
            itemAnimator = null
        }
    }

    private fun setupMedicinesAdapter(medicinesList: ArrayList<Medicine>) {
        with(binding.recSubCategoriesMedicines) {
            setHasFixedSize(false)
            adapter = MedicinesAdapter(medicinesList, onItemClick = { med ->
                val intent = android.content.Intent(requireContext(), dev.anonymous.eilaji.ui.other.base.AlternativesActivity::class.java)
                intent.putExtra("fragmentType", dev.anonymous.eilaji.storage.enums.FragmentsKeys.medicine.name)
                intent.putExtra("medicineId", med.id)
                startActivity(intent)
            })
        }
    }

    private fun fetchSubCategories() {
        apiService.getCategories().enqueue(object : Callback<ApiResponse<List<CategoryDto>>> {
            override fun onResponse(call: Call<ApiResponse<List<CategoryDto>>>, response: Response<ApiResponse<List<CategoryDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val categories = response.body()?.data ?: emptyList()
                    val target = categories.find { it.id == categoryId }
                    val subs = target?.subcategories ?: emptyList()
                    viewModel.setSubCategoriesList(subs)
                } else {
                    Log.e("SubCategoriesFragment", "fetchSubCategories: error ${response.body()?.error} code ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<CategoryDto>>>, t: Throwable) {
                Log.e("SubCategoriesFragment", "fetchSubCategories: failure", t)
            }
        })
    }

    private fun fetchMedicines() {
        apiService.getMedicines(subcategoryId = selectedSubcategoryId).enqueue(object : Callback<ApiResponse<PaginatedResult<MedicineDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<PaginatedResult<MedicineDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val items = response.body()?.data?.items ?: emptyList()
                    viewModel.setMedicineList(items)
                } else {
                    Log.e("SubCategoriesFragment", "fetchMedicines: error ${response.body()?.error} code ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ApiResponse<PaginatedResult<MedicineDto>>>, t: Throwable) {
                Log.e("SubCategoriesFragment", "fetchMedicines: failure", t)
            }
        })
    }

    private fun displaySubCategories() {
        viewModel.subCategoriesList.observe(viewLifecycleOwner) { dtoList ->
            val uiList = ArrayList(dtoList.map { dto ->
                SubCategory(dto.id, categoryId, dto.iconUrl ?: "", dto.nameEn)
            })
            setupSubCategoriesRecycler(uiList)
            if (uiList.isNotEmpty() && selectedSubcategoryId == null) {
                selectedSubcategoryId = uiList[0].id
                fetchMedicines()
            }
        }
    }

    private fun displayMedicines() {
        viewModel.medicineList.observe(viewLifecycleOwner) { dtoList ->
            val uiList = ArrayList(dtoList.map { dto ->
                Medicine(dto.id, dto.imageUrl ?: "", dto.titleEn, dto.price ?: 0.0, dto.descriptionEn ?: "", ArrayList(), categoryId, dto.subcategoryNameEn ?: selectedSubcategoryId ?: "", false)
            })
            setupMedicinesAdapter(uiList)
        }
    }
}
