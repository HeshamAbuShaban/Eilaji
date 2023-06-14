package dev.anonymous.eilaji.ui.user_interface.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.adapters.AdsAdapter
import dev.anonymous.eilaji.adapters.CategoriesPharmaceuticalsAdapter
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.databinding.FragmentHomeBinding
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.utils.DepthPageTransformer
import dev.anonymous.eilaji.utils.DummyData

class HomeFragment : Fragment() {
    private lateinit var _binding: FragmentHomeBinding
    // private late-init var homeViewModel: HomeViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding
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
        setupListeners()
        setupAdsPager()
        setupCategoriesPharmaceuticalsRecycler()
        setupBestSellerRecycler()
    }

    private fun setupListeners(){
//        val activity = requireActivity()
//        val baseViewModel = ViewModelProvider(activity)[BaseViewModel::class.java]
//        val alternativesViewModel = ViewModelProvider(this)[AlternativesViewModel::class.java]
        /**
         *  When you retrieve the BaseViewModel using ViewModelProvider(activity), it checks if an instance of BaseViewModel already exists in the ViewModelStore associated with the activity. If an instance already exists, it returns that instance instead of creating a new one.
         *  The ViewModelProvider is designed to handle the lifecycle of view models and ensure that you get the same instance of the view model within the same scope (in this case, the activity scope). This allows you to share the same instance of BaseViewModel between the activity and its fragments.
         *  So, when you retrieve the BaseViewModel in the HomeFragment using ViewModelProvider(activity), it will provide you with the existing instance that was created in the activity, rather than creating a new instance.
         *
         */

        // this method to navigate the user when he clicks on the searchBar in the HomeFragment to
                // @package dev.anonymous.eilaji.ui.other.search
        binding.editText.setOnClickListener {
            val intent = Intent(requireContext(), AlternativesActivity::class.java)
            intent.putExtra("fragmentType", "search") // Set the fragment type as "search" or "map"
            startActivity(intent)

//            val navController = findNavController()
//            navController.navigate(R.id.navigation_search)

//            baseViewModel.navigateToSearchFragment()
//            alternativesViewModel.navigateToSearchFragment()
        }


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
}




