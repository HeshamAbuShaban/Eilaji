package dev.anonymous.eilaji.ui.other.map

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
import androidx.navigation.findNavController
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
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment.RequestPermissionsListener
import dev.anonymous.eilaji.utils.DummyData


class MapFragment : Fragment(), OnMapReadyCallback, RequestPermissionsListener {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var _binding: FragmentMapBinding
    private val binding get() = _binding

    private lateinit var mapViewModel: MapViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(layoutInflater)
        // Fix => Unable to update local snapshot for com.google.android.libraries.consentverifier
        // Fix => Unable to update local snapshot for com.google.android.libraries.conservativeness
        // Fix => set_timerslack_ns write failed: Operation not permitted
        MapsInitializer.initialize(
            requireContext(),
            MapsInitializer.Renderer.LATEST
        ) { _: MapsInitializer.Renderer? -> }
        setupVMComponent()
        return binding.root
    }

    private fun setupVMComponent() {
        // Initialize the view model
        mapViewModel = ViewModelProvider(this)[MapViewModel::class.java]
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                // Check if all permissions are granted
                val allGranted = permissions.all { it.value }
                if (allGranted) {
                    // All permissions granted, you can proceed with sending notifications
                    Toast.makeText(
                        requireContext(),
                        "Great Now you are all set to use The Reminder",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Permission denied, handle accordingly
                    // At least one permission denied, handle accordingly (e.g., show a message or disable certain features)
                    Toast.makeText(
                        requireContext(),
                        "Permission denied. Cannot create reminder.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        mapViewModel.setRequestPermissionLauncher(requestPermissionLauncher)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mapViewModel.arePermissionsGranted()) {
            obtainGoogleMapInstance()
            observeCurrentLocation()
            setupAdsPager()
        } else {
            RequestPermissionsDialogFragment.newInstance("Please allow the map permission to be able to use the app properly")
                .show(childFragmentManager, "MapPermissions")
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        // Set the GoogleMap instance in the view model
        mapViewModel.setMap(googleMap)

        // this gets the Location and sends it to the viewModel to be used
        mapViewModel.getUserLastLocation()

        // Hesham:?> Provide Some Context of what dose this block of code do ?
        googleMap.setOnMarkerClickListener { marker ->
            val indexPharmacies = DummyData.listPharmaciesModels.indexOfFirst {
                marker.position.latitude == it.lat && marker.position.longitude == it.lng
            }

            if (indexPharmacies != -1) {
                binding.pharmaciesLocationsPager.currentItem = indexPharmacies
                mapViewModel.animateCameraToPosition(
                    LatLng(
                        DummyData.listPharmaciesModels[indexPharmacies].lat,
                        DummyData.listPharmaciesModels[indexPharmacies].lng
                    ),
                    17f
                )
            }

            true
        }

        /*// This Is Just a test
        mapViewModel.moveCameraToPosition(
            LatLng(31.449624242321853, 34.39510808345772),
            17f
        )*/

        // Nice Trying to show a Marker for a List of data
        DummyData.listPharmaciesModels.forEach {
            mapViewModel.addMarkerToMap(
                markerOptions(
                    latLng = LatLng(it.lat, it.lng),
                    title = it.pharmacy_name,
                    getMarkerIconFromDrawable(requireContext(), R.drawable.ic_hospital)
                )
            )
        }
    }


    // +++++++++Initializes Method
    private fun obtainGoogleMapInstance() {
        // Obtain the GoogleMap instance from SupportMapFragment
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /*locate the user location*/
    private fun observeCurrentLocation() {
        mapViewModel.currentLocation.observe(viewLifecycleOwner) { currentLatLng ->
            currentLatLng?.let {
                // Move the camera to the current location
                mapViewModel.animateCameraToPosition(currentLatLng, 17f)
                // Add a marker to the current location
                mapViewModel.addMarkerToMap(
                    markerOptions(latLng = currentLatLng, title = "Home Location")
                )
            }
        }
    }

    // +++++++++Setup AdsPager
    private fun setupAdsPager() {
        var scrollIsDragging = false
        with(binding.pharmaciesLocationsPager) {
            adapter = PharmaciesLocationsAdapter(
                DummyData.listPharmaciesModels,
                navigateToChat = {
                    val action = MapFragmentDirections.actionNavigationMapToNavigationMessaging(
                        null,
                        it.uid,
                        it.pharmacy_name,
                        it.pharmacy_image_url,
                        it.token,
                        null,
                        null
                    )
                    findNavController().navigate(action)
                }
            )

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
                        mapViewModel.animateCameraToPosition(
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

    // +++++++++Utilities for design
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

    // Permission Interface callbacks
    override fun onAllowClicked() {
        mapViewModel.requestPermissions()
    }

    override fun onDenyClicked() {
        Toast.makeText(
            requireContext(),
            getString(R.string.permissions_message_sorry_you_can_not),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapViewModel.currentLocation.removeObservers(viewLifecycleOwner)
    }

}
