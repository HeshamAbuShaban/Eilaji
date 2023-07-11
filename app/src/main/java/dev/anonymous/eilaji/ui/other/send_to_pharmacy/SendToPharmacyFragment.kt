package dev.anonymous.eilaji.ui.other.send_to_pharmacy

import android.location.Location
import android.os.Bundle
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
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.models.Pharmacy
import java.util.Collections

class SendToPharmacyFragment : Fragment() {
    private lateinit var viewModel: SendToPharmacyViewModel

    private lateinit var _binding: FragmentSendToPharmacyBinding
    private val binding get() = _binding

    private val firebaseController = FirebaseController.getInstance()

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

            firebaseController.getPharmacies(
                onTaskSuccessful = {
                    val myLocation = Location("my location")
                    myLocation.latitude = args.lat.toDouble()
                    myLocation.longitude = args.lng.toDouble()

                    Collections.sort(it, Comparator { ph1, ph2 ->
                        val locationA = Location("pharmacy 1")
                        locationA.latitude = ph1.lat
                        locationA.longitude = ph1.lng

                        val locationB = Location("pharmacy 2")
                        locationB.latitude = ph2.lat
                        locationB.longitude = ph2.lng

                        val distanceOne = myLocation.distanceTo(locationA)
                        val distanceTwo = myLocation.distanceTo(locationB)

                        return@Comparator Floats.compare(distanceOne, distanceTwo);
                    })

                    setupPharmaciesLocationRecycler(
                        it,
                        myLocation,
                        args.stringUri,
                        args.description
                    )
                },
                onTaskFailed = {
                    Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    //    intent.putExtra("stringUri", stringUri)
//    intent.putExtra("description", description)
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


}