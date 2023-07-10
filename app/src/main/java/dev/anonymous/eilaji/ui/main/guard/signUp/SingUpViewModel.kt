package dev.anonymous.eilaji.ui.main.guard.signUp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.firebase.FirebaseController

class SignUpViewModel : ViewModel() {
    private val firebaseController = FirebaseController.getInstance()

    private val _signUpResult = MutableLiveData<SignUpResult>()
    val signUpResult: LiveData<SignUpResult> get() = _signUpResult

    private val _token = MutableLiveData<String>()
    val token: LiveData<String> get() = _token

    fun signUp(fullName: String, token: String, email: String, password: String) {
        // Call the register method from FirebaseController
        firebaseController.register(email, password,
            onTaskSuccessful = { userUid ->
                putUserData(userUid, fullName, token)
            },
            onTaskFailed = { exception ->
//            val errorMessage = getFirebaseErrorMessage(exception)
                // now onTaskFailed returns final massage without the need for getFirebaseErrorMessage
                _signUpResult.value = SignUpResult.Error(exception)
            }
        )
    }

    private fun putUserData(userUid: String, fullName: String, token: String) {
        firebaseController.addUser(userUid, fullName, token = token,
            onTaskSuccessful = {
                _signUpResult.value = SignUpResult.Success(fullName)
            },
            onTaskFailed = {
                _signUpResult.value = SignUpResult.Error(it)
            }
        )
    }

    fun getToken() {
        firebaseController.getToken(
            onTaskSuccessful = { _token.value = it },
            onTaskFailed = {
                _signUpResult.value = SignUpResult.Error(it)
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
    data class Success(val fullName: String) : SignUpResult()
    data class Error(val errorMessage: String) : SignUpResult()
}
