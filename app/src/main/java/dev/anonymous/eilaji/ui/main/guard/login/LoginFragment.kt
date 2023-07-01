package dev.anonymous.eilaji.ui.main.guard.login

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentLoginBinding
import dev.anonymous.eilaji.ui.base.BaseActivity

class LoginFragment : Fragment() {
    private lateinit var _binding: FragmentLoginBinding
    private lateinit var loginViewModel: LoginViewModel
    private val binding get() = _binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupListeners()
        observeLoginResult()
    }

    private fun setupViewModel() {
        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]
    }

    private fun setupListeners() {
        // this gets the navController from the host activity
        val navController = findNavController()
        binding.apply {
            buSignUp.setOnClickListener {
                navController.navigate(R.id.navigation_SignUp)
                /*@Deprecated
                    val singUpFragment = FragmentSignUp()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.mainActivityContainer, singUpFragment)
                        .commitAllowingStateLoss()*/
            }

            buLogin.setOnClickListener {
                val email = edEmail.text.toString()
                val password = edPassword.text.toString()

                if (checkData(email, password)) {
                    loginViewModel.login(email, password, requireActivity())
                } else {
                    showTextError(email, password)
                }
            }

            buForgotYourPassword.setOnClickListener {
                navController.navigate(R.id.navigation_forgotPassword)
            }
        }
    }

    private fun showTextError(email: String, password: String) {
        binding.apply {
            edEmail.error = if (email.isEmpty()) "Please Enter Your Email" else null
            edPassword.error = if (password.isEmpty()) "Please Enter Your Password" else null
        }
    }

    private fun checkData(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }

    private fun observeLoginResult() {
        loginViewModel.loginResult.observe(viewLifecycleOwner) { loginResult ->
            when (loginResult) {
                is LoginResult.Success -> {
                    // Handle successful login
                    //TODO HomeActivity >> Base
                    startActivity(Intent(requireContext(), BaseActivity::class.java))
                    requireActivity().finish()
                }

                is LoginResult.Error -> {
                    // Handle login error
                    showSnackBar(loginResult.errorMessage)
                }
            }
        }
    }


    private fun showSnackBar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

}