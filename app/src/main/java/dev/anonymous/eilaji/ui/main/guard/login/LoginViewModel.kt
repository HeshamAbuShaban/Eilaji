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

    private val _token = MutableLiveData<String>()
    val token: LiveData<String> get() = _token

    fun login(email: String, password: String, activity: Activity) {
        firebaseController.login(email, password, activity,
            onTaskSuccessful = { getUserData(it) },
            showSnackBar = { task ->
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

    private fun getUserData(userUid: String) {
        firebaseController.getUser(
            userUid,
            onTaskSuccessful = { exists, fullName, imageUrl ->
                if (exists) {
                    _loginResult.value = LoginResult.Success(
                        fullName.toString(), imageUrl.toString()
                    )
                } else {
                    _loginResult.value = LoginResult.Error("User not found")
                }
            },
            onTaskFailed = {
                _loginResult.value = LoginResult.Error(it)
            }
        )
    }

    fun getToken() {
        firebaseController.getToken(
            onTaskSuccessful = { _token.value = it },
            onTaskFailed = {
                _loginResult.value = LoginResult.Error(it)
            }
        )
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
    data class Success(val fullName: String, val imageUrl: String) : LoginResult()
    data class Error(val errorMessage: String) : LoginResult()
}
