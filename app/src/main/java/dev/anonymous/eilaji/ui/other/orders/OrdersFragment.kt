package dev.anonymous.eilaji.ui.other.orders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentOrdersBinding
import dev.anonymous.eilaji.databinding.ItemOrderBinding
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.OrderDto
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
        load()
    }

    private fun load() {
        if (!isAdded || _binding == null) return
        binding.shimmerOrders.visibility = View.VISIBLE
        binding.shimmerOrders.startShimmer()
        binding.emptyOrders.visibility = View.GONE
        NetworkModule.provideApiService(requireContext()).getOrders().enqueue(object : Callback<ApiResponse<List<OrderDto>>> {
            override fun onResponse(call: Call<ApiResponse<List<OrderDto>>>, response: Response<ApiResponse<List<OrderDto>>>) {
                if (!isAdded || _binding == null || call.isCanceled) return
                binding.shimmerOrders.stopShimmer()
                binding.shimmerOrders.visibility = View.GONE
                val items = if (response.isSuccessful && response.body()?.success == true) response.body()?.data ?: emptyList() else emptyList()
                if (items.isEmpty()) {
                    binding.emptyOrders.visibility = View.VISIBLE
                    binding.recyclerOrders.visibility = View.GONE
                } else {
                    binding.emptyOrders.visibility = View.GONE
                    binding.recyclerOrders.visibility = View.VISIBLE
                    binding.recyclerOrders.adapter = OrdersAdapter(items)
                    binding.recyclerOrders.scheduleLayoutAnimation()
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<OrderDto>>>, t: Throwable) {
                if (!isAdded || _binding == null || call.isCanceled) return
                binding.shimmerOrders.stopShimmer()
                binding.shimmerOrders.visibility = View.GONE
                try { Toast.makeText(requireContext(), "Network error — retry", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                binding.emptyOrders.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }

    class OrdersAdapter(private val items: List<OrderDto>) : RecyclerView.Adapter<OrdersAdapter.Holder>() {
        override fun onCreateViewHolder(p: ViewGroup, v: Int): Holder {
            val b = ItemOrderBinding.inflate(LayoutInflater.from(p.context), p, false)
            return Holder(b)
        }
        override fun getItemCount() = items.size
        override fun onBindViewHolder(h: Holder, pos: Int) = h.bind(items[pos])
        class Holder(private val b: ItemOrderBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(o: OrderDto) {
                b.tvOrderId.text = "#${o.id.take(8).uppercase()}"
                b.tvOrderStatus.text = o.status
                b.tvOrderTotal.text = String.format("%.2f $", o.totalAmount)
                b.tvOrderMeta.text = listOfNotNull(o.pharmacyName, o.createdAt.take(10)).joinToString(" • ")
                val dot = when (o.status.uppercase()) {
                    "DELIVERED" -> android.graphics.Color.parseColor("#4CAF50")
                    "CANCELLED" -> android.graphics.Color.parseColor("#F44336")
                    "SHIPPED" -> android.graphics.Color.parseColor("#5E70FF")
                    else -> android.graphics.Color.parseColor("#FF8705")
                }
                try { b.viewStatusDot.background.setColorFilter(dot, android.graphics.PorterDuff.Mode.SRC_IN) } catch (_: Exception) {}
            }
        }
    }
}
