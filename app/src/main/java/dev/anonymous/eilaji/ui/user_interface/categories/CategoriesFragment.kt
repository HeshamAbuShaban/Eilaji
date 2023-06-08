package dev.anonymous.eilaji.ui.user_interface.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.adapters.EilajAdapter
import dev.anonymous.eilaji.storage.database.EilajViewModel


class CategoriesFragment : Fragment() {
    private lateinit var viewModel: EilajViewModel
    private lateinit var recyclerView: RecyclerView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Initialize the view for the root
        val view: View = inflater.inflate(R.layout.fragment_categories, container, false)
        recyclerView = view.findViewById(R.id.eilaj_recV)
        // Initialize the view model
        viewModel = ViewModelProvider(this)[EilajViewModel::class.java]
        // Inflate the layout for this fragment
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.allEilaj.observe(viewLifecycleOwner) { eilajList ->
            val eilajAdapter = EilajAdapter(eilajList)
            recyclerView.adapter = eilajAdapter
        }
    }

}