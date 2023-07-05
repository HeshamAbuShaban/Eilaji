package dev.anonymous.eilaji.ui.other.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var requestPermissionLauncher : ActivityResultLauncher<Array<String>>
    //.. make sure to call this in onCreate or onAttach or onViewCreated
    fun setRequestPermissionLauncher(requestPermissionLauncher : ActivityResultLauncher<Array<String>>){
        this.requestPermissionLauncher = requestPermissionLauncher
    }

    private lateinit var googleMap: GoogleMap


    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)


    private val _currentLocation = MutableLiveData<LatLng>()
    val currentLocation: LiveData<LatLng> = _currentLocation

    @SuppressLint("MissingPermission")
    fun updateLastLocation() {
        if (arePermissionsGranted()) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    _currentLocation.value = currentLatLng
                }
            }.addOnFailureListener {
                println("addOnFailureListener " + it.message)
            }
        } else {
            // Handle the case when location permission is not granted
            requestPermissions()
        }
    }

    fun setMap(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }

    fun addMarkerToMap(markerOptions: MarkerOptions) {
        googleMap.addMarker(markerOptions)
    }

    /*fun moveCameraToPosition(latLng: LatLng, zoomLevel: Float) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
        googleMap.moveCamera(cameraUpdate)
    }*/

    fun animateCameraToPosition(latLng: LatLng, zoomLevel: Float) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
        googleMap.animateCamera(cameraUpdate)
    }


    // STOPSHIP: 5 july 2023
    // NEW Permission Request logic

    // are the permissions granted ?
     fun arePermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED
        }
    }
    fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}

