package dev.anonymous.eilaji.ui.base.user_interface.categories

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import dev.anonymous.eilaji.adapters.server.CategoryAdapter
import dev.anonymous.eilaji.databinding.FragmentCategoriesBinding
import dev.anonymous.eilaji.models.server.Category
import dev.anonymous.eilaji.storage.enums.CollectionNames
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity

class CategoriesFragment : Fragment() {
    // #-Firebase
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // CategoriesReference
    private val categoriesRef = db.collection(CollectionNames.Category.collection_name)
//    private var adsListenerRegistration: ListenerRegistration? = null


    private lateinit var _binding: FragmentCategoriesBinding
    private val binding get() = _binding

    private lateinit var categoriesViewModel: CategoriesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        categoriesViewModel = ViewModelProvider(this)[CategoriesViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /*setupPharmacyDepartmentsRecycler()*/
        fetchCategories() // now its from the server
        displayCategories()
    }

    private fun displayCategories() {
        // setup the viewPager with data
        categoriesViewModel.categoryList.observe(viewLifecycleOwner) {
            setupPharmacyDepartmentsRecycler(it)
        }
    }

    private fun setupPharmacyDepartmentsRecycler(categoryList: ArrayList<Category>) {
        with(binding.recyclerPharmacyDepartments) {
            setHasFixedSize(false)
            layoutManager = GridLayoutManager(activity, 2)
            adapter = CategoryAdapter(categoryList) { categoryId , categoryTitle->
                navToSubCategories(categoryId, categoryTitle)
            }
        }
    }

    private fun navToSubCategories(argCategoryId: String,argCategoryTitle :String) {
        val intent = Intent(requireContext(), AlternativesActivity::class.java)
        intent.putExtra("fragmentType", FragmentsKeys.subCategories.name)
        intent.putExtra("categoryId", argCategoryId)
        intent.putExtra("categoryTitle", argCategoryTitle)
        startActivity(intent)
    }

    private fun fetchCategories() {
        val categoryList: ArrayList<Category> = ArrayList()
        categoriesRef.get().addOnSuccessListener { querySnapshot ->
            for (documentSnapshot in querySnapshot) {
                val category = documentSnapshot.toObject(Category::class.java)
                categoryList.add(category)
            }
            // send the data to the container
            categoriesViewModel.setCategoryList(categoryList)
        }.addOnFailureListener { exception ->
            Log.e("CategoriesFragment", "fetchCate: exc", exception)
            Log.d("CategoriesFragment", "fetchCate: massage" + exception.localizedMessage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeListeners()
    }

    private fun removeListeners() {
        categoriesViewModel.categoryList.removeObservers(viewLifecycleOwner)
    }

}