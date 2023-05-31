package dev.anonymous.eilaji.firebase

import android.app.Activity
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest

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

    fun register(
        username: String,
        email: String,
        password: String,
        onTaskSuccessful: () -> Unit,
        onTaskFailed: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    // Update user profile with username if desired
                    user?.updateProfile(
                        UserProfileChangeRequest.Builder().setDisplayName(username).build()
                    )
                    onTaskSuccessful()
                } else {
                    Log.d(TAG, "register: task isn't successfully proceed", task.exception)
                    val exception = task.exception as? FirebaseAuthException
                    val errorMessage = exception?.message ?: "Unknown error occurred"
                    onTaskFailed(errorMessage)
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }

}