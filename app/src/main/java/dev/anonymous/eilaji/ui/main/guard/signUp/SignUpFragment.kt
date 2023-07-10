package dev.anonymous.eilaji.ui.main.guard.signUp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.databinding.FragmentSignUpBinding
import dev.anonymous.eilaji.storage.AppSharedPreferences
import dev.anonymous.eilaji.ui.base.BaseActivity
import dev.anonymous.eilaji.utils.AppController

class SignUpFragment : Fragment() {
    private lateinit var _binding: FragmentSignUpBinding
    private lateinit var signUpViewModel: SignUpViewModel
    private val binding get() = _binding

    private val preferences = AppSharedPreferences.getInstance(AppController.getInstance())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        performSignUp()
        observeSignUpResult()
        setupListeners()
    }

    private fun setupListeners() {
        binding.buSkip.setOnClickListener {
            startActivity(Intent(requireContext(), BaseActivity::class.java))
            requireActivity().finish()
        }

        binding.buLoginScreen.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    // TODO(Under Testing)
    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
    }

    private fun setupViewModel() {
        signUpViewModel = ViewModelProvider(this)[SignUpViewModel::class.java]
    }

    //this method have a ClickListener that performSignUp using the ViewModel call for register method and the Result are being controlled in the @#observeSignUpResult()
    private fun performSignUp() {
        binding.buCreateAnAccount.setOnClickListener {
            if (validateInputs()) {
                val fullName = binding.edFullName.text.toString()
                val email = binding.edEmail.text.toString()
                val password = binding.edPassword.text.toString()
                val token = preferences.token

                if (token == null) {
                    signUpViewModel.getToken()
                    signUpViewModel.token.observe(viewLifecycleOwner) {
                        preferences.putToken(it)
                        signUpViewModel.signUp(fullName, it, email, password)
                    }
                } else {
                    signUpViewModel.signUp(fullName, token, email, password)
                }
            }
        }
    }

    //this method is responsible for making action based on the result of the registration
    private fun observeSignUpResult() {
        signUpViewModel.signUpResult.observe(viewLifecycleOwner) { signUpResult ->
            when (signUpResult) {
                is SignUpResult.Success -> {
                    preferences.putFullName(signUpResult.fullName)

                    // Handle successful sign up
                    // Show success message or navigate to the next screen
                    showSnackBar("Sign up successful")

                    //now its fine to say lets get to home.
                    navigateToHomeActivity()
                }

                is SignUpResult.Error -> {
                    // Handle sign up error
                    showSnackBar(signUpResult.errorMessage)
                    handleInputValidationErrors(signUpResult.errorMessage) // New line
                }
            }
        }
    }

    private fun showSnackBar(massage: String) {
        Snackbar.make(binding.root, massage, Snackbar.LENGTH_SHORT).show()
    }

    //TODO SET the proper direction after SingUp successfully here
    //TODO HomeActivity <>Base
    private fun navigateToHomeActivity() {
        val intent = Intent(requireContext(), BaseActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun handleInputValidationErrors(errorMessage: String) {
        when (errorMessage) {
            "INVALID_USERNAME" -> binding.edFullName.error = "Invalid username"
            "INVALID_EMAIL" -> binding.edEmail.error = "Invalid email"
            "INVALID_PASSWORD" -> binding.edPassword.error = "Invalid password"
            // Handle other validation errors as needed
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val username = binding.edFullName.text.toString().trim()
        val email = binding.edEmail.text.toString().trim()
        val password = binding.edPassword.text.toString()
        val confirmPassword = binding.edConfirmPassword.text.toString()

        // Validate username
        if (username.isEmpty()) {
            binding.edFullName.error = "Please enter a username"
            isValid = false
        } else if (username.length < 6) {
            binding.edFullName.error = "Username must be at least 6 characters long"
            isValid = false
        }

        // Validate email
        if (email.isEmpty()) {
            binding.edEmail.error = "Please enter an email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.edEmail.error = "Invalid email format"
            isValid = false
        }

        // Validate password
        if (password.isEmpty()) {
            binding.edPassword.error = "Please enter a password"
            isValid = false
        } else if (password.length < 8) {
            binding.edPassword.error = "Password must be at least 8 characters long"
            isValid = false
        } else if (!password.matches(Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).*\$"))) {
            binding.edPassword.error =
                "Password should contain at least one uppercase letter, one lowercase letter, and one digit"
            isValid = false
        }

        // Validate password confirmation
        if (confirmPassword.isEmpty()) {
            binding.edConfirmPassword.error = "Please confirm the password"
            isValid = false
        } else if (password != confirmPassword) {
            binding.edConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (password != confirmPassword) {
            binding.edConfirmPassword.error = "Passwords do not match."
            isValid = false
        }

        return isValid
    }
}
