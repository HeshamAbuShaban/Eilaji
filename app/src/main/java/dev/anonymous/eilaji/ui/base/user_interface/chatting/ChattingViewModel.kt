package dev.anonymous.eilaji.ui.base.user_interface.chatting

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import dev.anonymous.eilaji.firebase.FirebaseController

class ChattingViewModel : ViewModel() {
    private val currentUserLiveData = MutableLiveData<FirebaseUser?>()
    private lateinit var userUid: String

    fun getCurrentUser(): MutableLiveData<FirebaseUser?> {
        if (currentUserLiveData.value == null) {
            val firebaseController = FirebaseController.getInstance()
            val currentUser = firebaseController.getCurrentUser()
            currentUserLiveData.value = currentUser
            currentUser?.let { userUid = it.uid }
        }
        return currentUserLiveData
    }

    fun getUserUid(): String {
        return userUid
    }
}
