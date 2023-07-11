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
import com.google.firebase.messaging.FirebaseMessaging
import dev.anonymous.eilaji.models.Pharmacy
import dev.anonymous.eilaji.models.server.Medicine
import dev.anonymous.eilaji.models.server.SubCategory
import dev.anonymous.eilaji.storage.AppSharedPreferences


class FirebaseController private constructor() {
    enum class DatabaseDocuments {
        Users, SubCategories, Medicines, Pharmacies
    }

    enum class UsersChildes {
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

    fun getPharmacies(
        onTaskSuccessful: (arrayPharmacies: ArrayList<Pharmacy>) -> Unit,
        onTaskFailed: (error: String) -> Unit,
    ) {
        fireStore.collection(DatabaseDocuments.Pharmacies.name)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    onTaskSuccessful(ArrayList(it.result.toObjects(Pharmacy::class.java)))
                }
            }.addOnFailureListener {
                onTaskFailed(it.message.toString())
            }
    }

    fun getSubCategoryMedicines(
        categoryId: String,
        subCategoryId: String?,
        onTaskSuccessful: (arrayMedicines: ArrayList<Medicine>) -> Unit,
        onTaskFailed: (error: String) -> Unit,
    ) {
        // لجلب كل الادوية التابعة للقسم
        var query = fireStore.collection(DatabaseDocuments.Medicines.name)
            .whereEqualTo("categoryId", categoryId)

        // لجلب كل الادوية التابعة للقسم الفرعي
        if (subCategoryId != null) {
            query = query.whereEqualTo("subCategoryId", subCategoryId)
        }

        query.get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    onTaskSuccessful(ArrayList(it.result.toObjects(Medicine::class.java)))
                }
            }.addOnFailureListener {
                onTaskFailed(it.message.toString())
            }
    }

    fun getSubCategories(
        categoryId: String,
        onTaskSuccessful: (arrayListSubCategories: ArrayList<SubCategory>) -> Unit,
        onTaskFailed: (error: String) -> Unit,
    ) {
        fireStore.collection(DatabaseDocuments.SubCategories.name)
            .whereEqualTo("categoryId", categoryId)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    onTaskSuccessful(ArrayList(it.result.toObjects(SubCategory::class.java)))
                }
            }.addOnFailureListener {
                onTaskFailed(it.message.toString())
            }
    }

    fun getToken(
        onTaskSuccessful: (token: String) -> Unit,
        onTaskFailed: (error: String) -> Unit,
    ) {
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                Log.d(TAG, "getToken: $token")
                onTaskSuccessful(token)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "onCreate:getToken: " + e.message)
                onTaskFailed(e.message!!)
            }
    }

    fun getUser(
        userUid: String,
        onTaskSuccessful: (exists: Boolean, fullName: String?, imageUrl: String?) -> Unit,
        onTaskFailed: (error: String) -> Unit,
    ) {
        fireStore.collection(DatabaseDocuments.Users.name)
            .document(userUid)
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    if (it.result.exists()) {
                        it.result.data?.let { data ->
                            onTaskSuccessful(
                                true,
                                data[UsersChildes.fullName.name].toString(),
                                data[UsersChildes.imageUrl.name].toString()
                            )
                        }
                    } else {
                        onTaskSuccessful(false, null, null)
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
        val data = mapOf(
            UsersChildes.userUid.name to userUid,
            UsersChildes.fullName.name to fullName,
            UsersChildes.imageUrl.name to imageUrl,
            UsersChildes.token.name to token,
        )

        fireStore.collection(DatabaseDocuments.Users.name)
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
        onTaskSuccessful: (userUid: String) -> Unit,
        showSnackBar: (task: Task<AuthResult>) -> Unit,
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    onTaskSuccessful(task.result.user!!.uid)
                } else {
                    showSnackBar(task)
                }
            }
    }

    fun register(
        email: String,
        password: String,
        onTaskSuccessful: (userUid: String) -> Unit,
        onTaskFailed: (String) -> Unit,
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onTaskSuccessful(task.result.user!!.uid)
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
}