package dev.anonymous.eilaji.ui.map

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.temp.permission.PermissionHandler

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var viewModel: MapViewModel
    private lateinit var permissionHandler: PermissionHandler


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //Initialize the permission handler in your fragment or activity:
        permissionHandler = PermissionHandler(this)
        permissionHandler.registerPermissionLauncher()

        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize the view model
        viewModel = ViewModelProvider(this)[MapViewModel::class.java]
        // create google Map Instance && getting the current location
        viewModel.checkLocationPermission().let { granted ->
            if (granted) {
                // Permission granted, proceed with map-related functionality
                obtainGoogleMapInstance()
                observeCurrentLocation()
            } else {
                // Permission denied, handle accordingly (e.g., show a message or disable map-related functionality)
            }
        }
        /*obtainGoogleMapInstance()
        observeCurrentLocation()*/
    }

    /*private fun requestTheRequiredPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (permissionHandler.checkPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Permission is already granted. Proceed with using Google Maps.
        } else {
            permissionHandler.requestPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) { isGranted ->
                if (isGranted) {
                    // Permission is granted. Proceed with using Google Maps.
                } else {
                    // Permission is denied. Handle this case accordingly.
                }
            }
        }

    }*/

    private fun obtainGoogleMapInstance() {
        // Obtain the GoogleMap instance from SupportMapFragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun observeCurrentLocation() {
        viewModel.currentLocation.observe(viewLifecycleOwner) { currentLatLng ->
            // Move the camera to the current location
            viewModel.moveCameraToPosition(currentLatLng, 15f)
            // Add a marker to the current location
            viewModel.addMarkerToMap(
                markerOptions(latLng = currentLatLng, title = "Home Location")
            )
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Set the GoogleMap instance in the view model
        viewModel.setMap(googleMap)

        viewModel.updateMarkerOptions()

        // Call the methods from the view model
        viewModel.addMarkerToMap(
            markerOptions(LatLng(31.512353713620534, 34.453313673293344), "Mark 1(al-bala-dya park)")
        )
//        viewModel.moveCameraToPosition(LatLng(37.7749, -122.4194), 15f)
        viewModel.animateCameraToPosition(LatLng(31.512353713620534, 34.453313673293344), 15f)
    }

    private fun markerOptions(latLng: LatLng, title: String) =
        MarkerOptions().position(latLng).title(title)


}