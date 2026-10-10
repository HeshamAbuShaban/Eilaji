package dev.anonymous.eilaji.ui.other.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentOrdersBinding
import dev.anonymous.eilaji.databinding.ItemOrderBinding
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.OrderDto
import dev.anonymous.eilaji.utils.Money
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OrdersFragment : Fragment() {
    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try { binding.toolbarOrders.title = getString(R.string.my_orders) } catch (_: Exception) {}
        binding.recyclerOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.btnRetryOrders.setOnClickListener { load() }
        binding.btnRetryErrorOrders.setOnClickListener { load() }
        load()
    }

    private fun load() {
        if (!isAdded || _binding == null) return
        binding.shimmerOrders.visibility = View.VISIBLE
        binding.shimmerOrders.startShimmer()
        binding.emptyOrders.visibility = View.GONE
        binding.errorOrders.visibility = View.GONE
        NetworkModule.provideApiService(requireContext()).getOrders().enqueue(object : Callback<ApiResponse<List<OrderDto>>> {
            override fun onResponse(call: Call<ApiResponse<List<OrderDto>>>, response: Response<ApiResponse<List<OrderDto>>>) {
                if (!isAdded || _binding == null || call.isCanceled) return
                binding.shimmerOrders.stopShimmer()
                binding.shimmerOrders.visibility = View.GONE
                binding.errorOrders.visibility = View.GONE
                val items = if (response.isSuccessful && response.body()?.success == true) response.body()?.data ?: emptyList() else emptyList()
                if (items.isEmpty()) {
                    binding.emptyOrders.visibility = View.VISIBLE
                    binding.recyclerOrders.visibility = View.GONE
                } else {
                    binding.emptyOrders.visibility = View.GONE
                    binding.recyclerOrders.visibility = View.VISIBLE
                    binding.recyclerOrders.adapter = OrdersAdapter(items) { order ->
                        // Only an order that is still moving needs the live map.
                        try {
                            val b = Bundle().apply { putString("orderId", order.id) }
                            findNavController().navigate(R.id.navigation_order_tracking, b)
                        } catch (_: Exception) {}
                    }
                    binding.recyclerOrders.scheduleLayoutAnimation()
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<OrderDto>>>, t: Throwable) {
                if (!isAdded || _binding == null || call.isCanceled) return
                binding.shimmerOrders.stopShimmer()
                binding.shimmerOrders.visibility = View.GONE
                binding.emptyOrders.visibility = View.GONE
                binding.errorOrders.visibility = View.VISIBLE
                try {
                    Snackbar.make(binding.root, getString(R.string.network_error), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.retry)) { load() }
                        .show()
                } catch (_: Exception) {}
            }
        })
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    /**
     * Each row expands in place: the state rail, the ETA and the pickup code appear under it,
     * so history and live tracking live in one screen instead of two.
     */
    class OrdersAdapter(
        private val items: List<OrderDto>,
        private val onClick: ((OrderDto) -> Unit)? = null
    ) : RecyclerView.Adapter<OrdersAdapter.Holder>() {

        private val expanded = mutableSetOf<String>()

        override fun onCreateViewHolder(p: ViewGroup, v: Int): Holder {
            val b = ItemOrderBinding.inflate(LayoutInflater.from(p.context), p, false)
            return Holder(b, onClick, expanded)
        }

        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: Holder, pos: Int) = h.bind(items[pos])

        class Holder(
            private val b: ItemOrderBinding,
            private val onClick: ((OrderDto) -> Unit)?,
            private val expanded: MutableSet<String>
        ) : RecyclerView.ViewHolder(b.root) {

            fun bind(o: OrderDto) {
                val context = b.root.context
                b.tvOrderId.text = context.getString(R.string.order_id_prefix, o.id.take(8).uppercase())
                b.tvOrderStatus.text = o.status
                b.tvOrderTotal.text = Money.format(o.totalAmount, context)
                b.tvOrderMeta.text = listOfNotNull(o.pharmacyName, o.createdAt.take(10)).joinToString(" • ")
                val dot = when (o.status.uppercase()) {
                    "DELIVERED" -> android.graphics.Color.parseColor("#4CAF50")
                    "CANCELLED" -> android.graphics.Color.parseColor("#F44336")
                    "SHIPPED" -> android.graphics.Color.parseColor("#5E70FF")
                    else -> android.graphics.Color.parseColor("#FF8705")
                }
                try { b.viewStatusDot.background.setColorFilter(dot, android.graphics.PorterDuff.Mode.SRC_IN) } catch (_: Exception) {}

                // detail block
                val isOpen = o.id in expanded
                b.orderDetail.visibility = if (isOpen) android.view.View.VISIBLE else android.view.View.GONE
                b.root.contentDescription = context.getString(
                    if (isOpen) R.string.collapse_order else R.string.expand_order
                )
                if (isOpen) {
                    b.orderStepper.state = dev.anonymous.eilaji.ui.components.StepStepper.fromApi(o.status)
                    val eta = o.etaMinutes
                    b.tvOrderEta.text = if (eta != null && o.status.uppercase() !in listOf("DELIVERED", "CANCELLED")) {
                        context.getString(R.string.arriving_in_minutes, eta)
                    } else {
                        context.getString(R.string.order_status_line, o.status)
                    }
                    val handoff = o.handoffCode
                    if (!handoff.isNullOrBlank() && o.status.uppercase() !in listOf("DELIVERED", "CANCELLED")) {
                        b.tvOrderHandoff.visibility = android.view.View.VISIBLE
                        b.tvOrderHandoff.text = context.getString(R.string.pickup_code, handoff)
                    } else {
                        b.tvOrderHandoff.visibility = android.view.View.GONE
                    }
                    b.buTrackOnMap.setOnClickListener { try { onClick?.invoke(o) } catch (_: Exception) {} }
                } else {
                    b.buTrackOnMap.setOnClickListener(null)
                }

                // tapping the card toggles; the button is the map entry point
                b.root.setOnClickListener {
                    try { b.root.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY) } catch (_: Exception) {}
                    val nowOpen = o.id !in expanded
                    if (nowOpen) expanded.add(o.id) else expanded.remove(o.id)
                    // Toggle in place: no adapter round trip, no animation flicker.
                    b.orderDetail.visibility = if (nowOpen) android.view.View.VISIBLE else android.view.View.GONE
                    b.root.contentDescription = context.getString(
                        if (nowOpen) R.string.collapse_order else R.string.expand_order
                    )
                    if (nowOpen) {
                        b.orderStepper.state = dev.anonymous.eilaji.ui.components.StepStepper.fromApi(o.status)
                        val eta = o.etaMinutes
                        b.tvOrderEta.text = if (eta != null && o.status.uppercase() !in listOf("DELIVERED", "CANCELLED")) {
                            context.getString(R.string.arriving_in_minutes, eta)
                        } else {
                            context.getString(R.string.order_status_line, o.status)
                        }
                        val handoff = o.handoffCode
                        if (!handoff.isNullOrBlank() && o.status.uppercase() !in listOf("DELIVERED", "CANCELLED")) {
                            b.tvOrderHandoff.visibility = android.view.View.VISIBLE
                            b.tvOrderHandoff.text = context.getString(R.string.pickup_code, handoff)
                        } else {
                            b.tvOrderHandoff.visibility = android.view.View.GONE
                        }
                    }
                }
            }
        }
    }
}
