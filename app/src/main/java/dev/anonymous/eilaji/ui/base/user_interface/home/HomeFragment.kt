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
import dev.anonymous.eilaji.adapters.AdsAdapter
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.databinding.FragmentHomeBinding
import dev.anonymous.eilaji.models.server.Ad
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.storage.enums.CollectionNames
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.utils.DepthPageTransformer

class HomeFragment : Fragment() {
    companion object {
        private const val TAG = "HomeFragment"
    }

    // Firebase FireStore
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // AdsReference
    private val adsRef = db.collection(CollectionNames.Ad.collection_name)
//    private var adsListenerRegistration: ListenerRegistration? = null

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var _binding: FragmentHomeBinding

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

        // display
        displayAds()
        setupCategoriesPharmaceuticalsRecycler()
//        setupBestSellerRecycler()
    }

    /*override fun onStart() {
        super.onStart()
        // run the ads listeners ,And do not forget to stop it
        adsFetcherListener()
    }*/

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {

        binding.editText.setOnTouchListener { _, _ ->
            val intent = Intent(requireContext(), AlternativesActivity::class.java)
            intent.putExtra(
                "fragmentType",
                FragmentsKeys.search.name
            ) // Set the fragment type as "search" or "map"
            startActivity(intent)
            return@setOnTouchListener false
        }

        // testing for the medicine fragment
        binding.buShowAllBestSeller.setOnClickListener {
            val intent = Intent(requireContext(), AlternativesActivity::class.java)
            intent.putExtra("fragmentType", FragmentsKeys.medicine.name)
            startActivity(intent)
        }

    }

    private fun displayAds() {
        homeViewModel.adsList.observe(viewLifecycleOwner) { adsList ->
            if (adsList != null) {
                setupAdsPager(adsList)
            }
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
        adsRef.get().addOnSuccessListener { querySnapshot ->
            val adList: ArrayList<Ad> = ArrayList()
            for (documentSnapshot in querySnapshot) {
                val ad = documentSnapshot.toObject(Ad::class.java)
                adList.add(ad)
            }
            homeViewModel.setAdsList(adList)
        }.addOnFailureListener { exception ->
            Log.e("HomeFragment", "fetchAds: exc", exception)
            Log.d("HomeFragment", "fetchAds: massage" + exception.localizedMessage)
        }
    }


    /*private fun adsFetcherListener() {
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
    }*/


    private fun setupCategoriesPharmaceuticalsRecycler() {
        with(binding.recyclerCategoriesPharmaceuticals) {
//            setHasFixedSize(false)
//            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
//            adapter = SubCategoriesAdapter(DummyData.listCategoriesPharmaceuticalModels)
        }
    }

    // get the  ("Medicines") Categories
    private fun fetchPharmaceuticals() {
        val categoryID = "Y5JJJYQykkCbaxy7ZOa4"

        // Query the SubCategories collection to filter based on the category ID
        val subCategoriesRef = FirebaseFirestore.getInstance().collection("SubCategories")
        val subCategoriesQuery = subCategoriesRef.whereEqualTo("idCategory", categoryID)

        subCategoriesQuery.get()
            .addOnSuccessListener { subCategoriesQuerySnapshot ->
                // Process the filtered subcategories
                val subCategoryIDs = subCategoriesQuerySnapshot.documents.map { it.id }

                // Query the Medicines collection to filter based on the filtered subcategory IDs
                val medicinesRef = FirebaseFirestore.getInstance().collection("Medicines")
                val medicinesQuery = medicinesRef.whereIn("idSubCategory", subCategoryIDs)

                medicinesQuery.get()
                    .addOnSuccessListener { medicinesQuerySnapshot ->
                        // Process the filtered medicines
                        for (documentSnapshot in medicinesQuerySnapshot) {
                            val medicine = documentSnapshot.toObject(Medicine::class.java)
                            // Handle each medicine as needed
                        }
                    }
                    .addOnFailureListener { exception ->
                        // Handle any errors that occurred during the query for medicines
                        Log.e(TAG, "fetchPharmaceuticals: e", exception)
                    }
            }
            .addOnFailureListener { exception ->
                // Handle any errors that occurred during the query for subcategories
                Log.e(TAG, "fetchPharmaceuticals: ex", exception)
            }
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
        /*adsListenerRegistration?.remove()
        adsListenerRegistration = null*/
        homeViewModel.adsList.removeObservers(viewLifecycleOwner)
    }
}
