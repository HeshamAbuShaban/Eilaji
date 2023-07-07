package dev.anonymous.eilaji.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import dev.anonymous.eilaji.storage.AppSharedPreferences


class FirebaseController private constructor() {
    enum class FirebaseKeys {
        userUid, fullName, imageUrl, token
    }

    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var instance: FirebaseController? = null
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: FirebaseController().also { instance = it }
            }

        private const val TAG = "FirebaseController"
    }

    fun getUser(
        userUid: String,
        onTaskSuccessful: (fullName: String, imageUrl: String) -> Unit,
        onTaskFailed: (error: String) -> Unit,
    ) {
        fireStore.collection("Users")
            .document(userUid)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    it.result.data?.let { data ->
                        onTaskSuccessful(
                            data["fullName"].toString(),
                            data["imageUrl"].toString()
                        )
                    }
                } else {
                    onTaskFailed(it.exception?.message!!)
                }
            }.addOnFailureListener {
                onTaskFailed(it.message.toString())
            }
    }

    fun addUser(
        userUid: String,
        fullName: String,
        imageUrl: String = "default",
        token: String,
        onTaskSuccessful: () -> Unit,
        onTaskFailed: (error: String) -> Unit,
    ) {
        val data: Map<String, String> = mapOf(
            "userUid" to userUid,
            "fullName" to fullName,
            "imageUrl" to imageUrl,
            "token" to token,
        )

        fireStore.collection("Users")
            .document(userUid)
            .set(data)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    onTaskSuccessful()
                } else {
                    onTaskFailed(it.exception?.message!!)
                }
            }.addOnFailureListener {
                onTaskFailed(it.message.toString())
            }
    }

    fun login(
        email: String,
        password: String,
        activity: Activity,
        onTaskSuccessful: () -> Unit,
        showSnackBar: (task: Task<AuthResult>) -> Unit,
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
        email: String,
        password: String,
        onTaskSuccessful: () -> Unit,
        onTaskFailed: (String) -> Unit,
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onTaskSuccessful()
                } else {
                    Log.d(TAG, "register: task isn't successfully proceed", task.exception)
                    val exception = task.exception as? FirebaseAuthException
                    val errorMessage = exception?.message ?: "Unknown error occurred"
                    onTaskFailed(errorMessage)
                }
            }
    }

    fun signOut(context: Context) {
        auth.signOut()
        AppSharedPreferences.getInstance(context).clear()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun forgotPassword(
        email: String, onTaskSuccessful: () -> Unit,
        onTaskFailed: (Exception) -> Unit
    ) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onTaskSuccessful()
                } else {
                    val exception: Exception? = task.exception
                    if (exception != null) {
                        val error = exception.message
                        Log.e(TAG, "forgotPassword:error:$error , ex :", exception)
                    }
                }
            }
            .addOnFailureListener {
                onTaskFailed(it)
            }
    }

    /*fun changePassword() {
        // auth.
    }*/
}