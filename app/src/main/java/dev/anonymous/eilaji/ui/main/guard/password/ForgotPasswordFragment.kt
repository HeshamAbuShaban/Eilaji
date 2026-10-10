package dev.anonymous.eilaji.ui.main.guard.password

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentForgotPasswordBinding
import dev.anonymous.eilaji.network.ApiService
import dev.anonymous.eilaji.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Asks the backend for a password reset code (POST /api/v1/auth/forgot-password).
 *
 * The code is shown until an email/SMS provider is configured on the backend, so this screen
 * stays useful for support calls without depending on Firebase.
 */
class ForgotPasswordFragment : Fragment() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val apiService: ApiService by lazy {
        NetworkModule.provideApiService(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        val navController = findNavController()
        with(binding) {
            buSend.setOnClickListener {
                val email = edEmail.text.toString().trim()
                when {
                    email.isEmpty() -> Snackbar.make(
                        root, getString(R.string.enter_email), Snackbar.LENGTH_LONG
                    ).show()

                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> Snackbar.make(
                        root, getString(R.string.invalid_email), Snackbar.LENGTH_LONG
                    ).show()

                    else -> requestResetCode(email, navController)
                }
            }
        }
    }

    private fun requestResetCode(email: String, navController: androidx.navigation.NavController) {
        hideKeyboard()
        binding.buSend.isEnabled = false
        scope.launch {
            var message = ""
            var sent = false
            try {
                val response = apiService.forgotPassword(mapOf("email" to email)).execute()
                val body = response.body()
                if (response.isSuccessful && body?.success == true) {
                    sent = true
                    // Until the backend delivers the code by email/SMS, show it on screen.
                    val code = body.data?.get("resetToken").orEmpty()
                    message = if (code.isBlank()) {
                        getString(R.string.reset_code_sent_hint)
                    } else {
                        getString(R.string.reset_code_display, code)
                    }
                } else {
                    message = body?.error ?: getString(R.string.reset_request_failed)
                }
            } catch (_: Exception) {
                message = getString(R.string.network_error)
            } finally {
                binding.buSend.isEnabled = true
                val root = view ?: return@launch
                Snackbar.make(root, message, if (sent) Snackbar.LENGTH_LONG else Snackbar.LENGTH_LONG).show()
                if (sent) {
                    navController.popBackStack()
                }
            }
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        } catch (_: Exception) {
        }
    }

    // TODO(Under Testing)
    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        scope.cancel()
        _binding = null
        super.onDestroyView()
    }
}
