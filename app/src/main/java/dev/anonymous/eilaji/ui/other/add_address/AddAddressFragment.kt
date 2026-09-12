package dev.anonymous.eilaji.ui.other.add_address

import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentAddAddressBinding
import dev.anonymous.eilaji.storage.AppSharedPreferences
import java.util.Locale

class AddAddressFragment : Fragment() {
    private var _binding: FragmentAddAddressBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AddAddressViewModel
    private var map: GoogleMap? = null
    private var marker: Marker? = null
    private var picked: LatLng = LatLng(31.5, 34.46)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        MapsInitializer.initialize(requireContext(), MapsInitializer.Renderer.LATEST) { }
        _binding = FragmentAddAddressBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[AddAddressViewModel::class.java]
        binding.includeAppBarLayoutAddAddress.toolbarApp.title = getString(R.string.add_address)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        restoreSaved()
        Handler(Looper.getMainLooper()).postDelayed({
            val f = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            f?.getMapAsync(cb)
        }, 300)
        binding.buUseMyLocation.setOnClickListener { useMyLocation() }
        binding.buAddAddress.setOnClickListener { saveAddress() }
    }

    private fun restoreSaved() {
        try {
            val prefs = AppSharedPreferences.getInstance(requireContext())
            val lat = prefs.getString("delivery_lat", null)?.toDoubleOrNull()
            val lng = prefs.getString("delivery_lng", null)?.toDoubleOrNull()
            if (lat != null && lng != null) picked = LatLng(lat, lng)
            val addr = prefs.getString("delivery_address", null)
            if (!addr.isNullOrBlank()) binding.edDetailsAddress.setText(addr)
        } catch (_: Exception) {}
    }

    private val cb = OnMapReadyCallback { m: GoogleMap ->
        map = m
        m.uiSettings.isZoomControlsEnabled = true
        m.uiSettings.isMyLocationButtonEnabled = false
        try { m.isMyLocationEnabled = false } catch (_: SecurityException) {}
        placeMarker(picked)
        m.moveCamera(CameraUpdateFactory.newLatLngZoom(picked, 15f))
        m.setOnMapClickListener { ll ->
            picked = ll
            placeMarker(ll)
            reverseGeocode(ll)
        }
    }

    private fun placeMarker(ll: LatLng) {
        val m = map ?: return
        marker?.remove()
        marker = m.addMarker(MarkerOptions().position(ll).draggable(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
        m.animateCamera(CameraUpdateFactory.newLatLng(ll))
    }

    private fun reverseGeocode(ll: LatLng) {
        try {
            val g = Geocoder(requireContext(), Locale.getDefault())
            @Suppress("DEPRECATION")
            val list = g.getFromLocation(ll.latitude, ll.longitude, 1)
            val line = list?.firstOrNull()?.getAddressLine(0)
            if (!line.isNullOrBlank() && binding.edDetailsAddress.text.toString().isBlank()) {
                binding.edDetailsAddress.setText(line)
            }
            binding.tvPickedCoords.text = String.format(Locale.US, "%.5f, %.5f", ll.latitude, ll.longitude)
        } catch (_: Exception) {}
    }

    private fun useMyLocation() {
        try {
            val client = LocationServices.getFusedLocationProviderClient(requireContext())
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    picked = LatLng(loc.latitude, loc.longitude)
                    placeMarker(picked)
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(picked, 16f))
                    reverseGeocode(picked)
                } else {
                    Toast.makeText(requireContext(), "Location unavailable — move the pin manually", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Location unavailable", Toast.LENGTH_SHORT).show()
            }
        } catch (_: SecurityException) {
            Toast.makeText(requireContext(), "Location permission needed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveAddress() {
        val details = binding.edDetailsAddress.text.toString().trim()
        if (details.isBlank()) {
            Snackbar.make(binding.root, "Please add address details", Snackbar.LENGTH_SHORT).show()
            return
        }
        try {
            val prefs = AppSharedPreferences.getInstance(requireContext())
            prefs.putString("delivery_address", details)
            prefs.putString("delivery_lat", picked.latitude.toString())
            prefs.putString("delivery_lng", picked.longitude.toString())
            Snackbar.make(binding.root, "Address saved", Snackbar.LENGTH_SHORT).show()
            view?.postDelayed({ try { requireActivity().onBackPressedDispatcher.onBackPressed() } catch (_: Exception) {} }, 600)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
