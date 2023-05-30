package dev.anonymous.eilaji.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuthException
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentLoginBinding
import dev.anonymous.eilaji.firebase.FirebaseController
import dev.anonymous.eilaji.ui.home.HomeActivity
import dev.anonymous.eilaji.ui.singup.FragmentSingUp

class FragmentLogin : Fragment() {
    private var binding: FragmentLoginBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        binding?.apply {
            buSignUp.setOnClickListener {
                val singUpFragment = FragmentSingUp()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.mainActivityContainer, singUpFragment)
                    .commitAllowingStateLoss()
            }

            buLogin.setOnClickListener {
                val email = edEmail.text.toString()
                val password = edPassword.text.toString()

                if (checkData(email, password)) {
                    login(email, password)
                } else {
                    showTextError(email, password)
                }
            }
        }
        return binding!!.root
    }

    private fun showTextError(email: String, password: String) {
        binding?.apply {
            edEmail.error = if (email.isEmpty()) "Please Enter Your Email" else null
            edPassword.error = if (password.isEmpty()) "Please Enter Your Password" else null
        }
    }

    private fun checkData(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }

    private fun login(email: String, password: String) {
        // This Method Handle different Firebase error codes and return appropriate error messages
        fun getFirebaseErrorMessage(errorCode: String): String
        {
            return when (errorCode) {
                // Add specific error codes and corresponding error messages here
                "ERROR_INVALID_EMAIL" -> "Invalid email address."
                "ERROR_WRONG_PASSWORD" -> "Incorrect password."
                "ERROR_USER_NOT_FOUND" -> "User not found."
                "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use."
                else -> "Authentication failed."
            }
        }

        fun showSnackBar(view: View, message: String) =
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()

        FirebaseController.getInstance().login(email, password, requireActivity(), {
            startActivity(Intent(requireContext(), HomeActivity::class.java))
            activity?.finish()
        }) { task ->
            val exception = task.exception
            if (exception is FirebaseAuthException) {
                val errorCode = exception.errorCode
                val errorMessage = getFirebaseErrorMessage(errorCode)
                showSnackBar(binding?.buLogin!!, errorMessage)
            } else {
                Log.e("FirebaseController.TAG", "Login error: ${exception?.message}")
                Toast.makeText(activity, "Authentication failed.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}