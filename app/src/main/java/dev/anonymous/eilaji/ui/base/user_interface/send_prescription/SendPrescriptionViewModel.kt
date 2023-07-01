package dev.anonymous.eilaji.ui.base.user_interface.send_prescription

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
class SendPrescriptionViewModel : ViewModel() {
    var imagePickerCallback: ImagePickerResultCallback? = null

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

