package dev.anonymous.eilaji.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
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
import dev.anonymous.eilaji.temp.permission.PermissionHandler
import dev.anonymous.eilaji.utils.DummyData


class MapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MapViewModel
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Fix => Unable to update local snapshot for com.google.android.libraries.consentverifier
        // Fix => Unable to update local snapshot for com.google.android.libraries.conservativeness
        // Fix => set_timerslack_ns write failed: Operation not permitted
        MapsInitializer.initialize(
            requireContext(),
            MapsInitializer.Renderer.LATEST
        ) { _: MapsInitializer.Renderer? -> }

        _binding = FragmentMapBinding.inflate(inflater, container, false)

        //Initialize the permission handler in your fragment or activity:
        permissionHandler = PermissionHandler(this)
        permissionHandler.registerPermissionLauncher()

        return binding.root
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

        setupAdsPager()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Set the GoogleMap instance in the view model
        viewModel.setMap(googleMap)

        googleMap.setOnMarkerClickListener { marker ->
            val indexPharmacies = DummyData.listPharmaciesModels.indexOfFirst {
                marker.position.latitude == it.lat && marker.position.longitude == it.lng
            }

            if (indexPharmacies != -1) {
                binding.pharmaciesLocationsPager.currentItem = indexPharmacies
                viewModel.animateCameraToPosition(
                    LatLng(
                        DummyData.listPharmaciesModels[indexPharmacies].lat,
                        DummyData.listPharmaciesModels[indexPharmacies].lng
                    ),
                    17f
                )
            }

            true
        }

        viewModel.updateMarkerOptions()

        // Call the methods from the view model
        viewModel.moveCameraToPosition(
            LatLng(31.449624242321853, 34.39510808345772),
            17f
        )

        DummyData.listPharmaciesModels.forEach {
            viewModel.addMarkerToMap(
                markerOptions(
                    latLng = LatLng(it.lat, it.lng),
                    title = it.name,
                    getMarkerIconFromDrawable(requireContext(), R.drawable.ic_hospital)
                )
            )
        }
    }

    private fun setupAdsPager() {
        var scrollIsDragging = false

        with(binding.pharmaciesLocationsPager) {
            adapter = PharmaciesLocationsAdapter(DummyData.listPharmaciesModels)
            registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
//                    ViewPager2.SCROLL_STATE_DRAGGING // 1 // سحب
//                    ViewPager2.SCROLL_STATE_SETTLING // 2 // افلات
//                    ViewPager2.SCROLL_STATE_IDLE     // 0 // توقف

                    if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                        scrollIsDragging = true
                    } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                        scrollIsDragging = false
                    }
                }

                override fun onPageSelected(position: Int) {
                    // يتم استدعاء onPageSelected
                    // فقط عند الانتقال الى عنصر اخر وقبل ان تصبح حالة التمرير SCROLL_STATE_IDLE
                    if (scrollIsDragging) {
                        viewModel.animateCameraToPosition(
                            LatLng(
                                DummyData.listPharmaciesModels[position].lat,
                                DummyData.listPharmaciesModels[position].lng
                            ),
                            17f
                        )
                    }
                }
            })
        }
    }

    private fun markerOptions(
        latLng: LatLng,
        title: String,
        icon: BitmapDescriptor = BitmapDescriptorFactory.defaultMarker(
            BitmapDescriptorFactory.HUE_BLUE
        )
    ) = MarkerOptions().position(latLng).title(title).icon(icon)

    private fun getMarkerIconFromDrawable(context: Context, resId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, resId)!!

        val drawableWidth = (drawable.intrinsicWidth * 0.2).toInt()
        val drawableHeight = (drawable.intrinsicHeight * 0.2).toInt()

        val canvas = Canvas()
        // الطول والعرض المطلوبان للصورة
        val bitmap = Bitmap.createBitmap(
            drawableWidth,
            drawableHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        // رسم المسطتيل الذي سيحوي الصورة
        drawable.setBounds(
            0,
            0,
            drawableWidth,
            drawableHeight
        )
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun obtainGoogleMapInstance() {
        // Obtain the GoogleMap instance from SupportMapFragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun observeCurrentLocation() {
        viewModel.currentLocation.observe(viewLifecycleOwner) { currentLatLng ->
            // Move the camera to the current location
            viewModel.animateCameraToPosition(currentLatLng, 17f)

            // Add a marker to the current location
            viewModel.addMarkerToMap(
                markerOptions(latLng = currentLatLng, title = "Home Location")
            )
        }
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

}
