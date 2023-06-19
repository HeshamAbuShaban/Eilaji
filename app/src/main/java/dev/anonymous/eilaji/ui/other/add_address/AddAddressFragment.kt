package dev.anonymous.eilaji.ui.other.add_address

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentAddAddressBinding

class AddAddressFragment : Fragment() {
    private var _binding: FragmentAddAddressBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AddAddressViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Fix => Unable to update local snapshot for com.google.android.libraries.conservativeness
        // Fix => set_timerslack_ns write failed: Operation not permitted
        MapsInitializer.initialize(requireContext(), MapsInitializer.Renderer.LATEST) { }

        _binding = FragmentAddAddressBinding.inflate(inflater, container, false)

        viewModel = ViewModelProvider(this)[AddAddressViewModel::class.java]

        binding.includeAppBarLayoutAddAddress.toolbarApp.title = getString(R.string.add_address)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
            mapFragment?.getMapAsync(callback)
        }, 300)
    }

    private val callback = OnMapReadyCallback { map: GoogleMap ->
        val latLng = LatLng(31.4495843, 34.3951343)
        map.uiSettings.isScrollGesturesEnabled = false
        map.uiSettings.setAllGesturesEnabled(false)
        val marker = MarkerOptions()
            .position(latLng)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        map.addMarker(marker)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }
}