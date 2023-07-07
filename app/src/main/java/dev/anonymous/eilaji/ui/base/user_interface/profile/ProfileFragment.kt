package dev.anonymous.eilaji.ui.base.user_interface.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dev.anonymous.eilaji.databinding.FragmentProfileBinding
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.ui.main.MainActivity
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.dialogs.LogoutDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.LogoutDialogFragment.LogoutDialogListener
import java.net.URLEncoder

class ProfileFragment : Fragment() ,LogoutDialogListener{
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
        with(binding){
            // SignOut
            buLogout.setOnClickListener {
                // show an alert to notify user of the logout action
                LogoutDialogFragment().show(childFragmentManager,"LogoutTriggered")
            }
            // change-password need to be fixed
            buChangePassword.setOnClickListener {
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.putExtra("fragmentType", FragmentsKeys.changePassword.name)
                startActivity(intent)
            }
            // check-for-favorite items
            buFavorites.setOnClickListener {
                val intent = Intent(requireContext(), AlternativesActivity::class.java)
                intent.putExtra("fragmentType", FragmentsKeys.favorites.name)
                startActivity(intent)
            }
            // rate temp
            buRateApp.setOnClickListener {
                val query = "Leo Messi"
                val url = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            // map temp
            buAppMap.setOnClickListener {
                val intent = Intent(requireContext(), AlternativesActivity::class.java)
                intent.putExtra("fragmentType", FragmentsKeys.map.name)
                startActivity(intent)
            }

            // connect with us temp
            buConnectWithUs.setOnClickListener {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:eilaji.health@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Connect with us")
                }
                startActivity(emailIntent)
            }
            // etc..
        }

    }
    // logout inside of dialog that shows when user clicked on bnLogout in the fragment
    override fun onLogoutClicked() {
        //first logout from firebase server
        firebaseController.signOut(requireContext())
        // pop the current screen from the back stack
        requireActivity().finish()
        // move to the login screen
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("logoutTrigger",
            FragmentsKeys.logout.name)
        startActivity(intent)
    }
}