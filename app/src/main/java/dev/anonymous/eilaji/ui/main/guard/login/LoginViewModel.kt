package dev.anonymous.eilaji.ui.main.guard.login

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuthException
import dev.anonymous.eilaji.firebase.FirebaseController


class LoginViewModel : ViewModel() {
    private val firebaseController = FirebaseController.getInstance()

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> get() = _loginResult

    fun login(email: String, password: String, activity: Activity) {
        firebaseController.login(email, password, activity, onTaskSuccessful = {
            _loginResult.value = LoginResult.Success
        }, showSnackBar = { task ->
            val exception = task.exception
            if (exception is FirebaseAuthException) {
                val errorCode = exception.errorCode
                val errorMessage = getFirebaseErrorMessage(errorCode)
                _loginResult.value = LoginResult.Error(errorMessage)
            } else {
                _loginResult.value = LoginResult.Error("Authentication failed.")
            }
        })
    }

    private fun getFirebaseErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "ERROR_INVALID_EMAIL" -> "Invalid email address."
            "ERROR_WRONG_PASSWORD" -> "Incorrect password."
            "ERROR_USER_NOT_FOUND" -> "User not found."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use."
            else -> "Authentication failed."
        }
    }
}

sealed class LoginResult {
    object Success : LoginResult()
    data class Error(val errorMessage: String) : LoginResult()
}
