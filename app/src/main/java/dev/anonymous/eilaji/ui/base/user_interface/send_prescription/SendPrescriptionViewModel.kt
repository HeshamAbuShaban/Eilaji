package dev.anonymous.eilaji.ui.base.user_interface.send_prescription

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
class SendPrescriptionViewModel : ViewModel() {
    var imagePickerCallback: ImagePickerResultCallback? = null
    // inputs to be saved
    private val _bitmap = MutableLiveData<Bitmap?>()
    val bitmap: LiveData<Bitmap?> = _bitmap
    fun setBitmap(bitmap: Bitmap) {
        _bitmap.value = bitmap
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
        fun onImageSelected(bitmap: Bitmap)
        fun onImageSelectionCancelled()
    }
}

