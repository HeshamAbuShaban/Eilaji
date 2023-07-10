package dev.anonymous.eilaji.ui.base.user_interface.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dev.anonymous.eilaji.adapters.AdsAdapter
import dev.anonymous.eilaji.adapters.CategoriesPharmaceuticalsAdapter
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.databinding.FragmentHomeBinding
import dev.anonymous.eilaji.models.server.Ad
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.storage.enums.CollectionNames
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.utils.DepthPageTransformer
import dev.anonymous.eilaji.utils.DummyData

class HomeFragment : Fragment() {
    // #-Firebase
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // AdsReference
    private val adsRef = db.collection(CollectionNames.Ad.collection_name)
    private var adsListenerRegistration: ListenerRegistration? = null

//    // PharmacyReference
//    private val pharmacyRef = db.collection(CollectionNames.Pharmacy.collection_name)

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var _binding: FragmentHomeBinding
    // private late-init var homeViewModel: HomeViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        // init the viewModel
        homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        /*setupAdsPager()*/
        fetchAds() // now the setup AdpVP works if the data arrived from server

        setupCategoriesPharmaceuticalsRecycler()
//        setupBestSellerRecycler()
    }

    override fun onStart() {
        super.onStart()
        // run the ads listeners ,And do not forget to stop it
        adsFetcherListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {

        binding.editText.setOnTouchListener { _, _ ->
            val intent = Intent(requireContext(), AlternativesActivity::class.java)
            intent.putExtra("fragmentType", FragmentsKeys.search.name) // Set the fragment type as "search" or "map"
            startActivity(intent)
            false
        }

        // testing for the medicine fragment
        binding.buShowAllBestSeller.setOnClickListener {
            val intent = Intent(requireContext(), AlternativesActivity::class.java)
            intent.putExtra(
                "fragmentType",
                FragmentsKeys.medicine.name
            ) // Set the fragment type as "search" or "map"
            startActivity(intent)
        }

    }

    private fun setupAdsPager(adsList: ArrayList<Ad>) {
        with(binding.pagerAds) {
            adapter = AdsAdapter(adsList)
            setPageTransformer(DepthPageTransformer())
            binding.indicatorAds.setupViewPager2(this, 0)
        }
    }

    //this methods gets the ads from server
    private fun fetchAds() {
        val adList: ArrayList<Ad> = ArrayList()
        adsRef.get().addOnSuccessListener { querySnapshot ->
            for (documentSnapshot in querySnapshot) {
                val ad = documentSnapshot.toObject(Ad::class.java)
                adList.add(ad)
            }
            // send the data to the container
            homeViewModel.setAdsList(adList)
            // setup the viewPager with data
            homeViewModel.adsList.observe(viewLifecycleOwner) {
                if (it != null) {
                    setupAdsPager(it)
                }
            }
        }.addOnFailureListener { exception ->
            Log.e("HomeFragment", "fetchAds: exc", exception)
            Log.d("HomeFragment", "fetchAds: massage" + exception.localizedMessage)
        }
    }

    private fun adsFetcherListener() {
        adsListenerRegistration = adsRef.addSnapshotListener { querySnapshot, e ->
            if (e != null) {
                Log.e("HomeFragment", "fetchAds: Error", e)
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                val adList: ArrayList<Ad> = ArrayList()
                for (documentSnapshot in querySnapshot) {
                    val ad = documentSnapshot.toObject(Ad::class.java)
                    adList.add(ad)
                }
                homeViewModel.setAdsList(adList)
            } else {
                // Handle the case where the querySnapshot is null
                // (e.g., show a message to the user or handle the absence of data)
            }
        }
    }


    private fun setupCategoriesPharmaceuticalsRecycler() {
        with(binding.recyclerCategoriesPharmaceuticals) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = CategoriesPharmaceuticalsAdapter(DummyData.listCategoriesPharmaceuticalModels)
        }
    }

    private fun fetchPharmaceuticals() {

    }

    private fun setupBestSellerRecycler(medicineList: ArrayList<Medicine>) {
        with(binding.recyclerBestSeller) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = MedicinesAdapter(medicineList)
        }
    }

    private fun fetchBestSellerMedicines() {

    }

    override fun onStop() {
        super.onStop()
        removeAdsListeners()
    }

    private fun removeAdsListeners() {
        adsListenerRegistration?.remove()
        adsListenerRegistration = null
    }
}



