package dev.anonymous.eilaji.ui.other.orders

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dev.anonymous.eilaji.databinding.FragmentOrderTrackingBinding
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.OrderDto
import dev.anonymous.eilaji.storage.AppSharedPreferences
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Live delivery tracking. Phase A: simulated courier interpolating
 * pharmacy -> dropoff, with bearing rotation, polyline route, ETA panel
 * and status stepper. Polls order status so the pharmacy dashboard
 * advancing states reflects here. Backend courier fields (courierLat/
 * courierLng/etaMinutes) override the simulation when present.
 */
class OrderTrackingFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentOrderTrackingBinding? = null
    private val binding get() = _binding!!
    private var orderId: String? = null
    private var map: GoogleMap? = null
    private var courierMarker: Marker? = null
    private val handler = Handler(Looper.getMainLooper())
    private var simTick: Runnable? = null
    private var pollTick: Runnable? = null
    private var progress = 0.06f
    private var from: LatLng = LatLng(31.4495, 34.3925)
    private var to: LatLng = LatLng(31.5, 34.46)
    private var totalKm = 1.0
    private var status: String = "SHIPPED"
    private var handoff: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderId = arguments?.getString("orderId") ?: activity?.intent?.getStringExtra("orderId")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrderTrackingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val f = childFragmentManager.findFragmentById(dev.anonymous.eilaji.R.id.trackingMap) as? SupportMapFragment
        f?.getMapAsync(this)
        loadOrder()
    }

    private fun loadOrder() {
        val id = orderId ?: return
        try {
            NetworkModule.provideApiService(requireContext()).getOrder(id).enqueue(object : Callback<ApiResponse<OrderDto>> {
                override fun onResponse(call: Call<ApiResponse<OrderDto>>, response: Response<ApiResponse<OrderDto>>) {
                    if (!isAdded || _binding == null || call.isCanceled) return
                    val o = response.body()?.data ?: return
                    status = o.status.uppercase()
                    handoff = o.handoffCode
                    // Backend live position wins over simulation when present
                    if (o.courierLat != null && o.courierLng != null) {
                        progress = -1f // marker driven by backend
                        placeBackendCourier(o.courierLat, o.courierLng, o.etaMinutes)
                    }
                    renderSheet(o)
                    startPolling()
                }
                override fun onFailure(call: Call<ApiResponse<OrderDto>>, t: Throwable) { startPolling() }
            })
        } catch (_: Exception) {}
    }

    private fun startPolling() {
        pollTick?.let { handler.removeCallbacks(it) }
        pollTick = object : Runnable {
            override fun run() {
                if (!isAdded || _binding == null) return
                val id = orderId ?: return
                try {
                    NetworkModule.provideApiService(requireContext()).getOrder(id).enqueue(object : Callback<ApiResponse<OrderDto>> {
                        override fun onResponse(call: Call<ApiResponse<OrderDto>>, r: Response<ApiResponse<OrderDto>>) {
                            val o = r.body()?.data
                            if (o != null && isAdded && _binding != null) {
                                status = o.status.uppercase()
                                handoff = o.handoffCode ?: handoff
                                if (o.courierLat != null && o.courierLng != null) {
                                    progress = -1f
                                    placeBackendCourier(o.courierLat, o.courierLng, o.etaMinutes)
                                }
                                renderSheet(o)
                                if (status == "DELIVERED" || status == "CANCELLED") stopSim()
                            }
                        }
                        override fun onFailure(call: Call<ApiResponse<OrderDto>>, t: Throwable) {}
                    })
                } catch (_: Exception) {}
                handler.postDelayed(this, 15000)
            }
        }
        handler.postDelayed(pollTick!!, 15000)
    }

    override fun onMapReady(gMap: GoogleMap) {
        if (!isAdded || _binding == null) return
        map = gMap
        try {
            val prefs = AppSharedPreferences.getInstance(requireContext())
            val dLat = prefs.getString("delivery_lat", null)?.toDoubleOrNull()
            val dLng = prefs.getString("delivery_lng", null)?.toDoubleOrNull()
            if (dLat != null && dLng != null) to = LatLng(dLat, dLng)
        } catch (_: Exception) {}
        drawRoute()
        startSim()
    }

    private fun drawRoute() {
        val m = map ?: return
        try {
            m.clear()
            // Pickup pin
            m.addMarker(MarkerOptions().position(from).title("Pharmacy").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
            // Dropoff pin
            m.addMarker(MarkerOptions().position(to).title("Dropoff").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
            // Route polyline with slight perpendicular bow for road feel
            val pts = bowedPoints(from, to, 24)
            m.addPolyline(PolylineOptions().addAll(pts).width(10f).color(Color.parseColor("#BA324F")).geodesic(true))
            m.addPolyline(PolylineOptions().addAll(pts).width(3f).color(Color.WHITE).geodesic(true))
            totalKm = haversine(from.latitude, from.longitude, to.latitude, to.longitude).coerceAtLeast(0.3)
            val bounds = LatLngBounds.builder().include(from).include(to).build()
            m.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160))
        } catch (_: Exception) {}
    }

    private fun bowedPoints(a: LatLng, b: LatLng, n: Int): List<LatLng> {
        val mx = (a.latitude + b.latitude) / 2
        val my = (a.longitude + b.longitude) / 2
        val dx = b.latitude - a.latitude
        val dy = b.longitude - a.longitude
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-6)
        val bow = 0.06 * len
        val cx = mx - dy / len * bow
        val cy = my + dx / len * bow
        return (0..n).map { i ->
            val t = i.toFloat() / n
            val u = 1 - t
            LatLng(
                (u * u * a.latitude + 2 * u * t * cx + t * t * b.latitude),
                (u * u * a.longitude + 2 * u * t * cy + t * t * b.longitude)
            )
        }
    }

    private fun pointAt(t: Float): LatLng {
        val pts = bowedPoints(from, to, 48)
        val idx = (t.coerceIn(0f, 1f) * (pts.size - 1)).toInt().coerceIn(0, pts.size - 1)
        return pts[idx]
    }

    private fun bearing(a: LatLng, b: LatLng): Float {
        val dLng = b.longitude - a.longitude
        val y = sin(dLng * Math.PI / 180) * cos(b.latitude * Math.PI / 180)
        val x = cos(a.latitude * Math.PI / 180) * sin(b.latitude * Math.PI / 180) -
                sin(a.latitude * Math.PI / 180) * cos(b.latitude * Math.PI / 180) * cos(dLng * Math.PI / 180)
        return ((atan2(y, x) * 180 / Math.PI + 360) % 360).toFloat()
    }

    private fun startSim() {
        simTick?.let { handler.removeCallbacks(it) }
        simTick = object : Runnable {
            override fun run() {
                if (!isAdded || _binding == null) return
                if (progress < 0f) return // backend-driven
                if (status == "DELIVERED" || status == "CANCELLED") return
                progress = (progress + 0.012f).coerceAtMost(1f)
                moveCourier()
                if (progress < 1f) handler.postDelayed(this, 2000) else finishTrip()
            }
        }
        handler.postDelayed(simTick!!, 1500)
    }

    private fun moveCourier() {
        val m = map ?: return
        try {
            val p = pointAt(progress)
            val q = pointAt((progress + 0.02f).coerceAtMost(1f))
            if (courierMarker == null) {
                courierMarker = m.addMarker(
                    MarkerOptions().position(p)
                        .title("Courier")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .flat(true).rotation(bearing(p, q)).anchor(0.5f, 0.5f)
                )
            } else {
                courierMarker?.position = p
                courierMarker?.rotation = bearing(p, q)
            }
            val remainKm = totalKm * (1 - progress)
            val eta = (remainKm / 25.0 * 60).toInt().coerceAtLeast(1)
            renderMotion(remainKm, eta)
        } catch (_: Exception) {}
    }

    private fun placeBackendCourier(lat: Double, lng: Double, eta: Int?) {
        val m = map ?: return
        try {
            val p = LatLng(lat, lng)
            if (courierMarker == null) {
                courierMarker = m.addMarker(MarkerOptions().position(p).title("Courier").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).flat(true).anchor(0.5f, 0.5f))
            } else courierMarker?.position = p
            val remainKm = haversine(p.latitude, p.longitude, to.latitude, to.longitude)
            renderMotion(remainKm, eta ?: (remainKm / 25.0 * 60).toInt().coerceAtLeast(1))
        } catch (_: Exception) {}
    }

    private var composeStatus = androidx.compose.runtime.mutableStateOf("SHIPPED")
    private var composeProgress = androidx.compose.runtime.mutableStateOf(0.1f)
    private var composeBound = false

    private fun bindComposeIsland() {
        try {
            val cv = binding.composeSteps
            cv.setViewCompositionStrategy(androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            cv.setContent {
                androidx.compose.material3.MaterialTheme {
                    TrackingSteps(status = composeStatus.value, progress = composeProgress.value)
                }
            }
            composeBound = true
        } catch (_: Exception) {}
    }

    private fun renderMotion(remainKm: Double, eta: Int) {
        try {
            _binding ?: return
            binding.tvTrackingEta.text = "~$eta min"
            binding.progressRoute.progress = (progress.coerceIn(0f, 1f) * 100).toInt()
            binding.tvTrackingStatus.text = when (status) {
                "PENDING" -> "Confirming with pharmacy"
                "CONFIRMED" -> "Preparing your order"
                "PREPARING" -> "Packing your medicines"
                "SHIPPED" -> "Courier on the way"
                "DELIVERED" -> "Delivered"
                "CANCELLED" -> "Cancelled"
                else -> "On the way"
            }
            if (!composeBound) bindComposeIsland()
            composeStatus.value = status
            composeProgress.value = progress.coerceIn(0f, 1f)
        } catch (_: Exception) {}
    }

    private fun renderSheet(o: OrderDto) {
        try {
            _binding ?: return
            binding.tvTrackingFrom.text = o.pharmacyName ?: "Pharmacy"
            binding.tvTrackingTo.text = o.deliveryAddress ?: "Your address"
            binding.tvHandoffCode.text = if (!o.handoffCode.isNullOrBlank()) "Handoff code: ${o.handoffCode}" else "Handoff code appears here"
            if (progress < 0f) {
                binding.progressRoute.progress = when (status) {
                    "PENDING" -> 8; "CONFIRMED" -> 25; "PREPARING" -> 45
                    "SHIPPED" -> 70; "DELIVERED" -> 100; else -> 50
                }
                binding.tvTrackingStatus.text = status.lowercase().replaceFirstChar { it.uppercase() }
            } else renderMotion(totalKm * (1 - progress), (totalKm * (1 - progress) / 25.0 * 60).toInt().coerceAtLeast(1))
        } catch (_: Exception) {}
    }

    private fun finishTrip() {
        try {
            binding.tvTrackingEta.text = "Arriving"
            binding.progressRoute.progress = 100
        } catch (_: Exception) {}
    }

    private fun stopSim() {
        try { simTick?.let { handler.removeCallbacks(it) } } catch (_: Exception) {}
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    override fun onDestroyView() {
        try { simTick?.let { handler.removeCallbacks(it) } } catch (_: Exception) {}
        try { pollTick?.let { handler.removeCallbacks(it) } } catch (_: Exception) {}
        map = null
        _binding = null
        super.onDestroyView()
    }
}
