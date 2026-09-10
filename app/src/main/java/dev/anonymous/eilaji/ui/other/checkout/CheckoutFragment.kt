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
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.data.repository.CartRepository
import dev.anonymous.eilaji.databinding.FragmentCheckoutBinding
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.MedicineDto
import dev.anonymous.eilaji.network.NetworkModule
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
            })
            binding.recyclerOrderItems.adapter = adapter
            updateTotal(adapter.getTotal())
        } else {
            binding.recyclerOrderItems.adapter = CheckoutAdapter(emptyList())
            updateTotal(0.0)
        }
    }

    private fun loadSingleMedicine(id: String) {
        NetworkModule.provideApiService(requireContext()).getMedicine(id).enqueue(object : Callback<ApiResponse<MedicineDto>> {
            override fun onResponse(call: Call<ApiResponse<MedicineDto>>, response: Response<ApiResponse<MedicineDto>>) {
                if (response.isSuccessful && response.body()?.data != null) {
                    currentMedicine = response.body()!!.data
                    val dto = currentMedicine!!
                    val item = CheckoutItem(dto.id, dto.titleEn.ifBlank { dto.titleAr }, dto.price ?: 0.0, dto.imageUrl, quantity)
                    val adapter = CheckoutAdapter(listOf(item))
                    binding.recyclerOrderItems.adapter = adapter
                    updateTotal(adapter.getTotal())
                }
            }
            override fun onFailure(call: Call<ApiResponse<MedicineDto>>, t: Throwable) {}
        })
    }

    private fun updateTotal(total: Double) {
        binding.tvTotalAmount.text = String.format("%.2f $", total)
        binding.tvSubtotal.text = String.format("%.2f $", total)
    }

    private fun observeVm() {
        vm.loading.observe(viewLifecycleOwner) { binding.btnPlaceOrder.isEnabled = it != true; binding.progressPlaceOrder.visibility = if (it == true) View.VISIBLE else View.GONE }
        vm.orderResult.observe(viewLifecycleOwner) { res ->
            res.onSuccess {
                Snackbar.make(binding.root, "Order placed", Snackbar.LENGTH_LONG).show()
                cartRepo.clear()
                (activity as? BaseActivity)?.refreshCartBadge()
                view?.postDelayed({ try { findNavController().popBackStack() } catch (_: Exception) {} }, 1200)
            }
        }
        vm.error.observe(viewLifecycleOwner) { msg -> if (!msg.isNullOrBlank()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show() }
    }

    private fun doPlaceOrder() {
        val prefs = AppSharedPreferences.getInstance(requireContext())
        val address = prefs.getString("delivery_address", null) ?: prefs.getString("address", null) ?: binding.tvAddress.text.toString()
        if (address.isBlank() || address == getString(R.string.add_address)) {
            Snackbar.make(binding.root, "Please add delivery address", Snackbar.LENGTH_SHORT).show()
            return
        }
        val total = binding.tvTotalAmount.text.toString().replace("$","").trim().toDoubleOrNull()
        val prescId = medicineId ?: cartRepo.getAll().firstOrNull()?.medicineId ?: "direct-order"
        val pharmId = pharmacyId ?: cartRepo.getAll().firstOrNull()?.pharmacyId ?: "default-pharmacy"
        vm.placeOrder(requireContext(), prescId, pharmId, total, selectedPayment, address)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

data class CheckoutItem(val id: String, val title: String, val price: Double, val imageUrl: String?, val qty: Int)

class CheckoutAdapter(private val items: List<CheckoutItem>) : androidx.recyclerview.widget.RecyclerView.Adapter<CheckoutAdapter.Holder>() {
    fun getTotal(): Double = items.sumOf { it.price * it.qty }
    override fun onCreateViewHolder(p: ViewGroup, v: Int): Holder {
        val b = dev.anonymous.eilaji.databinding.ItemCheckoutBinding.inflate(LayoutInflater.from(p.context), p, false)
        return Holder(b)
    }
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: Holder, pos: Int) = h.bind(items[pos])
    class Holder(private val b: dev.anonymous.eilaji.databinding.ItemCheckoutBinding) : androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root) {
        fun bind(i: CheckoutItem) {
            b.tvMedicineName.text = i.title
            b.tvMedicinePrice.text = String.format("%.2f $", i.price)
            b.tvQty.text = "x${i.qty}"
            b.tvLineTotal.text = String.format("%.2f $", i.price * i.qty)
            if (!i.imageUrl.isNullOrBlank()) {
                try { dev.anonymous.eilaji.utils.GeneralUtils.getInstance().loadImage(i.imageUrl).into(b.ivMedicine) } catch (_: Exception) {}
            }
        }
    }
}
