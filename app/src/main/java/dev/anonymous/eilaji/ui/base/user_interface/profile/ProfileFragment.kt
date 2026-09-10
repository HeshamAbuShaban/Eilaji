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
import dev.anonymous.eilaji.network.ApiResponse
import dev.anonymous.eilaji.network.NetworkModule
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.main.MainActivity
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.ui.other.dialogs.LogoutDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.LogoutDialogFragment.LogoutDialogListener
import dev.anonymous.eilaji.utils.AppController
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
        fun updateChecks(theme: String) {
            binding.checkLight.visibility = if (theme == AppSharedPreferences.Theme.light.name) View.VISIBLE else View.GONE
            binding.checkDark.visibility = if (theme == AppSharedPreferences.Theme.dark.name) View.VISIBLE else View.GONE
            binding.checkSystem.visibility = if (theme == AppSharedPreferences.Theme.system.name) View.VISIBLE else View.GONE
        }
        updateChecks(prefs.getTheme())
        fun selectTheme(theme: String) {
            if (theme == prefs.getTheme()) return
            prefs.putTheme(theme)
            AppController.applyTheme(theme)
            updateChecks(theme)
            requireActivity().recreate()
        }
        binding.buThemeLight.setOnClickListener { selectTheme(AppSharedPreferences.Theme.light.name) }
        binding.buThemeDark.setOnClickListener { selectTheme(AppSharedPreferences.Theme.dark.name) }
        binding.buThemeSystem.setOnClickListener { selectTheme(AppSharedPreferences.Theme.system.name) }
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
                try {
                    val uri = Uri.parse("market://details?id=${requireContext().packageName}")
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (_: Exception) {
                    val url = "https://play.google.com/store/apps/details?id=${requireContext().packageName}"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
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
