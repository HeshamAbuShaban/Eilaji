package dev.anonymous.eilaji.ui.other.checkout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.data.repository.CartRepository
import dev.anonymous.eilaji.databinding.FragmentCheckoutBinding
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PharmacyDto
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.ui.base.BaseActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckoutFragment : Fragment() {
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!
    private lateinit var vm: CheckoutViewModel
    private var medicineId: String? = null
    private var quantity: Int = 1
    private var pharmacyId: String? = null
    private var pharmacyName: String? = null
    private var currentMedicine: MedicineDto? = null
    private var selectedPayment = "CASH"
    private lateinit var cartRepo: CartRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            medicineId = it.getString("medicineId")
            quantity = it.getInt("quantity", 1).coerceAtLeast(1)
            pharmacyId = it.getString("pharmacyId")
            pharmacyName = it.getString("pharmacyName")
        }
        if (medicineId == null) medicineId = activity?.intent?.getStringExtra("medicineId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        vm = ViewModelProvider(this)[CheckoutViewModel::class.java]
        cartRepo = CartRepository.getInstance(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAddress()
        setupPaymentChips()
        setupList()
        observeVm()
        binding.btnPlaceOrder.setOnClickListener { doPlaceOrder() }
        binding.cardAddress.setOnClickListener {
            try { findNavController().navigate(R.id.navigation_add_address) } catch (_: Exception) {}
        }
        binding.btnEditAddress.setOnClickListener {
            try { findNavController().navigate(R.id.navigation_add_address) } catch (_: Exception) {}
        }
    }

    private fun setupAddress() {
        val prefs = AppSharedPreferences.getInstance(requireContext())
        val addr = prefs.getString("delivery_address", null) ?: prefs.getString("address", null)
        if (!addr.isNullOrBlank()) {
            binding.tvAddress.text = addr
            binding.tvAddressEmpty.visibility = View.GONE
        } else {
            binding.tvAddress.text = getString(R.string.add_address)
            binding.tvAddressEmpty.visibility = View.VISIBLE
            binding.tvAddressEmpty.text = "Tap to add delivery address"
        }
    }

    private fun setupPaymentChips() {
        binding.chipGroupPayment.setOnCheckedStateChangeListener { group, checkedIds ->
            val chipId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(chipId)
            selectedPayment = when (chip?.text?.toString()?.lowercase()) {
                "card" -> "CARD"
                else -> "CASH"
            }
            try { binding.tvCardHint.visibility = if (selectedPayment == "CARD") View.VISIBLE else View.GONE } catch (_: Exception) {}
        }
        binding.chipCash.isChecked = true
    }

    private fun setupList() {
        binding.recyclerOrderItems.layoutManager = LinearLayoutManager(requireContext())
        val cartItems = cartRepo.getAll()
        if (medicineId != null) {
            loadSingleMedicine(medicineId!!)
        } else if (cartItems.isNotEmpty()) {
            val adapter = CheckoutAdapter(cartItems.map {
                CheckoutItem(it.medicineId, it.title, it.price, it.imageUrl, it.quantity)
            }.toMutableList()) { syncLinesToCart() }
            binding.recyclerOrderItems.adapter = adapter
            updateTotal(adapter.getTotal())
            refreshFee(pharmacyId ?: cartItems.firstOrNull()?.pharmacyId)
        } else {
            binding.recyclerOrderItems.adapter = CheckoutAdapter(mutableListOf())
            updateTotal(0.0)
        }
    }

    /** Push line edits back to the persistent cart + badge + totals. */
    private fun syncLinesToCart() {
        try {
            val adapter = binding.recyclerOrderItems.adapter as? CheckoutAdapter ?: return
            val lines = adapter.currentItems()
            if (medicineId != null) {
                quantity = lines.firstOrNull()?.qty?.coerceAtLeast(1) ?: 1
            } else {
                val repo = cartRepo.getAll()
                lines.forEach { line ->
                    repo.find { it.medicineId == line.id }?.let { it.quantity = line.qty }
                }
                // Drop removed lines
                repo.filter { r -> lines.any { it.id == r.medicineId } }.let { kept ->
                    cartRepo.clear()
                    kept.forEach { cartRepo.addItem(it) }
                }
                try { (activity as? BaseActivity)?.refreshCartBadge() } catch (_: Exception) {}
            }
            updateTotal(adapter.getTotal())
        } catch (_: Exception) {}
    }

    private fun loadSingleMedicine(id: String) {
        NetworkModule.provideApiService(requireContext()).getMedicine(id).enqueue(object : Callback<ApiResponse<MedicineDto>> {
            override fun onResponse(call: Call<ApiResponse<MedicineDto>>, response: Response<ApiResponse<MedicineDto>>) {
                if (!isAdded) return
                if (response.isSuccessful && response.body()?.data != null) {
                    currentMedicine = response.body()!!.data
                    val dto = currentMedicine!!
                    val item = CheckoutItem(dto.id, dto.titleEn.ifBlank { dto.titleAr }, dto.price ?: 0.0, dto.imageUrl, quantity)
                    val adapter = CheckoutAdapter(mutableListOf(item)) { syncLinesToCart() }
                    binding.recyclerOrderItems.adapter = adapter
                    updateTotal(adapter.getTotal())
                    refreshFee(pharmacyId)
                }
            }
            override fun onFailure(call: Call<ApiResponse<MedicineDto>>, t: Throwable) {}
        })
    }

    private var currentFee = 0.0
    private var currentMinOrder: Double? = null
    private var currentSubtotal = 0.0

    private fun updateTotal(subtotal: Double) {
        currentSubtotal = subtotal
        val total = subtotal + currentFee
        binding.tvTotalAmount.text = String.format("%.2f $", total)
        binding.tvSubtotal.text = String.format("%.2f $", subtotal)
        binding.tvDeliveryFee.text = String.format("%.2f $", currentFee)
    }

    private fun refreshFee(pharmacyId: String?) {
        if (pharmacyId.isNullOrBlank()) {
            // Dummy distance-based fee until a pharmacy is chosen: base 1.5 + location factor
            currentFee = dummyFee(null)
            currentMinOrder = null
            updateTotal(currentSubtotal)
            return
        }
        NetworkModule.provideApiService(requireContext()).getPharmacy(pharmacyId).enqueue(object : Callback<ApiResponse<PharmacyDto>> {
            override fun onResponse(call: Call<ApiResponse<PharmacyDto>>, response: Response<ApiResponse<PharmacyDto>>) {
                val dto = response.body()?.data
                currentFee = dto?.deliveryFee ?: dummyFee(dto?.distanceKm)
                currentMinOrder = dto?.minOrderAmount
                try {
                    binding.tvDeliveryFeeLabel.text = if (dto?.prepTimeMin != null) "Delivery · ~${dto.prepTimeMin} min" else "Delivery"
                } catch (_: Exception) {}
                updateTotal(currentSubtotal)
            }
            override fun onFailure(call: Call<ApiResponse<PharmacyDto>>, t: Throwable) {
                currentFee = dummyFee(null)
                updateTotal(currentSubtotal)
            }
        })
    }

    /** Logical dummy fee: base 1.5 + 0.30/km, capped at 6.0. */
    private fun dummyFee(distanceKm: Double?): Double {
        val d = distanceKm ?: 3.0
        return (1.5 + 0.30 * d).coerceAtMost(6.0)
    }

    private fun observeVm() {
        vm.loading.observe(viewLifecycleOwner) { binding.btnPlaceOrder.isEnabled = it != true; binding.progressPlaceOrder.visibility = if (it == true) View.VISIBLE else View.GONE }
        vm.orderResult.observe(viewLifecycleOwner) { res ->
            res.onSuccess { order ->
                cartRepo.clear()
                (activity as? BaseActivity)?.refreshCartBadge()
                celebrate(order.id.take(8))
            }
            res.onFailure {
                Snackbar.make(binding.root, it.message ?: "Order failed", Snackbar.LENGTH_LONG).show()
            }
        }
        vm.error.observe(viewLifecycleOwner) { msg -> if (!msg.isNullOrBlank()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }
    }

    private fun doPlaceOrder() {
        if (selectedPayment == "CARD") {
            confirmCardWithBiometrics { ok -> if (ok) submitOrder() }
            return
        }
        submitOrder()
    }

    /** Card paywall: biometric (or device credential) confirm before charging. */
    private fun confirmCardWithBiometrics(done: (Boolean) -> Unit) {
        try {
            val manager = androidx.biometric.BiometricManager.from(requireContext())
            val can = manager.canAuthenticate(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            if (can != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                // No biometrics enrolled: fall back to an explicit confirm dialog
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.confirm_card_payment))
                    .setMessage(getString(R.string.pay_with_fingerprint))
                    .setPositiveButton(android.R.string.ok) { _, _ -> done(true) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> done(false) }
                    .show()
                return
            }
            val executor = androidx.core.content.ContextCompat.getMainExecutor(requireContext())
            val prompt = androidx.biometric.BiometricPrompt(
                this,
                executor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        done(true)
                    }
                    override fun onAuthenticationFailed() {}
                    override fun onAuthenticationError(code: Int, msg: CharSequence) {
                        done(false)
                    }
                }
            )
            val info = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.confirm_card_payment))
                .setSubtitle(getString(R.string.pay_with_fingerprint))
                .setAllowedAuthenticators(
                    androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
            prompt.authenticate(info)
        } catch (_: Exception) {
            submitOrder()
        }
    }

    private fun submitOrder() {
        val prefs = AppSharedPreferences.getInstance(requireContext())
        val address = prefs.getString("delivery_address", null) ?: prefs.getString("address", null) ?: binding.tvAddress.text.toString()
        if (address.isBlank() || address == getString(R.string.add_address)) {
            Snackbar.make(binding.root, "Please add delivery address", Snackbar.LENGTH_SHORT).show()
            return
        }
        currentMinOrder?.let { min ->
            if (currentSubtotal < min) {
                Snackbar.make(binding.root, "Minimum order is ${String.format("%.2f $", min)}", Snackbar.LENGTH_LONG).show()
                return
            }
        }
        val total = currentSubtotal + currentFee
        // Direct OTC order: no prescription. Never send medicine IDs or placeholders as prescriptionId.
        val prescId: String? = null
        val pharmId = pharmacyId ?: cartRepo.getAll().firstOrNull()?.pharmacyId
        if (pharmId.isNullOrBlank()) {
            resolveNearestPharmacy { resolved ->
                if (resolved == null) {
                    Snackbar.make(binding.root, "No pharmacy available right now", Snackbar.LENGTH_SHORT).show()
                } else {
                    vm.placeOrder(requireContext(), prescId, resolved, total, selectedPayment, address)
                }
            }
            return
        }
        vm.placeOrder(requireContext(), prescId, pharmId, total, selectedPayment, address)
    }

    private fun resolveNearestPharmacy(done: (String?) -> Unit) {
        try {
            val prefs = AppSharedPreferences.getInstance(requireContext())
            val lat = prefs.getString("delivery_lat", null)?.toDoubleOrNull() ?: 31.5
            val lng = prefs.getString("delivery_lng", null)?.toDoubleOrNull() ?: 34.46
            NetworkModule.provideApiService(requireContext()).getNearbyPharmacies(lat, lng, 600.0)
                .enqueue(object : retrofit2.Callback<ApiResponse<List<PharmacyDto>>> {
                    override fun onResponse(call: retrofit2.Call<ApiResponse<List<PharmacyDto>>>, response: retrofit2.Response<ApiResponse<List<PharmacyDto>>>) {
                        done(response.body()?.data?.firstOrNull()?.id)
                    }
                    override fun onFailure(call: retrofit2.Call<ApiResponse<List<PharmacyDto>>>, t: Throwable) { done(null) }
                })
        } catch (_: Exception) { done(null) }
    }

    private fun celebrate(orderShortId: String?) {
        try {
            if (!orderShortId.isNullOrBlank()) binding.tvOrderId.text = "Order #${orderShortId.uppercase()}"
            binding.successOverlay.visibility = View.VISIBLE
            binding.successOverlay.alpha = 0f
            binding.successOverlay.scaleX = 0.85f
            binding.successOverlay.scaleY = 0.85f
            binding.successOverlay.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(320)
                .setInterpolator(android.view.animation.OvershootInterpolator(1.4f)).start()
            binding.ivSuccessCheck.scaleX = 0.4f
            binding.ivSuccessCheck.scaleY = 0.4f
            binding.ivSuccessCheck.animate().scaleX(1f).scaleY(1f).setDuration(420)
                .setInterpolator(android.view.animation.OvershootInterpolator(2f)).start()
            binding.confettiView.burst(110)
            binding.root.postDelayed({
                try { if (isAdded) findNavController().popBackStack() } catch (_: Exception) {}
            }, 2200)
        } catch (_: Exception) {
            try { findNavController().popBackStack() } catch (_: Exception) {}
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

data class CheckoutItem(val id: String, val title: String, val price: Double, val imageUrl: String?, var qty: Int)

class CheckoutAdapter(
    private val items: MutableList<CheckoutItem>,
    private val onChanged: (() -> Unit)? = null
) : androidx.recyclerview.widget.RecyclerView.Adapter<CheckoutAdapter.Holder>() {
    fun getTotal(): Double = items.sumOf { it.price * it.qty }
    fun currentItems(): List<CheckoutItem> = items
    override fun onCreateViewHolder(p: ViewGroup, v: Int): Holder {
        val b = dev.anonymous.eilaji.databinding.ItemCheckoutBinding.inflate(LayoutInflater.from(p.context), p, false)
        return Holder(b,
            onQty = { item, delta ->
                item.qty = (item.qty + delta).coerceAtLeast(1)
                notifyItemChanged(items.indexOf(item))
                onChanged?.invoke()
            },
            onRemove = { item ->
                val pos = items.indexOf(item)
                if (pos != -1) {
                    items.removeAt(pos)
                    notifyItemRemoved(pos)
                    onChanged?.invoke()
                }
            })
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: Holder, pos: Int) = h.bind(items[pos])
    class Holder(
        private val b: dev.anonymous.eilaji.databinding.ItemCheckoutBinding,
        private val onQty: (CheckoutItem, Int) -> Unit,
        private val onRemove: (CheckoutItem) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {
        private var current: CheckoutItem? = null
        init {
            b.buQtyPlus.setOnClickListener { current?.let { onQty(it, 1) } }
            b.buQtyMinus.setOnClickListener { current?.let { onQty(it, -1) } }
            b.buRemoveItem.setOnClickListener { current?.let { onRemove(it) } }
        }
        fun bind(i: CheckoutItem) {
            current = i
            b.tvMedicineName.text = i.title
            b.tvMedicinePrice.text = String.format("%.2f $", i.price)
            b.tvQty.text = "x${i.qty}"
            b.tvLineTotal.text = String.format("%.2f $", i.price * i.qty)
            if (!i.imageUrl.isNullOrBlank()) {
                try { dev.anonymous.eilaji.utils.GeneralUtils.getInstance().loadImage(i.imageUrl).into(b.ivMedicine) } catch (_: Exception) {}
            } else {
                try { b.ivMedicine.setImageResource(dev.anonymous.eilaji.R.drawable.temp_medicine_1) } catch (_: Exception) {}
            }
        }
    }
}
