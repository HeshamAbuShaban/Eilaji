package dev.anonymous.eilaji.firebase

import android.app.Activity
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseController private constructor() {
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    companion object {
        @Volatile
        private var instance: FirebaseController? = null
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: FirebaseController().also { instance = it }
            }

        private const val TAG = "FirebaseController"
    }

    fun login(
        email: String,
        password: String,
        activity: Activity,
        onTaskSuccessful: () -> Unit,
        showSnackBar: (task: Task<AuthResult>) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    onTaskSuccessful()
                } else {
                    showSnackBar(task)
                }
            }
    }

    fun registerUser(email: String, password: String, callback: RegistrationCallback) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Registration successful
                    val user: FirebaseUser? = auth.currentUser
                    callback.onRegistrationSuccess(user)
                } else {
                    // Registration failed
                    val exception = task.exception
                    callback.onRegistrationFailure(exception)
                }
            }.addOnFailureListener {
                Log.d(TAG, "registerUser() FailureListener called")
            }
    }

    interface RegistrationCallback {
        fun onRegistrationSuccess(user: FirebaseUser?)
        fun onRegistrationFailure(exception: Exception?)
    }
}