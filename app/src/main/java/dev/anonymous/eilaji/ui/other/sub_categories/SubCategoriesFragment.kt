package dev.anonymous.eilaji.ui.other.sub_categories

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
import dev.anonymous.eilaji.adapters.MedicinesAdapter
import dev.anonymous.eilaji.adapters.SubCategoriesAdapter
import dev.anonymous.eilaji.databinding.FragmentSubCategoriesBinding
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.models.server.SubCategory
import dev.anonymous.eilaji.storage.enums.CollectionNames
import dev.anonymous.eilaji.utils.LoadingDialog


class SubCategoriesFragment : Fragment() {
    private lateinit var binding: FragmentSubCategoriesBinding
    private lateinit var viewModel: SubCategoriesViewModel

    //..Firebase
    // Firebase FireStore
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // AdsReference
    private val subCategoryCollection = db.collection(CollectionNames.SubCategory.collection_name)
    private val medicineCollection = db.collection(CollectionNames.Medicine.collection_name)

    // life of dialog tobe controlled
    private val loadingDialog = LoadingDialog()

    // values container
    private lateinit var categoryId: String
    private lateinit var categoryTitle: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog.show(childFragmentManager, "FetchingData")

        val arguments = arguments
        if (arguments != null) {
            val args = SubCategoriesFragmentArgs.fromBundle(arguments)
            categoryId = args.categoryId
            categoryTitle = args.categoryTitle

            //..loadingDialog.dismiss();
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSubCategoriesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[SubCategoriesViewModel::class.java]
        fetchMedicines()
        fetchSubCategories()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // set the toolbar
        binding.includeAppBarLayoutAlternatives.toolbarApp.title = categoryTitle

        // show list from server
        displaySubCategories()
        // show list from server
        displayMedicines()
        // dismiss the loading
        loadingDialog.dismiss()
    }

    private fun setupSubCategoriesRecycler(subCategoriesList: ArrayList<SubCategory>) {
        with(binding.recyclerSubCategories) {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false)
            adapter = SubCategoriesAdapter(subCategoriesList)
            // علشان يحذف الومضة تعت العنصر يلي تحدث
            itemAnimator = null
        }
    }

    private fun setupMedicinesAdapter(medicinesList: ArrayList<Medicine>) {
        with(binding.recSubCategoriesMedicines) {
            setHasFixedSize(false)
            adapter = MedicinesAdapter(medicinesList)
        }
    }


    private fun fetchSubCategories() {
        subCategoryCollection.get().addOnSuccessListener { querySnapshot ->
            val subCategoriesList: ArrayList<SubCategory> = ArrayList()
            for (documentSnapshot in querySnapshot) {
                val subCategories = documentSnapshot.toObject(SubCategory::class.java)
                subCategoriesList.add(subCategories)
            }
            viewModel.setSubCategoriesList(subCategoriesList)
            // Stop the Shimmer
//            removeAdsShimmer()
        }.addOnFailureListener { exception ->
            Log.e("SubCategoriesFragment", "fetchAds: exc", exception)
            Log.d("SubCategoriesFragment", "fetchAds: massage" + exception.localizedMessage)
        }
    }

    private fun fetchMedicines() {
        medicineCollection.get().addOnSuccessListener { querySnapshot ->
            val medicineList: ArrayList<Medicine> = ArrayList()
            for (documentSnapshot in querySnapshot) {
                val medicine = documentSnapshot.toObject(Medicine::class.java)
                medicineList.add(medicine)
            }
            viewModel.setMedicineList(medicineList)
            //Stop the Shimmer
//            removeAdsShimmer()
        }.addOnFailureListener { exception ->
            Log.e("SubCategoriesFragment", "fetchAds: exc", exception)
            Log.d("SubCategoriesFragment", "fetchAds: massage" + exception.localizedMessage)
        }
    }

    private fun displaySubCategories() {
        viewModel.subCategoriesList.observe(viewLifecycleOwner) {
            setupSubCategoriesRecycler(it)
        }
    }

    private fun displayMedicines() {
        viewModel.medicineList.observe(viewLifecycleOwner) {
            setupMedicinesAdapter(it)
        }
    }


//    binding.root.layoutParams.width = ConstraintLayout.LayoutParams.MATCH_PARENT

}