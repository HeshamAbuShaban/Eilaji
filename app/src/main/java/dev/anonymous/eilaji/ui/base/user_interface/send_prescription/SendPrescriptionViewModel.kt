package dev.anonymous.eilaji.ui.base.user_interface.send_prescription

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class SendPrescriptionViewModel : ViewModel() {
    var imagePickerCallback: ImagePickerResultCallback? = null
    // inputs to be saved
    private val _bitmap = MutableLiveData<Uri?>()
    val bitmap: LiveData<Uri?> = _bitmap
    fun setUri(uri: Uri) {
        _bitmap.value = uri
    }

    private val _prescriptionAdditionalText = MutableLiveData<String?>()
    val prescriptionAdditionalText: LiveData<String?> = _prescriptionAdditionalText

    fun setPrescriptionAdditionalText(text :String) {
        _prescriptionAdditionalText.value = text
    }
    fun registerImagePickerCallback(callback: ImagePickerResultCallback) {
        imagePickerCallback = callback
    }
    override fun onCleared() {
        super.onCleared()
        // Clear the callback reference to avoid memory leaks
        imagePickerCallback = null
    }

    // FOR THE UI ========================================================================
    interface ImagePickerResultCallback {
        fun onImageSelected(uri: Uri)
        fun onImageSelectionCancelled()
    }
}

