package dev.anonymous.eilaji.ui.other.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PharmacyDto
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.utils.AppController
import dev.anonymous.eilaji.utils.location.LocationController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    fun setRequestPermissionLauncher(requestPermissionLauncher: ActivityResultLauncher<Array<String>>) {
        this.requestPermissionLauncher = requestPermissionLauncher
    }

    private var googleMap: GoogleMap? = null
    private var fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(application)
    private val appContext = getApplication<Application>()
    private val prefs = AppSharedPreferences.getInstance(appContext)
    private val apiService by lazy { NetworkModule.provideApiService(appContext) }
    private val gson = Gson()

    private val _currentLocation = MutableLiveData<LatLng?>()
    val currentLocation: LiveData<LatLng?> = _currentLocation

    private val _pharmacies = MutableLiveData<List<PharmacyDto>>()
    val pharmacies: LiveData<List<PharmacyDto>> = _pharmacies

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    @SuppressLint("MissingPermission")
    fun getUserLastLocation() {
        if (arePermissionsGranted()) {
            val locationController = LocationController(AppController.getInstance() ?: appContext)
            val location = locationController.getUserLocationLatLng()
            if (location != null) {
                _currentLocation.value = location
                Log.i("MVM", "updateLastLocation: $location")
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { fusedLocation ->
                    if (fusedLocation != null) {
                        val latLng = LatLng(fusedLocation.latitude, fusedLocation.longitude)
                        _currentLocation.value = latLng
                        Log.i("MVM", "FusedUpdateLastLocation: $latLng")
                    } else {
                        val fallback = LatLng(FALLBACK_LAT, FALLBACK_LNG)
                        _currentLocation.value = fallback
                        Log.i("MVM", "fallback location used: $fallback")
                    }
                }.addOnFailureListener {
                    val fallback = LatLng(FALLBACK_LAT, FALLBACK_LNG)
                    _currentLocation.value = fallback
                    Log.e("MVM", "lastLocation failure, fallback: $fallback msg=${it.message}")
                }
            }
        } else {
            requestPermissions()
        }
    }

    fun getNearbyPharmacies(lat: Double, lng: Double, radius: Double = 10.0) {
        _isLoading.value = true
        apiService.getNearbyPharmacies(lat, lng, radius).enqueue(object : Callback<ApiResponse<List<PharmacyDto>>> {
            override fun onResponse(call: Call<ApiResponse<List<PharmacyDto>>>, response: Response<ApiResponse<List<PharmacyDto>>>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.success && body.data != null) {
                        val sorted = body.data!!.sortedWith(compareBy { it.distanceKm ?: haversineKm(lat, lng, it.latitude, it.longitude) })
                        _pharmacies.value = sorted
                        cachePharmacies(sorted)
                        _error.value = null
                        Log.i("MVM", "getNearbyPharmacies success size=${sorted.size}")
                    } else {
                        Log.w("MVM", "getNearbyPharmacies empty or not success, load cache")
                        loadFromCache()
                    }
                } else {
                    Log.w("MVM", "getNearbyPharmacies http ${response.code()} load cache")
                    loadFromCache()
                }
            }

            override fun onFailure(call: Call<ApiResponse<List<PharmacyDto>>>, t: Throwable) {
                _isLoading.value = false
                Log.e("MVM", "getNearbyPharmacies failure ${t.message} load cache")
                loadFromCache()
            }
        })
    }

    private fun cachePharmacies(list: List<PharmacyDto>) {
        try {
            val json = gson.toJson(list)
            prefs.putCachedNearbyPharmacies(json)
            prefs.putCachedNearbyTimestamp(System.currentTimeMillis())
            prefs.putString("cached_nearby_pharmacies", json)
            prefs.putLong("cached_nearby_timestamp", System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("MVM", "cache save failed ${e.message}")
        }
    }

    fun loadFromCache() {
        try {
            var json = prefs.getCachedNearbyPharmacies()
            if (json == null) json = prefs.getString("cached_nearby_pharmacies", null)
            if (json != null) {
                val type = object : TypeToken<List<PharmacyDto>>() {}.type
                val cached: List<PharmacyDto> = gson.fromJson(json, type)
                if (cached.isNotEmpty()) {
                    val sorted = cached.sortedWith(compareBy { it.distanceKm ?: 0.0 })
                    _pharmacies.value = sorted
                    _error.value = "offline_cache"
                    Log.i("MVM", "loaded from cache size=${sorted.size}")
                    return
                }
            }
            _error.value = "no_cache"
        } catch (e: Exception) {
            Log.e("MVM", "load cache failed ${e.message}")
            _error.value = e.message
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun setMap(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }

    fun addMarkerToMap(markerOptions: MarkerOptions): Marker? {
        return googleMap?.addMarker(markerOptions)
    }

    fun animateCameraToPosition(latLng: LatLng, zoomLevel: Float) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomLevel)
        googleMap?.animateCamera(cameraUpdate)
    }

    fun arePermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(getApplication(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions() {
        if (::requestPermissionLauncher.isInitialized) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        const val FALLBACK_LAT = 33.5138
        const val FALLBACK_LNG = 36.2765
        const val CACHE_KEY = "cached_nearby_pharmacies"
        const val CACHE_TS_KEY = "cached_nearby_timestamp"
    }
}
