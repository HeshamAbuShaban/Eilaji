package dev.anonymous.eilaji.ui.base.user_interface.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentProfileBinding
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.main.MainActivity
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.ui.other.dialogs.LogoutDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.LogoutDialogFragment.LogoutDialogListener
import dev.anonymous.eilaji.utils.AppController
import java.net.URLEncoder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment(), LogoutDialogListener {
    private lateinit var _binding: FragmentProfileBinding
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupThemeSelector()
        setupListeners()
    }

    private fun setupThemeSelector() {
        val prefs = AppSharedPreferences.getInstance(requireContext())
        when (prefs.getTheme()) {
            AppSharedPreferences.Theme.light.name -> binding.rbLight.isChecked = true
            AppSharedPreferences.Theme.dark.name -> binding.rbDark.isChecked = true
            else -> binding.rbSystem.isChecked = true
        }
        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.rbLight -> AppSharedPreferences.Theme.light.name
                R.id.rbDark -> AppSharedPreferences.Theme.dark.name
                else -> AppSharedPreferences.Theme.system.name
            }
            if (selected != prefs.getTheme()) {
                prefs.putTheme(selected)
                AppController.applyTheme(selected)
                requireActivity().recreate()
            }
        }
    }

    private fun setupListeners() {
        with(binding) {
            buLogout.setOnClickListener {
                LogoutDialogFragment().show(childFragmentManager, "LogoutTriggered")
            }
            buChangePassword.setOnClickListener {
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.putExtra("fragmentType", FragmentsKeys.changePassword.name)
                startActivity(intent)
            }
            buFavorites.setOnClickListener {
                val intent = Intent(requireContext(), AlternativesActivity::class.java)
                intent.putExtra("fragmentType", FragmentsKeys.favorites.name)
                startActivity(intent)
            }
            buRateApp.setOnClickListener {
                val query = "Leo Messi"
                val url = "https://www.google.com/search?q=${URLEncoder.encode(query, "UTF-8")}"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
            buAppMap.setOnClickListener {
                val intent = Intent(requireContext(), AlternativesActivity::class.java)
                intent.putExtra("fragmentType", FragmentsKeys.add_address.name)
                startActivity(intent)
            }
            buConnectWithUs.setOnClickListener {
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:eilaji.health@gmail.com")
                    putExtra(Intent.EXTRA_SUBJECT, "Connect with us")
                }
                startActivity(emailIntent)
            }
        }
    }

    override fun onLogoutClicked() {
        val prefs = AppSharedPreferences.getInstance(requireContext())
        val api = NetworkModule.provideApiService(requireContext())
        api.logout().enqueue(object : Callback<ApiResponse<Any>> {
            override fun onResponse(call: Call<ApiResponse<Any>>, response: Response<ApiResponse<Any>>) {
                prefs.clearAll()
                try {
                    FirebaseController.getInstance().signOut(requireContext())
                } catch (_: Exception) {
                }
                navigateToLogin()
            }

            override fun onFailure(call: Call<ApiResponse<Any>>, t: Throwable) {
                prefs.clearAll()
                try {
                    FirebaseController.getInstance().signOut(requireContext())
                } catch (_: Exception) {
                }
                navigateToLogin()
            }
        })
    }

    private fun navigateToLogin() {
        requireActivity().finish()
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("logoutTrigger", FragmentsKeys.logout.name)
        startActivity(intent)
    }
}
