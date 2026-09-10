package dev.anonymous.eilaji.ui.base.user_interface.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import android.content.Intent
import dev.anonymous.eilaji.adapters.server.CategoryAdapter
import dev.anonymous.eilaji.databinding.FragmentCategoriesBinding
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.utils.LoadingDialog

class CategoriesFragment : Fragment() {

    private lateinit var _binding: FragmentCategoriesBinding
    private val binding get() = _binding

    private lateinit var categoriesViewModel: CategoriesViewModel

    private val loadingDialog = LoadingDialog()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        categoriesViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        categoriesViewModel.init(requireContext())
        loadingDialog.show(requireActivity().supportFragmentManager, "Loading")
        displayCategories()
        fetchCategories()
        categoriesViewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                loadingDialog.dismiss()
            }
        }
    }

    private fun displayCategories() {
        categoriesViewModel.categoryList.observe(viewLifecycleOwner) { categoryList ->
            loadingDialog.dismiss()
            if (categoryList.isNullOrEmpty()) {
                // Show empty state
                binding.recyclerPharmacyDepartments.visibility = View.GONE
                // You might want to show an empty state view here
            } else {
                setupPharmacyDepartmentsRecycler(categoryList)
            }
        }
    }

    private fun setupPharmacyDepartmentsRecycler(categoryList: List<dev.anonymous.eilaji.network.CategoryDto>) {
        with(binding.recyclerPharmacyDepartments) {
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(activity, 2)
            adapter = CategoryAdapter(categoryList) { categoryId, categoryTitle ->
                navToSubCategories(categoryId, categoryTitle)
            }
        }
    }

    private fun navToSubCategories(categoryId: String, categoryTitle: String) {
        activity?.window?.apply {
            enterTransition = android.transition.Fade()
        }
        val intent = Intent(requireContext(), AlternativesActivity::class.java)
        intent.putExtra("fragmentType", "subCategories")
        intent.putExtra("categoryId", categoryId)
        intent.putExtra("categoryTitle", categoryTitle)
        startActivity(intent)
    }

    private fun fetchCategories() {
        categoriesViewModel.loadCategories()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog.dismiss()
    }
}
