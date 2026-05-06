package dev.anonymous.eilaji.ui.other.medicine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.anonymous.eilaji.databinding.FragmentMedicineBinding
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.ui.other.sub_categories.SubCategoriesViewModel

class MedicineFragment : Fragment() {

    private lateinit var _binding: FragmentMedicineBinding
    private val binding get() = _binding

    private lateinit var medicineViewModel: MedicineViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicineBinding.inflate(inflater, container, false)
        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        medicineViewModel.init(requireContext())
        medicineViewModel.loadMedicines()

        setupObservers()
    }

    private fun setupObservers() {
        medicineViewModel.medicines.observe(viewLifecycleOwner) { medicineList ->
            // Update UI with medicines
            // medicineAdapter.submitList(medicineList)
        }

        medicineViewModel.error.observe(viewLifecycleOwner) { errorMsg ->
            errorMsg?.let {
                // Show error message
                // Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
