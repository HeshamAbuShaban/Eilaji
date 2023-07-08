package dev.anonymous.eilaji.ui.main.guard.signUp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentSignUpBinding
import dev.anonymous.eilaji.ui.base.BaseActivity

class SignUpFragment : Fragment() {
    private lateinit var _binding: FragmentSignUpBinding
    private lateinit var signUpViewModel: SignUpViewModel
    private val binding get() = _binding

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
    
    private fun setupListeners(){

        binding.buSkip.setOnClickListener {
            startActivity(Intent(requireContext(), BaseActivity::class.java))
            requireActivity().finish()
        }

        binding.buLoginScreen.setOnClickListener {
            findNavController().navigate(R.id.navigation_Login)
        }
    }
    private fun setupViewModel() {
        signUpViewModel = ViewModelProvider(this)[SignUpViewModel::class.java]
    }

    //this method have a ClickListener that performSignUp using the ViewModel call for register method and the Result are being controlled in the @#observeSignUpResult()
    private fun performSignUp() {
        with(binding){
            buCreateAnAccount.setOnClickListener {
                if (validateInputs()) {
                    val username = edUsername.text.toString()
                    val email = edEmail.text.toString()
                    val password = edPassword.text.toString()
                    val confirmPassword = edConfirmPassword.text.toString()
                    signUpViewModel.signUp(username, email, password, confirmPassword)
                }
                Log.i("FragmentSignUp", "performSingUp: data got collected")
            }
        }

    }

    //this method is responsible for making action based on the result of the registration
    private fun observeSignUpResult() {
        signUpViewModel.signUpResult.observe(viewLifecycleOwner) { signUpResult ->
            when (signUpResult) {
                is SignUpResult.Success -> {
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
        Snackbar.make(binding.root, massage, Snackbar.LENGTH_SHORT)
            .show()
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
            "INVALID_USERNAME" -> binding.edUsername.error = "Invalid username"
            "INVALID_EMAIL" -> binding.edEmail.error = "Invalid email"
            "INVALID_PASSWORD" -> binding.edPassword.error = "Invalid password"
            // Handle other validation errors as needed
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        with(binding) {
            val username = edUsername.text.toString().trim()
            val email = edEmail.text.toString().trim()
            val password = edPassword.text.toString()
            val confirmPassword = edConfirmPassword.text.toString()


            // Validate username
            if (username.isEmpty()) {
                edUsername.error = "Please enter a username"
                isValid = false
            } else if (username.length < 6) {
                edUsername.error = "Username must be at least 6 characters long"
                isValid = false
            }

            // Validate email
            if (email.isEmpty()) {
                edEmail.error = "Please enter an email"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                edEmail.error = "Invalid email format"
                isValid = false
            }

            // Validate password
            if (password.isEmpty()) {
                edPassword.error = "Please enter a password"
                isValid = false
            } else if (password.length < 8) {
                edPassword.error = "Password must be at least 8 characters long"
                isValid = false
            } else if (!password.matches(Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).*\$"))) {
                edPassword.error =
                    "Password should contain at least one uppercase letter, one lowercase letter, and one digit"
                isValid = false
            }

            // Validate password confirmation
            if (confirmPassword.isEmpty()) {
                edConfirmPassword.error = "Please confirm the password"
                isValid = false
            } else if (password != confirmPassword) {
                edConfirmPassword.error = "Passwords do not match"
                isValid = false
            }
        }
        return isValid
    }

    
}
