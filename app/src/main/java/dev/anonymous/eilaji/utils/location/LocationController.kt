package dev.anonymous.eilaji.utils.location

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.google.android.gms.maps.model.LatLng

class LocationController(context: Context) {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager


    // Listener Might be Useful
    private var locationListener: LocationListener? = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Handle location updates here
        }

        override fun onProviderEnabled(provider: String) {
            // Handle provider enabled state changes here
        }

        override fun onProviderDisabled(provider: String) {
            // Handle provider disabled state changes here
        }
        /*
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
            // Handle status changes here
        }*/
    }

    /**
     *   ##You would need to register the listener with the LocationManager and call requestLocationUpdates() to start receiving location updates:
     *
     *   locationManager.requestLocationUpdates(
     *   LocationManager.GPS_PROVIDER,
     *   MIN_TIME_BETWEEN_UPDATES,
     *   MIN_DISTANCE_CHANGE_FOR_UPDATES,
     *   locationListener
     *   )
     *
     *   ##Remember to unregister the listener using removeUpdates() when you no longer need to receive location updates:
     *
     *   locationManager.removeUpdates(locationListener)
     *   In the simplified implementation provided earlier, we did not include these steps since we were only retrieving the last known location without actively requesting updates.
     *   */


    /**
     *  #This to get the location using the providers
     *  and return it as a Pair
     * */
    fun getUserLocationPair(): Pair<Double, Double>? {
        val providers = locationManager.getProviders(true)
        var location: Location? = null
        for (provider in providers) {
            try {
                location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    break
                }
            } catch (e: SecurityException) {
                // Handle exception if location permission is not granted
            }
        }

        if (location != null) {
            val latitude = location.latitude
            val longitude = location.longitude
            return Pair(latitude, longitude)
        }

        return null
    }

    /**
     *  #This to get the location using the providers
     *  and return it as a LatLng Of the <@sample MapLibrary>
     * */
    fun getUserLocationLatLng(): LatLng? {
        val providers = locationManager.getProviders(true)
        var location: Location? = null
        for (provider in providers) {
            try {
                location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    break
                }
            } catch (e: SecurityException) {
                // Handle exception if location permission is not granted
            }
        }

        return location?.let {
            LatLng(it.latitude, it.longitude)
        }
    }
}
