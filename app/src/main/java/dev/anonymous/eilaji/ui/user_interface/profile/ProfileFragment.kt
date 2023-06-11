package dev.anonymous.eilaji.ui.user_interface.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.anonymous.eilaji.databinding.FragmentProfileBinding
import dev.anonymous.eilaji.firebase.FirebaseController

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
            firebaseController.signOut()
            requireActivity().finish()
        }
        // change-password
        // check-for-favorite items
        // etc..
    }
}