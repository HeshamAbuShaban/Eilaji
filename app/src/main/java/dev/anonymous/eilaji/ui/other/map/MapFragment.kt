package dev.anonymous.eilaji.ui.other.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.PharmaciesLocationsAdapter
import dev.anonymous.eilaji.databinding.FragmentMapBinding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.network.PharmacyDto
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment.RequestPermissionsListener

class MapFragment : Fragment(), OnMapReadyCallback, RequestPermissionsListener {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val bindingOrNull get() = _binding
    private lateinit var mapViewModel: MapViewModel
    private var googleMap: GoogleMap? = null
    private var pharmacyDtos: List<PharmacyDto> = emptyList()
    private var currentLatLngCache: LatLng? = null
    private var pagerCallbackRegistered = false
    private var mapReady = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        try {
            MapsInitializer.initialize(
                requireContext().applicationContext, MapsInitializer.Renderer.LATEST
            ) { _: MapsInitializer.Renderer? -> }
        } catch (_: Exception) {}
        try { setupVMComponent() } catch (_: Exception) {}
        return binding.root
    }

    private fun setupVMComponent() {
        mapViewModel = ViewModelProvider(this)[MapViewModel::class.java]
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (!isAdded) return@registerForActivityResult
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                try { Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                obtainGoogleMapInstance()
            } else {
                try { Toast.makeText(requireContext(), "Permission denied. Cannot show nearby pharmacies.", Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
                try { mapViewModel.loadFromCache() } catch (_: Exception) {}
                try { mapViewModel.fetchAllPharmacies(31.5, 34.46) } catch (_: Exception) {}
            }
        }
        try { mapViewModel.setRequestPermissionLauncher(requestPermissionLauncher) } catch (_: Exception) {}
    }

    private var permissionDialogShown = false

    override fun onResume() {
        super.onResume()
        // Permission granted elsewhere (Settings) applies without exit/re-enter:
        // (re)attach map, refresh blue-dot layer, and fetch if list is empty.
        try {
            if (mapViewModel.arePermissionsGranted()) {
                if (googleMap == null) obtainGoogleMapInstance() else refreshLocationLayer()
                if (pharmacyDtos.isEmpty()) mapViewModel.getUserLastLocation()
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun refreshLocationLayer() {
        try {
            val gm = googleMap ?: return
            if (!isAdded || _binding == null) return
            if (mapViewModel.arePermissionsGranted()) {
                gm.isMyLocationEnabled = true
                gm.uiSettings.isMyLocationButtonEnabled = false
            }
        } catch (_: SecurityException) {} catch (_: Exception) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeCurrentLocation()
        observePharmacies()
        try {
            bindingOrNull?.fabRecenter?.setOnClickListener {
                val ll = currentLatLngCache
                if (ll != null) {
                    try { mapViewModel.animateCameraToPosition(ll, 15f) } catch (_: Exception) {}
                } else {
                    try { mapViewModel.getUserLastLocation() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
        if (mapViewModel.arePermissionsGranted()) {
            obtainGoogleMapInstance()
        } else if (!permissionDialogShown) {
            permissionDialogShown = true
            try {
                view.post {
                    try {
                        if (!isAdded || _binding == null || childFragmentManager.isStateSaved) {
                            throw IllegalStateException("not ready")
                        }
                        RequestPermissionsDialogFragment.newInstance("Please allow the map permission to be able to use the app properly")
                            .show(childFragmentManager, "MapPermissions")
                    } catch (_: Exception) {
                        try { mapViewModel.loadFromCache() } catch (_: Exception) {}
                        try { mapViewModel.fetchAllPharmacies(31.5, 34.46) } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {
                try { mapViewModel.loadFromCache() } catch (_: Exception) {}
                try { mapViewModel.fetchAllPharmacies(31.5, 34.46) } catch (_: Exception) {}
            }
        } else {
            mapViewModel.loadFromCache()
            mapViewModel.fetchAllPharmacies(31.5, 34.46)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(gMap: GoogleMap) {
        if (!isAdded || _binding == null) { googleMap = gMap; return }
        googleMap = gMap
        mapReady = true
        try { mapViewModel.setMap(gMap) } catch (_: Exception) {}
        // Night surprise: dark map style follows app theme
        try {
            val night = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            if (night) {
                gMap.setMapStyle(com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_night))
            } else {
                gMap.setMapStyle(null)
            }
        } catch (_: Exception) {}
        try { bindingOrNull?.fabRecenter?.visibility = View.VISIBLE } catch (_: Exception) {}
        try {
            if (mapViewModel.arePermissionsGranted()) {
                gMap.isMyLocationEnabled = true
                gMap.uiSettings.isMyLocationButtonEnabled = true
            }
        } catch (_: SecurityException) {}
        gMap.uiSettings.isZoomControlsEnabled = true
        gMap.setOnMarkerClickListener { marker ->
            try {
                val tag = marker.tag as? PharmacyDto
                val idx = if (tag != null) pharmacyDtos.indexOfFirst { it.id == tag.id } else -1
                if (idx != -1) {
                    try { bindingOrNull?.pharmaciesLocationsPager?.currentItem = idx } catch (_: Exception) {}
                    try { mapViewModel.animateCameraToPosition(LatLng(pharmacyDtos[idx].latitude, pharmacyDtos[idx].longitude), 15f) } catch (_: Exception) {}
                }
                try { marker.showInfoWindow() } catch (_: Exception) {}
            } catch (_: Exception) {}
            true
        }
        mapViewModel.getUserLastLocation()
        if (pharmacyDtos.isNotEmpty()) renderMarkers()
    }

    private fun obtainGoogleMapInstance() {
        try {
            if (!isAdded || _binding == null) return
            val mapFragment = try {
                childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment
            } catch (_: Exception) { null } ?: return
            try { mapFragment.getMapAsync(this) } catch (_: Exception) {
                // Play Services missing/outdated: fall back to list-only mode
                try { mapViewModel.loadFromCache() } catch (_: Exception) {}
                try { mapViewModel.fetchAllPharmacies(31.5, 34.46) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun observeCurrentLocation() {
        mapViewModel.currentLocation.observe(viewLifecycleOwner) { latLng ->
            if (!isAdded || _binding == null) return@observe
            latLng?.let {
                currentLatLngCache = it
                try { mapViewModel.animateCameraToPosition(it, 14f) } catch (_: Exception) {}
                try { mapViewModel.getNearbyPharmacies(it.latitude, it.longitude, 600.0) } catch (_: Exception) {}
            }
        }
    }

    private fun observePharmacies() {
        mapViewModel.pharmacies.observe(viewLifecycleOwner) { list ->
            if (!isAdded || _binding == null) { pharmacyDtos = list ?: emptyList(); return@observe }
            pharmacyDtos = list ?: emptyList()
            renderMarkers()
            setupPager()
        }
    }

    private fun renderMarkers() {
        try {
            val gm = googleMap ?: return
            if (!isAdded) return
            gm.clear()
            currentLatLngCache?.let {
                try { gm.addMarker(MarkerOptions().position(it).title("My Location").snippet("You are here")) } catch (_: Exception) {}
            }
            val ctx = context ?: return
            pharmacyDtos.forEach { dto ->
                try {
                    val snippet = "${if (dto.isOpen) "Open" else "Closed"} • ${String.format("%.1f", dto.ratingAvg)}★ • ${dto.phone ?: dto.address}"
                    val opts = MarkerOptions().position(LatLng(dto.latitude, dto.longitude)).title(dto.name).snippet(snippet).icon(getMarkerIconFromDrawable(ctx, R.drawable.ic_hospital))
                    val m = gm.addMarker(opts)
                    m?.tag = dto
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun setupPager() {
        val b = bindingOrNull ?: return
        if (!isAdded) return
        try {
            // Peek neighbors + depth zoom: pager cards feel swipeable, current pops
            b.pharmaciesLocationsPager.offscreenPageLimit = 1
            val margin = (12 * resources.displayMetrics.density).toInt()
            b.pharmaciesLocationsPager.setPageTransformer(androidx.viewpager2.widget.CompositePageTransformer().apply {
                addTransformer(androidx.viewpager2.widget.MarginPageTransformer(margin))
                addTransformer { page, position ->
                    val scale = 0.92f + (1 - kotlin.math.abs(position).coerceAtMost(1f)) * 0.08f
                    page.scaleX = scale
                    page.scaleY = scale
                    page.alpha = 0.65f + (1 - kotlin.math.abs(position).coerceAtMost(1f)) * 0.35f
                }
            })
        } catch (_: Exception) {}
        val list = ArrayList(pharmacyDtos.map { dto -> Pharmacy(uid = dto.id, pharmacy_image_url = dto.imageUrl ?: "", pharmacy_name = dto.name, phone = dto.phone ?: "", address = dto.address, lat = dto.latitude, lng = dto.longitude, token = "", ratingAvg = dto.ratingAvg, totalRatings = dto.totalRatings, isOpen = dto.isOpen, distanceKm = dto.distanceKm) })
        try { b.pharmaciesLocationsPager.adapter = PharmaciesLocationsAdapter(list) {
            val b = android.os.Bundle().apply {
                putString("receiverUid", it.uid)
                putString("receiverFullName", it.pharmacy_name)
                putString("receiverUrlImage", it.pharmacy_image_url)
                putString("receiverToken", it.token)
            }
            try { findNavController().navigate(R.id.action_navigation_map_to_navigation_messaging, b) } catch (_: Exception) {}
        } } catch (_: Exception) {}
        try {
            if (!pagerCallbackRegistered) {
                pagerCallbackRegistered = true
                b.pharmaciesLocationsPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        if (!isAdded || _binding == null) return
                        if (position in pharmacyDtos.indices) {
                            val dto = pharmacyDtos[position]
                            try { mapViewModel.animateCameraToPosition(LatLng(dto.latitude, dto.longitude), 15f) } catch (_: Exception) {}
                        }
                    }
                })
            }
            if (pharmacyDtos.isNotEmpty()) {
                val first = pharmacyDtos[0]
                try { mapViewModel.animateCameraToPosition(LatLng(first.latitude, first.longitude), 13f) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private fun getMarkerIconFromDrawable(context: Context, resId: Int): BitmapDescriptor {
        return try {
            val d = ContextCompat.getDrawable(context, resId)
                ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            val base = 96
            val w = base.coerceIn(48, 144)
            val bmp = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            d.setBounds(0, 0, w, w)
            d.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bmp)
        } catch (_: Exception) { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) }
    }

    override fun onAllowClicked() { try { mapViewModel.requestPermissions() } catch (_: Exception) {} }
    override fun onDenyClicked() {
        try { mapViewModel.loadFromCache() } catch (_: Exception) {}
        try { mapViewModel.fetchAllPharmacies(31.5, 34.46) } catch (_: Exception) {}
        val b = bindingOrNull ?: return
        if (!isAdded) return
        try {
            com.google.android.material.snackbar.Snackbar.make(b.root, getString(R.string.permissions_message_sorry_you_can_not), com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setAction("Settings") {
                    try { startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${requireContext().packageName}") }) } catch (_: Exception) {}
                }.show()
        } catch (_: Exception) {
            try { Toast.makeText(requireContext(), getString(R.string.permissions_message_sorry_you_can_not), Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
        }
    }
    override fun onDestroyView() {
        try { mapViewModel.currentLocation.removeObservers(viewLifecycleOwner) } catch (_: Exception) {}
        try { mapViewModel.pharmacies.removeObservers(viewLifecycleOwner) } catch (_: Exception) {}
        googleMap = null
        mapReady = false
        _binding = null
        super.onDestroyView()
    }
}
// trigger rebuild Thu Sep 10 02:47:34 AM EEST 2026
