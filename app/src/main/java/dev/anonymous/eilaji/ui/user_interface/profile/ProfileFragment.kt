package dev.anonymous.eilaji.ui.user_interface.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.anonymous.eilaji.databinding.FragmentProfileBinding
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.ui.main.MainActivity
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.utils.FragmentsKeys

class ProfileFragment : Fragment() {
    private lateinit var _binding: FragmentProfileBinding
    private val binding get() = _binding
    private val firebaseController = FirebaseController.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        // SignOut
        binding.buLogout.setOnClickListener {
            //first logout from firebase server
            firebaseController.signOut()
            // pop the current screen from the back stack
            requireActivity().finish()
            // move to the login screen
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.putExtra("logoutTrigger",FragmentsKeys.logout.name)
            startActivity(intent)
        }
        // change-password

        // check-for-favorite items
        binding.buFavorites.setOnClickListener {
            val intent = Intent(requireContext(), AlternativesActivity::class.java)
            intent.putExtra("fragmentType", FragmentsKeys.favorites.name)
            startActivity(intent)
        }
        // etc..
    }
}