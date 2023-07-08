package dev.anonymous.eilaji.ui.main.guard.signUp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.firebase.FirebaseController

class SignUpViewModel : ViewModel() {
    private val firebaseController = FirebaseController.getInstance()

    private val _signUpResult = MutableLiveData<SignUpResult>()
    val signUpResult: LiveData<SignUpResult> get() = _signUpResult

    fun signUp(username: String, email: String, password: String, confirmPassword: String) {
        // Perform input validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            _signUpResult.value = SignUpResult.Error("Please fill in all fields.")
            return
        }

        if (password != confirmPassword) {
            _signUpResult.value = SignUpResult.Error("Passwords do not match.")
            return
        }

        // Call the register method from FirebaseController
        firebaseController.register(username, email, password, onTaskSuccessful = {
            _signUpResult.value = SignUpResult.Success
        }, onTaskFailed = { exception ->
            // val errorMessage = getFirebaseErrorMessage(exception)
            // now onTaskFailed returns final massage without the need for getFirebaseErrorMessage
            _signUpResult.value = SignUpResult.Error(exception)
        })
    }

}

sealed class SignUpResult {
    object Success : SignUpResult()
    data class Error(val errorMessage: String) : SignUpResult()
}
