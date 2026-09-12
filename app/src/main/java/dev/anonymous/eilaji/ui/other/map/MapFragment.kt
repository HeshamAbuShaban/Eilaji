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
    private lateinit var _binding: FragmentMapBinding
    private val binding get() = _binding
    private lateinit var mapViewModel: MapViewModel
    private var googleMap: GoogleMap? = null
    private var pharmacyDtos: List<PharmacyDto> = emptyList()
    private var currentLatLngCache: LatLng? = null
    private var pagerCallbackRegistered = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(layoutInflater)
        MapsInitializer.initialize(requireContext(), MapsInitializer.Renderer.LATEST) { _: MapsInitializer.Renderer? -> }
        setupVMComponent()
        return binding.root
    }

    private fun setupVMComponent() {
        mapViewModel = ViewModelProvider(this)[MapViewModel::class.java]
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.all { it.value }
            if (allGranted) {
                Toast.makeText(requireContext(), "Location permission granted", Toast.LENGTH_SHORT).show()
                obtainGoogleMapInstance()
            } else {
                Toast.makeText(requireContext(), "Permission denied. Cannot show nearby pharmacies.", Toast.LENGTH_SHORT).show()
                mapViewModel.loadFromCache()
            }
        }
        mapViewModel.setRequestPermissionLauncher(requestPermissionLauncher)
    }

    private var permissionDialogShown = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeCurrentLocation()
        observePharmacies()
        if (mapViewModel.arePermissionsGranted()) {
            obtainGoogleMapInstance()
        } else if (!permissionDialogShown) {
            permissionDialogShown = true
            try {
                RequestPermissionsDialogFragment.newInstance("Please allow the map permission to be able to use the app properly")
                    .show(childFragmentManager, "MapPermissions")
            } catch (_: Exception) {
                mapViewModel.loadFromCache()
                mapViewModel.fetchAllPharmacies(31.5, 34.46)
            }
        } else {
            mapViewModel.loadFromCache()
            mapViewModel.fetchAllPharmacies(31.5, 34.46)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        mapViewModel.setMap(gMap)
        try {
            if (mapViewModel.arePermissionsGranted()) {
                gMap.isMyLocationEnabled = true
                gMap.uiSettings.isMyLocationButtonEnabled = true
            }
        } catch (_: SecurityException) {}
        gMap.uiSettings.isZoomControlsEnabled = true
        gMap.setOnMarkerClickListener { marker ->
            val tag = marker.tag as? PharmacyDto
            val idx = if (tag != null) pharmacyDtos.indexOfFirst { it.id == tag.id } else -1
            if (idx != -1) {
                binding.pharmaciesLocationsPager.currentItem = idx
                mapViewModel.animateCameraToPosition(LatLng(pharmacyDtos[idx].latitude, pharmacyDtos[idx].longitude), 15f)
            }
            marker.showInfoWindow()
            true
        }
        mapViewModel.getUserLastLocation()
        if (pharmacyDtos.isNotEmpty()) renderMarkers()
    }

    private fun obtainGoogleMapInstance() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment ?: return
        mapFragment.getMapAsync(this)
    }

    private fun observeCurrentLocation() {
        mapViewModel.currentLocation.observe(viewLifecycleOwner) { latLng ->
            latLng?.let {
                currentLatLngCache = it
                mapViewModel.animateCameraToPosition(it, 14f)
                mapViewModel.getNearbyPharmacies(it.latitude, it.longitude, 600.0)
            }
        }
    }

    private fun observePharmacies() {
        mapViewModel.pharmacies.observe(viewLifecycleOwner) { list ->
            pharmacyDtos = list ?: emptyList()
            renderMarkers()
            setupPager()
        }
    }

    private fun renderMarkers() {
        val gm = googleMap ?: return
        gm.clear()
        currentLatLngCache?.let { gm.addMarker(MarkerOptions().position(it).title("My Location").snippet("You are here")) }
        pharmacyDtos.forEach { dto ->
            val opts = MarkerOptions().position(LatLng(dto.latitude, dto.longitude)).title(dto.name).snippet(dto.phone ?: dto.address).icon(getMarkerIconFromDrawable(requireContext(), R.drawable.ic_hospital))
            val m = gm.addMarker(opts)
            m?.tag = dto
        }
    }

    private fun setupPager() {
        // Fixed: inline Pharmacy mapping to avoid private extension receiver mismatch
        val list = ArrayList(pharmacyDtos.map { dto -> Pharmacy(uid = dto.id, pharmacy_image_url = dto.imageUrl ?: "", pharmacy_name = dto.name, phone = dto.phone ?: "", address = dto.address, lat = dto.latitude, lng = dto.longitude, token = "") })
        binding.pharmaciesLocationsPager.adapter = PharmaciesLocationsAdapter(list) {
            val action = MapFragmentDirections.actionNavigationMapToNavigationMessaging(null, it.uid, it.pharmacy_name, it.pharmacy_image_url, it.token, null, null)
            findNavController().navigate(action)
        }
        if (!pagerCallbackRegistered) {
            pagerCallbackRegistered = true
            var dragging = false
            binding.pharmaciesLocationsPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) { dragging = state == ViewPager2.SCROLL_STATE_DRAGGING }
                override fun onPageSelected(position: Int) {
                    if (position in pharmacyDtos.indices) {
                        val dto = pharmacyDtos[position]
                        mapViewModel.animateCameraToPosition(LatLng(dto.latitude, dto.longitude), 15f)
                    }
                }
            })
        }
        if (pharmacyDtos.isNotEmpty()) {
            val first = pharmacyDtos[0]
            mapViewModel.animateCameraToPosition(LatLng(first.latitude, first.longitude), 13f)
        }
    }

    private fun getMarkerIconFromDrawable(context: Context, resId: Int): BitmapDescriptor {
        return try {
            val d = ContextCompat.getDrawable(context, resId)!!
            val w = (d.intrinsicWidth * 0.2).toInt().coerceAtLeast(48)
            val h = (d.intrinsicHeight * 0.2).toInt().coerceAtLeast(48)
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            d.setBounds(0, 0, w, h)
            d.draw(canvas)
            BitmapDescriptorFactory.fromBitmap(bmp)
        } catch (_: Exception) { BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) }
    }

    override fun onAllowClicked() { mapViewModel.requestPermissions() }
    override fun onDenyClicked() {
        mapViewModel.loadFromCache()
        mapViewModel.fetchAllPharmacies(31.5, 34.46)
        try {
            com.google.android.material.snackbar.Snackbar.make(binding.root, getString(R.string.permissions_message_sorry_you_can_not), com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                .setAction("Settings") {
                    try { startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = android.net.Uri.parse("package:${requireContext().packageName}") }) } catch (_: Exception) {}
                }.show()
        } catch (_: Exception) {
            try { Toast.makeText(requireContext(), getString(R.string.permissions_message_sorry_you_can_not), Toast.LENGTH_SHORT).show() } catch (_: Exception) {}
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        mapViewModel.currentLocation.removeObservers(viewLifecycleOwner)
        mapViewModel.pharmacies.removeObservers(viewLifecycleOwner)
    }
}
// trigger rebuild Thu Sep 10 02:47:34 AM EEST 2026
