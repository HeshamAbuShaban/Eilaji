package dev.anonymous.eilaji.ui.base.user_interface.send_prescription

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.databinding.FragmentSendPrescriptionBinding
import dev.anonymous.eilaji.ui.other.dialogs.ImagePickerDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.ImagePickerDialogFragment.ImagePickerListener

class SendPrescriptionFragment : Fragment(), ImagePickerListener,
    SendPrescriptionViewModel.ImagePickerResultCallback {
    // galleryLauncher Replacement
    private lateinit var pickVisualMediaRequest: ActivityResultLauncher<PickVisualMediaRequest>

    //----------------------------------
    private lateinit var cameraLauncher: ActivityResultLauncher<Void?>

    //----------------------------------
    private lateinit var collectedBitmap: Bitmap
    //----------------------------------


    private lateinit var _binding: FragmentSendPrescriptionBinding
    private lateinit var sendPrescriptionViewModel: SendPrescriptionViewModel

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSendPrescriptionBinding.inflate(inflater, container, false)
        sendPrescriptionViewModel = ViewModelProvider(this)[SendPrescriptionViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Set the Callback Value to the ViewModel
        sendPrescriptionViewModel.registerImagePickerCallback(this)
        // Setup the Click listeners
        setupClickListeners()
        // Setup The Launchers
        setupCameraLauncher()
        setupGalleryLauncher()
        // setup the data from the viewModel container
        setupInitialedDataInputs()
    }

    private fun setupInitialedDataInputs() {
        with(binding) {
            edAskAboutPrescription.setText(sendPrescriptionViewModel.prescriptionAdditionalText.value)
            prescriptionImagePreview.setImageBitmap(sendPrescriptionViewModel.bitmap.value)
        }
    }

    private fun setupClickListeners() {
        // this to capture the user inputs text
        setEditTextChangeListener()
        with(binding) {
            // this to fire up the image selecting functionality
            sendPrescriptionLinearParent.setOnClickListener {
                // Must BE "*childFragmentManager*"
                ImagePickerDialogFragment().show(childFragmentManager, "ImagePicking")
            }

            // and this is to collect the user data and send them to some where
            fabSendPrescription.setOnClickListener {
                //get the values from the viewModel
                val bitmap :Bitmap? = sendPrescriptionViewModel.bitmap.value
                val text :String = sendPrescriptionViewModel.prescriptionAdditionalText.value.toString()
                if (bitmap == null || text.isEmpty()) {
                    Snackbar.make(root,"Please Enter a Prescription Detail",Snackbar.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(),"Text : $text, Image: $bitmap",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // if the user typed any word it will be saved into the view-model to be called later on
    private fun setEditTextChangeListener(){
        binding.edAskAboutPrescription.addTextChangedListener {
            sendPrescriptionViewModel.setPrescriptionAdditionalText(it.toString())
        }
    }

    // ********************************************************************************************************************************
    // Picture Setups ------------------------------------------------------------------------

    // Set up the gallery launcher
    private fun setupGalleryLauncher() {
        pickVisualMediaRequest =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
                if (uri != null) {
                    var selectedImageBitmap: Bitmap? = null
                    try {
                        selectedImageBitmap = if (Build.VERSION.SDK_INT < 28) {
                            @Suppress("DEPRECATION")
                            MediaStore.Images.Media.getBitmap(
                                requireContext().contentResolver,
                                uri
                            )
                        } else {
                            val source = ImageDecoder.createSource(
                                requireContext().contentResolver,
                                uri
                            )
                            ImageDecoder.decodeBitmap(source)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    //here use the bitmap object as you wish
                    if (selectedImageBitmap != null) {
                        // done successfully
                        handleGalleryImage(selectedImageBitmap)
                    } else {
                        Toast.makeText(requireContext(), "What! No Image!", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Something wrong occurred", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Set up the camera launcher
    private fun setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview())
        { bitmap ->
            if (bitmap != null) {
                // Handle the captured image
                handleCameraImage(bitmap)
            } else {
                // Handle case when image capture was unsuccessful
                sendPrescriptionViewModel.imagePickerCallback?.onImageSelectionCancelled()
            }
        }
    }


    // These methods 2@open... for the Launching
    private fun openGallery() {
        // Request permission to access the gallery
        pickVisualMediaRequest.launch(
            PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly).build()
        )
    }

    private fun openCamera() {
        // Request permission to access the camera
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraLauncher.launch(null)
        } else {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                requireActivity(),
                permissions,
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    // Give the Bitmap to ViewModel
    private fun handleGalleryImage(bitmap: Bitmap) {
        sendPrescriptionViewModel.imagePickerCallback?.onImageSelected(bitmap)
    }

    private fun handleCameraImage(bitmap: Bitmap) {
        sendPrescriptionViewModel.imagePickerCallback?.onImageSelected(bitmap)
    }


    // These method trigger when the Dialog Buttons are Clicked
    override fun onCameraClicked() {
        openCamera()
    }


    override fun onGalleryClicked() {
        openGallery()
    }

    //===========Firebase ,Store, Api , Any way to use the image================================================
    override fun onImageSelected(bitmap: Bitmap) {
        // save or send or do what ever with the image
        collectedBitmap = bitmap
        //send this to the viewModel
        sendPrescriptionViewModel.setBitmap(collectedBitmap)
        // set a preview from the changed bitmap value within the viewModel If it got changed
        binding.prescriptionImagePreview.setImageBitmap(sendPrescriptionViewModel.bitmap.value)
    }

    override fun onImageSelectionCancelled() {
        // show a proper message for user why there is no image
        Toast.makeText(requireContext(), "Something Went Wrong !", Toast.LENGTH_SHORT).show()
    }

    // Other helper methods ============================
    companion object {
        /*private const val GALLERY_PERMISSION_REQUEST_CODE = 100*///no need for it rn
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

}