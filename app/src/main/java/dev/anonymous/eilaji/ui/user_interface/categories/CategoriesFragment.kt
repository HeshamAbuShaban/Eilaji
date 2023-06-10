package dev.anonymous.eilaji.ui.user_interface.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import dev.anonymous.eilaji.adapters.PharmacyDepartmentsAdapter
import dev.anonymous.eilaji.databinding.FragmentCategoriesBinding
import dev.anonymous.eilaji.utils.DummyData

class CategoriesFragment : Fragment() {
    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPharmacyDepartmentsRecycler()
    }

    private fun setupPharmacyDepartmentsRecycler() {
        with(binding.recyclerPharmacyDepartments) {
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(activity, 2)
            adapter = PharmacyDepartmentsAdapter(DummyData.listPharmacyDepartmentsModels)
        }
    }
}