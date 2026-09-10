package dev.anonymous.eilaji.ui.other.medicine

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.data.repository.CartItem
import dev.anonymous.eilaji.data.repository.CartRepository
import dev.anonymous.eilaji.databinding.FragmentMedicineBinding
import dev.anonymous.eilaji.favorite_system.repository.FavoriteSyncRepository
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.ui.base.BaseActivity
import dev.anonymous.eilaji.utils.GeneralUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MedicineFragment : Fragment() {
    private lateinit var _binding: FragmentMedicineBinding
    private val binding get() = _binding
    private lateinit var medicineViewModel: MedicineViewModel
    private var medicineId: String? = null
    private var currentDto: MedicineDto? = null
    private var qty = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        medicineId = arguments?.getString("medicineId") ?: activity?.intent?.getStringExtra("medicineId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicineBinding.inflate(inflater, container, false)
        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        medicineViewModel.init(requireContext())
        if (medicineId != null) loadDetails(medicineId!!)
        else {
            binding.toolbarMedicine.title = "Medicines"
            medicineViewModel.loadMedicines()
            medicineViewModel.medicines.observe(viewLifecycleOwner) { list ->
                if (!list.isNullOrEmpty() && currentDto == null) loadDetails(list[0].id)
            }
        }
        setupFavorite()
        setupQuantity()
        binding.cardCart?.setOnClickListener { addToCart() }
        binding.cardCheckout?.setOnClickListener { goCheckout() }
    }

    private fun loadDetails(id: String) {
        NetworkModule.provideApiService(requireContext()).getMedicine(id).enqueue(object : Callback<ApiResponse<MedicineDto>> {
            override fun onResponse(call: Call<ApiResponse<MedicineDto>>, response: Response<ApiResponse<MedicineDto>>) {
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    bindDetails(response.body()!!.data!!)
                } else {
                    Toast.makeText(requireContext(), response.body()?.error ?: "Failed to load", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<MedicineDto>>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun bindDetails(dto: MedicineDto) {
        currentDto = dto
        GeneralUtils.getInstance().loadImage(dto.imageUrl ?: "").into(binding.ivMedicineDetail as android.widget.ImageView)
        binding.textView2?.text = dto.titleEn.ifBlank { dto.titleAr }
        binding.textView?.text = "${dto.price ?: 0.0}$"
        binding.toolbarMedicine?.title = dto.titleEn.ifBlank { dto.titleAr }
        binding.tvMedicineManufacturer?.text = dto.manufacturer ?: ""
        binding.tvMedicineManufacturer?.visibility = if (dto.manufacturer.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvMedicineDescription?.text = dto.descriptionEn?.ifBlank { dto.descriptionAr } ?: dto.descriptionAr ?: ""
        binding.tvMedicinePrescription?.visibility = if (dto.requiresPrescription) View.VISIBLE else View.GONE
        updateFavoriteIcon()
    }

    private fun setupFavorite() {
        binding.fabAddToFavorite.setOnClickListener {
            val dto = currentDto ?: return@setOnClickListener
            val repo = FavoriteSyncRepository(requireContext())
            val isFav = repo.isFavoriteLocal(dto.id, null)
            if (isFav) {
                val ent = dev.anonymous.eilaji.favorite_system.database.db.FavoriteDatabase.getDatabase(requireContext()).favoriteDao().findByMedicineId(dto.id)
                if (ent != null) repo.syncDelete(ent.id) else repo.syncDelete(dto.id)
                binding.fabAddToFavorite.setImageResource(R.drawable.ic_favorite_border)
                Toast.makeText(requireContext(), "Removed from favorites", Toast.LENGTH_SHORT).show()
            } else {
                repo.syncCreateLocalFirst(dto.id, null)
                binding.fabAddToFavorite.setImageResource(R.drawable.ic_favorite)
                Toast.makeText(requireContext(), "Added to favorites", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateFavoriteIcon() {
        val dto = currentDto ?: return
        try {
            val isFav = FavoriteSyncRepository(requireContext()).isFavoriteLocal(dto.id, null)
            binding.fabAddToFavorite.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        } catch (_: Exception) {}
    }

    private fun setupQuantity() {
        binding.buIncrement.setOnClickListener { qty++; binding.tvNumMedicines.text = qty.toString() }
        binding.buDecrease.setOnClickListener { if (qty > 1) qty--; binding.tvNumMedicines.text = qty.toString() }
    }

    private fun addToCart() {
        val dto = currentDto ?: return
        CartRepository.getInstance(requireContext()).addItem(CartItem(dto.id, dto.titleEn.ifBlank { dto.titleAr }, dto.price ?: 0.0, dto.imageUrl, qty))
        (activity as? BaseActivity)?.refreshCartBadge()
        Snackbar.make(binding.root, "Added to cart", Snackbar.LENGTH_LONG).setAction("Checkout") { goCheckout() }.show()
    }

    private fun goCheckout() {
        val dto = currentDto
        val bundle = Bundle().apply {
            putString("medicineId", dto?.id)
            putInt("quantity", qty)
            putString("pharmacyId", null)
            putString("pharmacyName", null)
        }
        try { findNavController().navigate(R.id.navigation_checkout, bundle) } catch (_: Exception) {}
    }
}
