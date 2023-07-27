package dev.anonymous.eilaji.ui.other.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.fire-store.ListenerRegistration
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.adapters.PharmaciesLocationsAdapter
import dev.anonymous.eilaji.databinding.FragmentSearchBinding
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.storage.enums.CollectionNames

class SearchFragment : Fragment() {
    // #-Firebase
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // AdsReference
    private val medicinesRef = db.collection(CollectionNames.Medicine.collection_name)

    // PharmacyReference
    private val pharmaciesRef = db.collection(CollectionNames.Pharmacy.collection_name)

   /* // Listeners
    private var medicinesListenerRegistration: ListenerRegistration? = null
    private var pharmaciesListenerRegistration: ListenerRegistration? = null*/

    private lateinit var _binding: FragmentSearchBinding
    private val binding get() = _binding

    private lateinit var searchViewModel: SearchViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSearchBinding.inflate(layoutInflater)
        searchViewModel = ViewModelProvider(this)[SearchViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //get the medicines and set it into the recV
        fetchMedicinesData()
        displayMedicines()
        //get the pharmacies and set it into the recV
        fetchPharmaciesData()
        displayPharmacies()
    }

    /*override fun onStart() {
        super.onStart()
        medicinesFetcherListener()
        pharmaciesFetcherListener()
    }*/

    /*private fun medicinesFetcherListener() {
        medicinesListenerRegistration = medicinesRef.addSnapshotListener { querySnapshot, e ->
            if (e != null) {
                Log.e("SearchFragment", "fetch: Error", e)
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                val medicines: ArrayList<Medicine> = ArrayList()
                for (documentSnapshot in querySnapshot) {
                    val medicine = documentSnapshot.toObject(Medicine::class.java)
                    medicines.add(medicine)
                }
                searchViewModel.setMedicinesData(medicines)
            } else {
                // Handle the case where the querySnapshot is null
                // (e.g., show a message to the user or handle the absence of data)
            }
        }
    }

    private fun pharmaciesFetcherListener() {
        pharmaciesListenerRegistration = pharmaciesRef.addSnapshotListener { querySnapshot, e ->
            if (e != null) {
                Log.e("SearchFragment", "fetch: Error", e)
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                val pharmacies: ArrayList<Pharmacy> = ArrayList()
                for (documentSnapshot in querySnapshot) {
                    val pharmacy = documentSnapshot.toObject(Pharmacy::class.java)
                    pharmacies.add(pharmacy)
                }
                searchViewModel.setPharmaciesDataData(pharmacies)
            } else {
                // Handle the case where the querySnapshot is null
                // (e.g., show a message to the user or handle the absence of data)
            }
        }
    }*/

    private fun setupMedicinesAdapter(medicinesList: ArrayList<Medicine>) {
        with(binding.recVSearchMedicines) {
            adapter = MedicinesAdapter(medicinesList)
        }
    }

    private fun setupPharmaciesAdapter(pharmacy: ArrayList<Pharmacy>) {
        with(binding.recVSearchPharmacies) {
            adapter = PharmaciesLocationsAdapter(pharmacy,
                navigateToChat = {
                    val action = SearchFragmentDirections.actionNavigationSearchToNavigationMessaging(
                        null,
                        it.uid,
                        it.pharmacy_name,
                        it.pharmacy_image_url,
                        it.token,
                        null,
                        null
                    )
                    findNavController().navigate(action)
            })
        }
    }

    //.. get medicines
    private fun fetchMedicinesData() {
        val medicines: ArrayList<Medicine> = ArrayList()
        medicinesRef.get().addOnSuccessListener { querySnapshot ->
            for (documentSnapshot in querySnapshot) {
                val medicine = documentSnapshot.toObject(Medicine::class.java)
                medicines.add(medicine)
            }
            // send the data to the container
            searchViewModel.setMedicinesData(medicines)

        }.addOnFailureListener { exception ->
            Log.e("SF", "fetch: exc", exception)
            Log.d("SF", "fetch: massage" + exception.localizedMessage)
        }
    }
    private fun displayMedicines(){
        // setup the viewPager with data
        searchViewModel.medicineData.observe(viewLifecycleOwner) {
            setupMedicinesAdapter(it)
        }
    }
    //.. get pharmacies
    private fun fetchPharmaciesData() {
        val pharmacies: ArrayList<Pharmacy> = ArrayList()
        pharmaciesRef.get().addOnSuccessListener { querySnapshot ->
            for (documentSnapshot in querySnapshot) {
                val pharmacy = documentSnapshot.toObject(Pharmacy::class.java)
                pharmacies.add(pharmacy)
            }
            // send the data to the container
            searchViewModel.setPharmaciesDataData(pharmacies)
        }.addOnFailureListener { exception ->
            Log.e("SF", "fetch: exc", exception)
            Log.d("SF", "fetch: massage" + exception.localizedMessage)
        }
    }
    private fun displayPharmacies(){
        // setup the viewPager with data
        searchViewModel.pharmaciesData.observe(viewLifecycleOwner) {
            setupPharmaciesAdapter(it)
        }
    }

    /*override fun onStop() {
        super.onStop()
        removeMedicinesListener()
        removePharmaciesListener()
    }

    private fun removeMedicinesListener() {
        medicinesListenerRegistration?.remove()
        medicinesListenerRegistration = null
        searchViewModel.medicineData.removeObservers(viewLifecycleOwner)
    }

    private fun removePharmaciesListener() {
        pharmaciesListenerRegistration?.remove()
        pharmaciesListenerRegistration = null
        searchViewModel.pharmaciesData.removeObservers(viewLifecycleOwner)
    }*/

}