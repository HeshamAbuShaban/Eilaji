package dev.anonymous.eilaji.ui.other.favorite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.anonymous.eilaji.databinding.FragmentFavoritesBinding

class FavoritesFragment : Fragment() {
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoriteViewModel: FavoriteViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        setupVMComponent()
        return binding.root
    }

    private fun setupVMComponent() {
        favoriteViewModel = ViewModelProvider(this)[FavoriteViewModel::class.java]
        favoriteViewModel.setBinding(_binding)
        favoriteViewModel.setToolBarTitle(requireContext())
        favoriteViewModel.initRepo(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        favoriteViewModel.setupFavoritesRecycler(requireActivity())
        favoriteViewModel.reloadFavorites(requireContext().applicationContext)
        favoriteViewModel.favoritesLive(requireContext()).observe(viewLifecycleOwner) { list ->
            favoriteViewModel.updateList(list)
        }
        favoriteViewModel.syncOnStart()
    }

    override fun onResume() {
        super.onResume()
        favoriteViewModel.refreshFromNetwork()
    }

    override fun onDestroyView() {
        // The view model outlives the view; drop the reference so it cannot point at a dead tree.
        favoriteViewModel.setBinding(null)
        _binding = null
        super.onDestroyView()
    }
}
