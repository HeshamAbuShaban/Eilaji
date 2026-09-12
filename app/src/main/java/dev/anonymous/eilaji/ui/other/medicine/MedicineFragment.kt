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
        setupAvailability()
        setupRating()
        setupAlternatives()
    }

    private fun setupAlternatives() {
        try {
            binding.recyclerAlternatives.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        } catch (_: Exception) {}
        NetworkModule.provideApiService(requireContext()).getMedicines(page = 0, pageSize = 12)
            .enqueue(object : Callback<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>> {
                override fun onResponse(call: Call<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>>) {
                    val items = (response.body()?.data?.items ?: emptyList()).filter { it.id != currentDto?.id }.take(8)
                    if (items.isEmpty()) {
                        try { binding.tvAlternativesTitle.visibility = View.GONE; binding.recyclerAlternatives.visibility = View.GONE } catch (_: Exception) {}
                        return
                    }
                    val ui = ArrayList(items.map { dto ->
                        dev.anonymous.eilaji.models.server.Medicine(dto.id, dto.imageUrl ?: "", dto.titleEn.ifBlank { dto.titleAr }, dto.price ?: 0.0, dto.descriptionEn ?: "", ArrayList(), "", dto.subcategoryNameEn ?: "", false)
                    })
                    binding.recyclerAlternatives.adapter = dev.anonymous.eilaji.adapters.MedicinesAdapter(ui, onItemClick = { med ->
                        val b = Bundle().apply { putString("medicineId", med.id) }
                        try { findNavController().navigate(R.id.navigation_medicine, b) } catch (_: Exception) {
                            loadDetails(med.id)
                        }
                    })
                }
                override fun onFailure(call: Call<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>>, t: Throwable) {
                    try { binding.tvAlternativesTitle.visibility = View.GONE; binding.recyclerAlternatives.visibility = View.GONE } catch (_: Exception) {}
                }
            })
    }

    private fun userLatLng(): Pair<Double, Double> {
        return try {
            val prefs = dev.anonymous.eilaji.storage.AppSharedPreferences.getInstance(requireContext())
            val lat = prefs.getString("delivery_lat", null)?.toDoubleOrNull()
            val lng = prefs.getString("delivery_lng", null)?.toDoubleOrNull()
            if (lat != null && lng != null) lat to lng else 31.5 to 34.46
        } catch (_: Exception) { 31.5 to 34.46 }
    }

    private fun setupAvailability() {
        try {
            binding.recyclerAvailablePharmacies.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        } catch (_: Exception) {}
        val (lat, lng) = userLatLng()
        NetworkModule.provideApiService(requireContext()).getNearbyPharmacies(lat, lng, 600.0)
            .enqueue(object : Callback<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>> {
                override fun onResponse(call: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>, response: Response<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>) {
                    val items = if (response.isSuccessful && response.body()?.success == true) response.body()?.data ?: emptyList() else emptyList()
                    bindAvailability(items)
                }
                override fun onFailure(call: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>, t: Throwable) { bindAvailability(emptyList()) }
            })
    }

    private fun bindAvailability(items: List<dev.anonymous.eilaji.network.PharmacyDto>) {
        try {
            val top = items.take(5)
            if (top.isEmpty()) {
                binding.tvAvailabilityCount.visibility = View.GONE
                binding.recyclerAvailablePharmacies.visibility = View.GONE
                return
            }
            binding.tvAvailabilityCount.visibility = View.VISIBLE
            binding.tvAvailabilityCount.text = "${top.size} nearby"
            val ui = ArrayList(top.map { dto ->
                dev.anonymous.eilaji.models.Pharmacy(uid = dto.id, pharmacy_image_url = dto.imageUrl ?: "", pharmacy_name = dto.name, phone = dto.phone ?: "", address = dto.address, lat = dto.latitude, lng = dto.longitude, token = "", ratingAvg = dto.ratingAvg, totalRatings = dto.totalRatings, isOpen = dto.isOpen, distanceKm = dto.distanceKm)
            })
            binding.recyclerAvailablePharmacies.adapter = dev.anonymous.eilaji.adapters.PharmaciesLocationsAdapter(ui) { model ->
                val intent = android.content.Intent(requireContext(), dev.anonymous.eilaji.ui.other.base.AlternativesActivity::class.java)
                intent.putExtra("fragmentType", dev.anonymous.eilaji.storage.enums.FragmentsKeys.messaging.name)
                intent.putExtra("receiverUid", model.uid)
                intent.putExtra("receiverFullName", model.pharmacy_name)
                intent.putExtra("receiverUrlImage", model.pharmacy_image_url)
                startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    private fun setupRating() {
        binding.buRatePharmacy.setOnClickListener {
            val stars = binding.ratingPharmacy.rating.toInt().coerceIn(1, 5)
            val (lat, lng) = userLatLng()
            NetworkModule.provideApiService(requireContext()).getNearbyPharmacies(lat, lng, 600.0)
                .enqueue(object : Callback<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>> {
                    override fun onResponse(call: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>, response: Response<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>) {
                        val first = response.body()?.data?.firstOrNull()
                        if (first == null) {
                            Toast.makeText(requireContext(), "No pharmacy nearby to rate", Toast.LENGTH_SHORT).show()
                            return
                        }
                        NetworkModule.provideApiService(requireContext())
                            .createRating(dev.anonymous.eilaji.network.CreateRatingRequest(first.id, stars, null))
                            .enqueue(object : Callback<ApiResponse<dev.anonymous.eilaji.network.RatingDto>> {
                                override fun onResponse(call: Call<ApiResponse<dev.anonymous.eilaji.network.RatingDto>>, r2: Response<ApiResponse<dev.anonymous.eilaji.network.RatingDto>>) {
                                    if (r2.isSuccessful && r2.body()?.success == true) {
                                        Snackbar.make(binding.root, getString(R.string.thanks_for_rating), Snackbar.LENGTH_SHORT).show()
                                        setupAvailability()
                                    } else Toast.makeText(requireContext(), r2.body()?.error ?: "Rating failed — login required", Toast.LENGTH_SHORT).show()
                                }
                                override fun onFailure(call: Call<ApiResponse<dev.anonymous.eilaji.network.RatingDto>>, t: Throwable) {
                                    Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }
                    override fun onFailure(call: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>, t: Throwable) {
                        Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show()
                    }
                })
        }
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
        binding.toolbarMedicine?.title = dto.titleEn.ifBlank { dto.titleAr }
        binding.tvMedicineManufacturer?.text = dto.manufacturer ?: ""
        binding.tvMedicineManufacturer?.visibility = if (dto.manufacturer.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvMedicineDescription?.text = dto.descriptionEn?.ifBlank { dto.descriptionAr } ?: dto.descriptionAr ?: ""
        binding.tvMedicinePrescription?.visibility = if (dto.requiresPrescription) View.VISIBLE else View.GONE
        updateTotalLabel()
        updateFavoriteIcon()
        setupAlternatives()
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
        updateTotalLabel()
        binding.buIncrement.setOnClickListener { qty++; binding.tvNumMedicines.text = qty.toString(); updateTotalLabel() }
        binding.buDecrease.setOnClickListener { if (qty > 1) qty--; binding.tvNumMedicines.text = qty.toString(); updateTotalLabel() }
    }

    private fun updateTotalLabel() {
        try {
            val price = currentDto?.price ?: 0.0
            val total = price * qty
            binding.textView?.text = String.format("%.2f $", total)
        } catch (_: Exception) {}
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
