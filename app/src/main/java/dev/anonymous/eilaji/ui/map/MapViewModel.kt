package dev.anonymous.eilaji.ui.map

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.widget.Toast
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
    private lateinit var map: GoogleMap
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)
    private val _currentLocation = MutableLiveData<LatLng>()
    val currentLocation: LiveData<LatLng> = _currentLocation

    fun updateMarkerOptions() {
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    _currentLocation.value = currentLatLng
                }
            }
        } else {
            // Handle the case when location permission is not granted
            Toast.makeText(
                getApplication(),
                "Please turn on the required permissions",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun setMap(googleMap: GoogleMap) {
        map = googleMap
    }

    fun addMarkerToMap(markerOptions: MarkerOptions) {
        map.addMarker(markerOptions)
    }

    fun moveCameraToPosition(latLng: LatLng, zoomLevel: Float) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
        map.moveCamera(cameraUpdate)
    }

    fun animateCameraToPosition(latLng: LatLng, zoomLevel: Float) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
        map.animateCamera(cameraUpdate)
    }
    fun checkLocationPermission(): Boolean {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val result = ContextCompat.checkSelfPermission(getApplication(), permission)
        return result == PackageManager.PERMISSION_GRANTED
    }


}


