package dev.anonymous.eilaji.ui.signUp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.databinding.FragmentSignUpBinding
import dev.anonymous.eilaji.ui.home.HomeActivity

class FragmentSignUp : Fragment() {
    private var binding: FragmentSignUpBinding? = null
    private lateinit var signUpViewModel: SignUpViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSignUpBinding.inflate(inflater, container, false)
        signUpViewModel = ViewModelProvider(this)[SignUpViewModel::class.java]

        performSignUp()

        observeSignUpResult()

        return binding!!.root
    }
    //this method have a ClickListener that performSignUp using the ViewModel call for register method and the Result are being controlled in the @#observeSignUpResult()
    private fun performSignUp() {
        binding?.buCreateAnAccount?.setOnClickListener {
            if (validateInputs()) {
                val username = binding?.edUsername?.text.toString()
                val email = binding?.edEmail?.text.toString()
                val password = binding?.edPassword?.text.toString()
                val confirmPassword = binding?.edConfirmPassword?.text.toString()
                signUpViewModel.signUp(username, email, password, confirmPassword)
            }
            Log.i("FragmentSignUp", "performSingUp: data got collected")
        }
    }
    //this method is responsible for making action based on the result of the registration
    private fun observeSignUpResult() {
        signUpViewModel.signUpResult.observe(viewLifecycleOwner) { signUpResult ->
            when (signUpResult) {
                is SignUpResult.Success -> {
                    // Handle successful sign up
                    // Show success message or navigate to the next screen
                    Snackbar.make(binding!!.root, "Sign up successful", Snackbar.LENGTH_SHORT).show()
                    //now its fine to say lets get to home.
                    navigateToHomeActivity()
                }
                is SignUpResult.Error -> {
                    // Handle sign up error
                    Snackbar.make(binding!!.root, signUpResult.errorMessage, Snackbar.LENGTH_SHORT).show()
                    handleInputValidationErrors(signUpResult.errorMessage) // New line
                }
            }
        }
    }

    //TODO SET the proper direction after SingUp successfully here
    private fun navigateToHomeActivity() {
        val intent = Intent(requireContext(), HomeActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    private fun handleInputValidationErrors(errorMessage: String) {
        when (errorMessage) {
            "INVALID_USERNAME" -> binding!!.edUsername.error = "Invalid username"
            "INVALID_EMAIL" -> binding!!.edEmail.error = "Invalid email"
            "INVALID_PASSWORD" -> binding!!.edPassword.error = "Invalid password"
            // Handle other validation errors as needed
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        val username = binding!!.edUsername.text.toString().trim()
        val email = binding!!.edEmail.text.toString().trim()
        val password = binding!!.edPassword.text.toString()
        val confirmPassword = binding!!.edConfirmPassword.text.toString()

        // Validate username
        if (username.isEmpty()) {
            binding!!.edUsername.error = "Please enter a username"
            isValid = false
        } else if (username.length < 6) {
            binding!!.edUsername.error = "Username must be at least 6 characters long"
            isValid = false
        }

        // Validate email
        if (email.isEmpty()) {
            binding!!.edEmail.error = "Please enter an email"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding!!.edEmail.error = "Invalid email format"
            isValid = false
        }

        // Validate password
        if (password.isEmpty()) {
            binding!!.edPassword.error = "Please enter a password"
            isValid = false
        } else if (password.length < 8) {
            binding!!.edPassword.error = "Password must be at least 8 characters long"
            isValid = false
        } else if (!password.matches(Regex("^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9]).*\$"))) {
            binding!!.edPassword.error = "Password should contain at least one uppercase letter, one lowercase letter, and one digit"
            isValid = false
        }

        // Validate password confirmation
        if (confirmPassword.isEmpty()) {
            binding!!.edConfirmPassword.error = "Please confirm the password"
            isValid = false
        } else if (password != confirmPassword) {
            binding!!.edConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        return isValid
    }


    //To clean Objects in our case> binding object for now ...much faster app is the target of this step
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
