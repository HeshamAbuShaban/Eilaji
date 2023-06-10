package dev.anonymous.eilaji.ui.user_interface.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.AdsAdapter
import dev.anonymous.eilaji.adapters.CategoriesPharmaceuticalsAdapter
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.databinding.FragmentHomeBinding
import dev.anonymous.eilaji.utils.DepthPageTransformer
import dev.anonymous.eilaji.utils.DummyData

class HomeFragment : Fragment(), OnMapReadyCallback {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var map: GoogleMap

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        setupMapFragment()
        setupViewModel()
//        setupListeners()
        observeTextChanges()
        setupAdsPager()
        setupCategoriesPharmaceuticalsRecycler()
        setupBestSellerRecycler()
    }

    private fun setupAdsPager() {
        with(binding.pagerAds) {
            adapter = AdsAdapter(DummyData.listAdModels)
            setPageTransformer(DepthPageTransformer())
            binding.indicatorAds.setupViewPager2(this, 0)
        }
    }

    private fun setupCategoriesPharmaceuticalsRecycler() {
        with(binding.recyclerCategoriesPharmaceuticals) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = CategoriesPharmaceuticalsAdapter(DummyData.listCategoriesPharmaceuticalModels)
        }
    }

    private fun setupBestSellerRecycler() {
        with(binding.recyclerBestSeller) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = MedicinesAdapter(DummyData.listMedicineModels)
        }
    }

    private fun setupMapFragment() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun setupViewModel() {
//        homeViewModel.text.observe(viewLifecycleOwner, Observer { text ->
//            binding.textHome.text = text
//        })
    }

    private fun observeTextChanges() {
//        homeViewModel.text.observe(viewLifecycleOwner) { text ->
//            binding.textHome.text = text
//        }
    }

//    private fun setupListeners() {
//        binding.tempSignOut.setOnClickListener {
//            FirebaseController.getInstance().signOut()
//            requireActivity().finish()
//        }
//    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        updateMarkerOptions()
    }

    private fun updateMarkerOptions() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    val marker = MarkerOptions().position(currentLatLng).title("موقع المنزل")
                    map.addMarker(marker)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}




