package dev.anonymous.eilaji.ui.other.sub_categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.adapters.SubCategoriesAdapter
import dev.anonymous.eilaji.databinding.FragmentSubCategoriesBinding
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.utils.DummyData
import dev.anonymous.eilaji.utils.LoadingDialog


class SubCategoriesFragment : Fragment() {
    private var _binding: FragmentSubCategoriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SubCategoriesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubCategoriesBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arguments = arguments
        if (arguments != null) {
            val args = SubCategoriesFragmentArgs.fromBundle(arguments)
            val categoryId = args.categoryId
            val categoryTitle = args.categoryTitle

            binding.includeAppBarLayoutAlternatives.toolbarApp.title = categoryTitle

            setupSubCategoriesRecycler()
//            setupMedicinesAdapter(DummyData.listMedicineModels)

            val loadingDialog = LoadingDialog()
            loadingDialog.show(requireActivity().supportFragmentManager, "")
//                loadingDialog.dismiss();
        }
    }

    private fun setupSubCategoriesRecycler() {
        with(binding.recyclerSubCategories) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = SubCategoriesAdapter(DummyData.listSubCategories)

            // علشان يحذف الومضة تعت العنصر يلي تحدث
            itemAnimator = null
        }
    }

    private fun setupMedicinesAdapter(medicinesList: ArrayList<Medicine>) {
        with(binding.recSubCategoriesMedicines) {
            setHasFixedSize(false)
            adapter = MedicinesAdapter(medicinesList)
        }
    }


//    binding.root.layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT

}