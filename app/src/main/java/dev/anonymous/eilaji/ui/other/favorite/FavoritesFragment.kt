package dev.anonymous.eilaji.ui.other.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.anonymous.eilaji.databinding.FragmentFavoritesBinding
class FavoritesFragment : Fragment() {
    private lateinit var _binding: FragmentFavoritesBinding
    private val binding get() = _binding
    private lateinit var favoriteViewModel: FavoriteViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?, ): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        setupVMComponent()
        return binding.root
    }

    private fun setupVMComponent() {
        favoriteViewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]
        favoriteViewModel.setBindingObj(binding)
        favoriteViewModel.setToolBarTitle(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoriteViewModel.setupFavoritesRecycler(requireActivity())
    }
}