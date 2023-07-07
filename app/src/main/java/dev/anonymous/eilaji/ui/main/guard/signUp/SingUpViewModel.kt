package dev.anonymous.eilaji.ui.main.guard.signUp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.firebase.FirebaseController

class SignUpViewModel : ViewModel() {
    private val firebaseController = FirebaseController.getInstance()

    private val _signUpResult = MutableLiveData<SignUpResult>()
    val signUpResult: LiveData<SignUpResult> get() = _signUpResult

    fun signUp(fullName: String, email: String, password: String, confirmPassword: String) {
        // Perform input validation
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            _signUpResult.value = SignUpResult.Error("Please fill in all fields.")
            return
        }

        if (password != confirmPassword) {
            _signUpResult.value = SignUpResult.Error("Passwords do not match.")
            return
        }

        // Call the register method from FirebaseController
        firebaseController.register(email, password,
            onTaskSuccessful = {
                _signUpResult.value = SignUpResult.Success(fullName)
            }, onTaskFailed = { exception ->
//            val errorMessage = getFirebaseErrorMessage(exception)
                // now onTaskFailed returns final massage without the need for getFirebaseErrorMessage
                _signUpResult.value = SignUpResult.Error(exception)
            }
        )
    }

    /*
    //this one is a bit different than the Login one
    private fun getFirebaseErrorMessage(errorCode: String): String {
        Log.d("SignUp", "getFirebaseErrorMessage: errorCode from Firebase :$errorCode")
        // Map Firebase error codes to error messages
        return when (errorCode) {
            //Older 3 dose-nt actually work
            "ERROR_INVALID_EMAIL" -> "Invalid email address."
            "ERROR_WEAK_PASSWORD" -> "Weak password. Please choose a stronger password."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use."
            //New 3Lines That ChatP provide when ask to show error massage for `SignUp`
            "INVALID_USERNAME" -> "Invalid username"
            "INVALID_EMAIL" -> "Invalid email"
            "INVALID_PASSWORD" -> "Invalid password"
            //default replay if not found the accurate one.
            else -> "Sign up failed."
        }
    }
     */
}

sealed class SignUpResult {
    class Success(val fullName: String, val imageUrl: String = "default") : SignUpResult()
    data class Error(val errorMessage: String) : SignUpResult()
}
