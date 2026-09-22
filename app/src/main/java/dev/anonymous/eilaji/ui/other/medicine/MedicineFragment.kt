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
    private var _binding: FragmentMedicineBinding? = null
    private val binding get() = _binding!!
    private val bindingOrNull get() = _binding
    private lateinit var medicineViewModel: MedicineViewModel
    private var medicineId: String? = null
    private var currentDto: MedicineDto? = null
    private var qty = 1
    private var detailsCall: Call<ApiResponse<MedicineDto>>? = null
    private var extrasCall: Call<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>>? = null
    private var nearbyCall: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>? = null

    private var sharedTransitionName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        medicineId = arguments?.getString("medicineId") ?: activity?.intent?.getStringExtra("medicineId")
        sharedTransitionName = arguments?.getString("sharedTransitionName")
            ?: activity?.intent?.getStringExtra("sharedTransitionName")
            ?: medicineId?.let { "medicine_image_$it" }
        try { postponeEnterTransition(400, java.util.concurrent.TimeUnit.MILLISECONDS) } catch (_: Exception) {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMedicineBinding.inflate(inflater, container, false)
        medicineViewModel = ViewModelProvider(this)[MedicineViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val root = requireActivity().findViewById<android.view.ViewGroup>(android.R.id.content)
            if (root != null) dev.anonymous.eilaji.utils.Glass.frost(activity, binding.blurMedicineBar, root)
        } catch (_: Exception) {}
        try { medicineViewModel.init(requireContext()) } catch (_: Exception) { return }
        if (medicineId != null) loadDetails(medicineId!!)
        else {
            try { binding.toolbarMedicine?.title = "Medicines" } catch (_: Exception) {}
            medicineViewModel.loadMedicines()
            medicineViewModel.medicines.observe(viewLifecycleOwner) { list ->
                if (!isAdded || _binding == null) return@observe
                if (!list.isNullOrEmpty() && currentDto == null) loadDetails(list[0].id)
            }
        }
        setupFavorite()
        setupQuantity()
        setupToolbar()
        try { binding.cardCart?.setOnClickListener { addToCart() } } catch (_: Exception) {}
        try { binding.cardCheckout?.setOnClickListener { goCheckout() } } catch (_: Exception) {}
        setupAvailability()
        setupRating()
        setupAlternatives()
    }

    private fun setupToolbar() {
        try {
            bindingOrNull?.toolbarMedicine?.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.share_menu_item) {
                    shareMedicine()
                    true
                } else false
            }
        } catch (_: Exception) {}
    }

    private fun shareMedicine() {
        val dto = currentDto ?: return
        if (!isAdded) return
        try {
            val text = "${dto.titleEn.ifBlank { dto.titleAr }} — ${dto.price ?: 0.0}$" +
                (if (!dto.descriptionEn.isNullOrBlank()) "\n${dto.descriptionEn}" else "") +
                "\nvia Eilaji"
            val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_SUBJECT, dto.titleEn.ifBlank { dto.titleAr })
                putExtra(android.content.Intent.EXTRA_TEXT, text)
            }
            startActivity(android.content.Intent.createChooser(share, getString(R.string.share)))
        } catch (_: Exception) {}
    }

    override fun onDestroyView() {
        try { detailsCall?.cancel() } catch (_: Exception) {}
        try { extrasCall?.cancel() } catch (_: Exception) {}
        try { nearbyCall?.cancel() } catch (_: Exception) {}
        _binding = null
        super.onDestroyView()
    }

    private fun setupAlternatives() {
        if (!isAdded || _binding == null) return
        try {
            binding.recyclerAlternatives?.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        } catch (_: Exception) {}
        try { extrasCall?.cancel() } catch (_: Exception) {}
        val call = try { NetworkModule.provideApiService(requireContext()).getMedicines(page = 0, pageSize = 12) } catch (_: Exception) { return }
        extrasCall = call
        call.enqueue(object : Callback<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>> {
                override fun onResponse(call: Call<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>>, response: Response<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>>) {
                    if (!isAdded || _binding == null || call.isCanceled) return
                    val items = (response.body()?.data?.items ?: emptyList()).filter { it.id != currentDto?.id }.take(8)
                    if (items.isEmpty()) {
                        try { bindingOrNull?.tvAlternativesTitle?.visibility = View.GONE; bindingOrNull?.recyclerAlternatives?.visibility = View.GONE } catch (_: Exception) {}
                        return
                    }
                    val ui = ArrayList(items.map { dto ->
                        dev.anonymous.eilaji.models.server.Medicine(dto.id, dto.imageUrl ?: "", dto.titleEn.ifBlank { dto.titleAr }, dto.price ?: 0.0, dto.descriptionEn ?: "", ArrayList(), "", dto.subcategoryNameEn ?: "", false)
                    })
                    try {
                    bindingOrNull?.recyclerAlternatives?.adapter = dev.anonymous.eilaji.adapters.MedicinesAdapter(ui, onItemClick = { med, _ ->
                        val b = Bundle().apply { putString("medicineId", med.id) }
                        try { findNavController().navigate(R.id.navigation_medicine, b) } catch (_: Exception) {
                            try { medicineId = med.id; loadDetails(med.id) } catch (_: Exception) {}
                        }
                    })
                    } catch (_: Exception) {}
                }
                override fun onFailure(call: Call<ApiResponse<dev.anonymous.eilaji.network.PaginatedResult<MedicineDto>>>, t: Throwable) {
                    if (!isAdded || _binding == null || call.isCanceled) return
                    try { bindingOrNull?.tvAlternativesTitle?.visibility = View.GONE; bindingOrNull?.recyclerAlternatives?.visibility = View.GONE } catch (_: Exception) {}
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
        if (!isAdded || _binding == null) return
        try {
            binding.recyclerAvailablePharmacies?.layoutManager =
                androidx.recyclerview.widget.LinearLayoutManager(requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false)
        } catch (_: Exception) {}
        val (lat, lng) = userLatLng()
        try { nearbyCall?.cancel() } catch (_: Exception) {}
        val call = try { NetworkModule.provideApiService(requireContext()).getNearbyPharmacies(lat, lng, 600.0) } catch (_: Exception) { return }
        nearbyCall = call
        call.enqueue(object : Callback<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>> {
                override fun onResponse(call: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>, response: Response<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>) {
                    if (!isAdded || _binding == null || call.isCanceled) return
                    val items = if (response.isSuccessful && response.body()?.success == true) response.body()?.data ?: emptyList() else emptyList()
                    bindAvailability(items)
                }
                override fun onFailure(call: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>, t: Throwable) {
                    if (!isAdded || _binding == null || call.isCanceled) return
                    bindAvailability(emptyList())
                }
            })
    }

    private fun bindAvailability(items: List<dev.anonymous.eilaji.network.PharmacyDto>) {
        try {
            val top = items.take(5)
            if (top.isEmpty()) {
                binding.tvAvailabilityCount?.visibility = View.GONE
                binding.recyclerAvailablePharmacies?.visibility = View.GONE
                return
            }
            binding.tvAvailabilityCount?.visibility = View.VISIBLE
            binding.tvAvailabilityCount?.text = "${top.size} nearby"
            try {
                val best = top.maxByOrNull { it.ratingAvg }
                bindingOrNull?.tvPharmacyRatingAvg?.text = if (best != null && best.totalRatings > 0) {
                    String.format("%.1f ★ (%d)", best.ratingAvg, best.totalRatings)
                } else ""
            } catch (_: Exception) {}
            val ui = ArrayList(top.map { dto ->
                dev.anonymous.eilaji.models.Pharmacy(uid = dto.id, pharmacy_image_url = dto.imageUrl ?: "", pharmacy_name = dto.name, phone = dto.phone ?: "", address = dto.address, lat = dto.latitude, lng = dto.longitude, token = "", ratingAvg = dto.ratingAvg, totalRatings = dto.totalRatings, isOpen = dto.isOpen, distanceKm = dto.distanceKm)
            })
            binding.recyclerAvailablePharmacies?.adapter = dev.anonymous.eilaji.adapters.PharmaciesLocationsAdapter(ui) { model ->
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
        try {
            bindingOrNull?.ratingPharmacy?.setOnRatingBarChangeListener { bar, _, _ ->
                try { dev.anonymous.eilaji.utils.SpringFx.pop(bar) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        try {
            bindingOrNull?.buRatePharmacy?.setOnClickListener {
                if (!isAdded || _binding == null) return@setOnClickListener
                val stars = (bindingOrNull?.ratingPharmacy?.rating ?: 4f).toInt().coerceIn(1, 5)
                val ctx = try { requireContext() } catch (_: Exception) { return@setOnClickListener }
                val (lat, lng) = userLatLng()
                try {
                    NetworkModule.provideApiService(ctx).getNearbyPharmacies(lat, lng, 600.0)
                        .enqueue(object : Callback<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>> {
                            override fun onResponse(call: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>, response: Response<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>) {
                                if (!isAdded || _binding == null || call.isCanceled) return
                                val first = response.body()?.data?.firstOrNull()
                                if (first == null) {
                                    try { Toast.makeText(ctx, "No pharmacy nearby to rate", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                                    return
                                }
                                try {
                                    NetworkModule.provideApiService(ctx)
                                        .createRating(dev.anonymous.eilaji.network.CreateRatingRequest(first.id, stars, null))
                                        .enqueue(object : Callback<ApiResponse<dev.anonymous.eilaji.network.RatingDto>> {
                                            override fun onResponse(call: Call<ApiResponse<dev.anonymous.eilaji.network.RatingDto>>, r2: Response<ApiResponse<dev.anonymous.eilaji.network.RatingDto>>) {
                                                if (!isAdded || _binding == null || call.isCanceled) return
                                                if (r2.isSuccessful && r2.body()?.success == true) {
                                                    try { bindingOrNull?.let { Snackbar.make(it.root, getString(R.string.thanks_for_rating), Snackbar.LENGTH_SHORT).show() } } catch (_: Exception) {}
                                                    setupAvailability()
                                                } else try { Toast.makeText(ctx, r2.body()?.error ?: "Rating failed — login required", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                                            }
                                            override fun onFailure(call: Call<ApiResponse<dev.anonymous.eilaji.network.RatingDto>>, t: Throwable) {
                                                if (!isAdded || _binding == null || call.isCanceled) return
                                                try { Toast.makeText(ctx, "Network error", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                                            }
                                        })
                                } catch (_: Exception) {}
                            }
                            override fun onFailure(call: Call<ApiResponse<List<dev.anonymous.eilaji.network.PharmacyDto>>>, t: Throwable) {
                                if (!isAdded || _binding == null || call.isCanceled) return
                                try { Toast.makeText(ctx, "Network error", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                            }
                        })
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun loadDetails(id: String) {
        if (!isAdded || _binding == null) return
        try { detailsCall?.cancel() } catch (_: Exception) {}
        val call = try { NetworkModule.provideApiService(requireContext()).getMedicine(id) } catch (_: Exception) { return }
        detailsCall = call
        call.enqueue(object : Callback<ApiResponse<MedicineDto>> {
            override fun onResponse(call: Call<ApiResponse<MedicineDto>>, response: Response<ApiResponse<MedicineDto>>) {
                if (!isAdded || _binding == null || call.isCanceled) return
                if (response.isSuccessful && response.body()?.success == true && response.body()?.data != null) {
                    try { bindDetails(response.body()!!.data!!) } catch (_: Exception) {}
                } else {
                    try { Toast.makeText(requireContext(), response.body()?.error ?: "Failed to load", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                }
            }
            override fun onFailure(call: Call<ApiResponse<MedicineDto>>, t: Throwable) {
                if (!isAdded || _binding == null || call.isCanceled) return
                try { Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
            }
        })
    }

    private fun bindDetails(dto: MedicineDto) {
        if (!isAdded || _binding == null) { currentDto = dto; return }
        currentDto = dto
        try {
            val iv = bindingOrNull?.ivMedicineDetail ?: return
            try { iv.transitionName = sharedTransitionName ?: "medicine_image_${dto.id}" } catch (_: Exception) {}
            GeneralUtils.getInstance().loadImage(dto.imageUrl ?: "")
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(e: com.bumptech.glide.load.engine.GlideException?, model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>, isFirstResource: Boolean): Boolean {
                        try { startPostponedEnterTransition() } catch (_: Exception) {}
                        return false
                    }
                    override fun onResourceReady(resource: android.graphics.drawable.Drawable, model: Any, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, dataSource: com.bumptech.glide.load.DataSource, isFirstResource: Boolean): Boolean {
                        try { startPostponedEnterTransition() } catch (_: Exception) {}
                        return false
                    }
                })
                .into(iv as android.widget.ImageView)
        } catch (_: Exception) { try { startPostponedEnterTransition() } catch (_: Exception) {} }
        bindingOrNull?.textView2?.text = dto.titleEn.ifBlank { dto.titleAr }
        bindingOrNull?.toolbarMedicine?.title = dto.titleEn.ifBlank { dto.titleAr }
        try { bindingOrNull?.collapsingToolbar?.title = dto.titleEn.ifBlank { dto.titleAr } } catch (_: Exception) {}
        bindingOrNull?.tvMedicineManufacturer?.text = dto.manufacturer ?: ""
        bindingOrNull?.tvMedicineManufacturer?.visibility = if (dto.manufacturer.isNullOrBlank()) View.GONE else View.VISIBLE
        bindingOrNull?.tvMedicineDescription?.text = dto.descriptionEn?.ifBlank { dto.descriptionAr } ?: dto.descriptionAr ?: ""
        bindingOrNull?.tvMedicinePrescription?.visibility = if (dto.requiresPrescription) View.VISIBLE else View.GONE
        bindFacts(dto)
        updateTotalLabel()
        updateFavoriteIcon()
        setupAlternatives()
    }

    private fun bindFacts(dto: MedicineDto) {
        if (!isAdded || _binding == null) return
        var any = false
        fun bindFact(tv: android.widget.TextView?, label: String, value: String?) {
            if (value.isNullOrBlank()) { try { tv?.visibility = View.GONE } catch (_: Exception) {}; return }
            any = true
            try {
                tv?.visibility = View.VISIBLE
                val sb = android.text.SpannableString(label + "  " + value)
                sb.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 0, label.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                tv?.text = sb
            } catch (_: Exception) {}
        }
        try {
            bindFact(bindingOrNull?.tvFactDosage, "Dosage:", dto.dosage)
            bindFact(bindingOrNull?.tvFactWarnings, "Warnings:", dto.warnings)
            bindFact(bindingOrNull?.tvFactSideEffects, "Side effects:", dto.sideEffects)
            bindFact(bindingOrNull?.tvFactStorage, "Storage:", dto.storageInfo)
            bindingOrNull?.cardMedicineFacts?.visibility = if (any) View.VISIBLE else View.GONE
        } catch (_: Exception) {}
    }

    private fun setupFavorite() {
        try { bindingOrNull?.fabAddToFavorite?.setOnClickListener {
            if (!isAdded || _binding == null) return@setOnClickListener
            val dto = currentDto ?: return@setOnClickListener
            val ctx = try { requireContext() } catch (_: Exception) { return@setOnClickListener }
            val repo = FavoriteSyncRepository(ctx)
            val isFav = try { repo.isFavoriteLocal(dto.id, null) } catch (_: Exception) { return@setOnClickListener }
            if (isFav) {
                try {
                    val ent = dev.anonymous.eilaji.favorite_system.database.db.FavoriteDatabase.getDatabase(ctx).favoriteDao().findByMedicineId(dto.id)
                    if (ent != null) repo.syncDelete(ent.id) else repo.syncDelete(dto.id)
                } catch (_: Exception) {}
                try { bindingOrNull?.fabAddToFavorite?.setImageResource(R.drawable.ic_favorite_border) } catch (_: Exception) {}
                try { Toast.makeText(ctx, "Removed from favorites", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
            } else {
                try { repo.syncCreateLocalFirst(dto.id, null) } catch (_: Exception) {}
                try { bindingOrNull?.fabAddToFavorite?.setImageResource(R.drawable.ic_favorite) } catch (_: Exception) {}
                try { Toast.makeText(ctx, "Added to favorites", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
            }
        } } catch (_: Exception) {}
    }

    private fun updateFavoriteIcon() {
        val dto = currentDto ?: return
        if (!isAdded || _binding == null) return
        try {
            val ctx = requireContext()
            val isFav = FavoriteSyncRepository(ctx).isFavoriteLocal(dto.id, null)
            bindingOrNull?.fabAddToFavorite?.setImageResource(if (isFav) R.drawable.ic_favorite else R.drawable.ic_favorite_border)
        } catch (_: Exception) {}
    }

    private fun setupQuantity() {
        updateTotalLabel()
        try { bindingOrNull?.buIncrement?.setOnClickListener { qty++; bindingOrNull?.tvNumMedicines?.text = qty.toString(); updateTotalLabel() } } catch (_: Exception) {}
        try { bindingOrNull?.buDecrease?.setOnClickListener { if (qty > 1) qty--; bindingOrNull?.tvNumMedicines?.text = qty.toString(); updateTotalLabel() } } catch (_: Exception) {}
    }

    private fun updateTotalLabel() {
        if (!isAdded || _binding == null) return
        try {
            val price = currentDto?.price ?: 0.0
            bindingOrNull?.textView?.text = String.format("%.2f $", price)
            bindingOrNull?.tvBottomTotal?.text = String.format("%.2f $", price * qty)
        } catch (_: Exception) {}
    }

    private fun addToCart() {
        if (!isAdded || _binding == null) return
        val dto = currentDto ?: return
        val ctx = try { requireContext() } catch (_: Exception) { return }
        try { CartRepository.getInstance(ctx).addItem(CartItem(dto.id, dto.titleEn.ifBlank { dto.titleAr }, dto.price ?: 0.0, dto.imageUrl, qty)) } catch (_: Exception) { return }
        try { (activity as? BaseActivity)?.refreshCartBadge() } catch (_: Exception) {}
        try {
            val b = bindingOrNull ?: return
            Snackbar.make(b.root, "Added to cart", Snackbar.LENGTH_LONG).setAction("Checkout") { goCheckout() }.show()
        } catch (_: Exception) {}
    }

    private fun goCheckout() {
        val dto = currentDto
        if (dto?.requiresPrescription == true) {
            val b = bindingOrNull ?: return
            Snackbar.make(b.root, "Prescription required — upload it and the pharmacy will confirm", Snackbar.LENGTH_LONG)
                .setAction("Upload") {
                    try {
                        val intent = android.content.Intent(requireContext(), dev.anonymous.eilaji.ui.other.base.AlternativesActivity::class.java)
                        intent.putExtra("fragmentType", dev.anonymous.eilaji.storage.enums.FragmentsKeys.sendToPharmacy.name)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }.show()
            return
        }
        val bundle = Bundle().apply {
            putString("medicineId", dto?.id)
            putInt("quantity", qty)
            putString("pharmacyId", null)
            putString("pharmacyName", null)
        }
        try { findNavController().navigate(R.id.navigation_checkout, bundle) } catch (_: Exception) {}
    }
}
