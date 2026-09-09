package dev.anonymous.eilaji.ui.other.send_to_pharmacy

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.common.primitives.Floats
import dev.anonymous.eilaji.adapters.server.SendToPharmacyAdapter
import dev.anonymous.eilaji.databinding.FragmentSendToPharmacyBinding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.network.PaginatedResult
import dev.anonymous.eilaji.network.PharmacyDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections

class SendToPharmacyFragment : Fragment() {
    private lateinit var viewModel: SendToPharmacyViewModel

    private lateinit var _binding: FragmentSendToPharmacyBinding
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSendToPharmacyBinding.inflate(inflater, container, false)
        binding.includeAppBarLayoutSendToPharmacy.toolbarApp.title = "الصيدليات بحسب القرب"
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            val args = SendToPharmacyFragmentArgs.fromBundle(arguments)
            fetchPharmacies(args.lat.toDouble(), args.lng.toDouble(), args.stringUri, args.description)
        }
    }

    private fun fetchPharmacies(lat: Double, lng: Double, stringUri: String?, description: String?) {
        val apiService = NetworkModule.provideApiService(requireContext())
        val myLocation = Location("my location").apply {
            latitude = lat
            longitude = lng
        }
        apiService.getNearbyPharmacies(lat = lat, lng = lng, radius = 10.0).enqueue(object : Callback<ApiResponse<List<PharmacyDto>>> {
            override fun onResponse(call: Call<ApiResponse<List<PharmacyDto>>>, response: Response<ApiResponse<List<PharmacyDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val dtos = response.body()?.data ?: emptyList()
                    if (dtos.isNotEmpty()) {
                        handlePharmacies(dtos.map { it.toPharmacy() }, myLocation, stringUri, description)
                    } else {
                        fetchAllPharmaciesFallback(myLocation, stringUri, description)
                    }
                } else {
                    Log.e("SendToPharmacy", "nearby failed: ${response.body()?.error}")
                    fetchAllPharmaciesFallback(myLocation, stringUri, description)
                }
            }
            override fun onFailure(call: Call<ApiResponse<List<PharmacyDto>>>, t: Throwable) {
                Log.e("SendToPharmacy", "nearby network error", t)
                fetchAllPharmaciesFallback(myLocation, stringUri, description)
            }
        })
    }

    private fun fetchAllPharmaciesFallback(myLocation: Location, stringUri: String?, description: String?) {
        val apiService = NetworkModule.provideApiService(requireContext())
        apiService.getPharmacies(page = 0, pageSize = 50).enqueue(object : Callback<ApiResponse<PaginatedResult<PharmacyDto>>> {
            override fun onResponse(call: Call<ApiResponse<PaginatedResult<PharmacyDto>>>, response: Response<ApiResponse<PaginatedResult<PharmacyDto>>>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val dtos = response.body()?.data?.items ?: emptyList()
                    handlePharmacies(dtos.map { it.toPharmacy() }, myLocation, stringUri, description)
                } else {
                    val msg = response.body()?.error ?: "Failed to load pharmacies"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ApiResponse<PaginatedResult<PharmacyDto>>>, t: Throwable) {
                Toast.makeText(requireContext(), "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun handlePharmacies(pharmacies: List<Pharmacy>, myLocation: Location, stringUri: String?, description: String?) {
        val sorted = ArrayList(pharmacies)
        Collections.sort(sorted, Comparator { ph1, ph2 ->
            val locationA = Location("pharmacy 1").apply {
                latitude = ph1.lat
                longitude = ph1.lng
            }
            val locationB = Location("pharmacy 2").apply {
                latitude = ph2.lat
                longitude = ph2.lng
            }
            val distanceOne = myLocation.distanceTo(locationA)
            val distanceTwo = myLocation.distanceTo(locationB)
            return@Comparator Floats.compare(distanceOne, distanceTwo)
        })
        setupPharmaciesLocationRecycler(sorted, myLocation, stringUri, description)
    }

    private fun setupPharmaciesLocationRecycler(
        pharmacies: ArrayList<Pharmacy>,
        myLocation: Location,
        stringUri: String?,
        description: String?
    ) {
        with(binding.recyclerPharmaciesLocation) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity)
            adapter = SendToPharmacyAdapter(pharmacies, myLocation) {
                findNavController().navigate(
                    SendToPharmacyFragmentDirections
                        .actionNavigationSendToPharmacyFragmentToNavigationMessaging(
                            null,
                            it.uid,
                            it.pharmacy_name,
                            it.pharmacy_image_url,
                            it.token,
                            stringUri,
                            description
                        )
                )
            }
        }
    }

    private fun PharmacyDto.toPharmacy(): Pharmacy {
        return Pharmacy(
            uid = id,
            pharmacy_image_url = imageUrl ?: "",
            pharmacy_name = name,
            phone = phone ?: "",
            address = address,
            lat = latitude,
            lng = longitude,
            token = ""
        )
    }
}
