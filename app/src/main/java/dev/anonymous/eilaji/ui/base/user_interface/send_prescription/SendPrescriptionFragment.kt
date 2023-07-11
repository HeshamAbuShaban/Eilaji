package dev.anonymous.eilaji.ui.base.user_interface.send_prescription

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
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
import dev.anonymous.eilaji.storage.enums.FragmentsKeys
import dev.anonymous.eilaji.ui.other.base.AlternativesActivity
import dev.anonymous.eilaji.ui.other.dialogs.ImagePickerDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.ImagePickerDialogFragment.ImagePickerListener
import java.io.ByteArrayOutputStream


class SendPrescriptionFragment : Fragment(), ImagePickerListener,
    SendPrescriptionViewModel.ImagePickerResultCallback {
    // galleryLauncher Replacement
    private lateinit var pickVisualMediaRequest: ActivityResultLauncher<PickVisualMediaRequest>

    //----------------------------------
    private lateinit var cameraLauncher: ActivityResultLauncher<Void?>

    //----------------------------------
    private lateinit var collectedBitmap: Uri
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

            with(sendPrescriptionViewModel.bitmap.value) {
                if (this != null) {
                    ivAddPrescriptionImage.setImageURI(this)
                }
            }
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
                val uri: Uri? = sendPrescriptionViewModel.bitmap.value
                val text: String =
                    sendPrescriptionViewModel.prescriptionAdditionalText.value.toString()
                if (uri == null || text.isEmpty()) {
                    Snackbar.make(root, "Please Enter a Prescription Detail", Snackbar.LENGTH_LONG)
                        .show()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Text : $text, Image: $uri",
                        Toast.LENGTH_LONG
                    ).show()
                    navToSendToPharmacyFragment(uri.toString(), text, 31.44955F, 34.395092F)
                }
            }
        }
    }

    private fun navToSendToPharmacyFragment(
        stringUri: String,
        description: String,
        lat: Float,
        lng: Float
    ) {
        val intent = Intent(requireContext(), AlternativesActivity::class.java)
        intent.putExtra("fragmentType", FragmentsKeys.sendToPharmacy.name)
        intent.putExtra("stringUri", stringUri)
        intent.putExtra("description", description)
        intent.putExtra("lat", lat)
        intent.putExtra("lng", lng)
        startActivity(intent)
    }

    // if the user typed any word it will be saved into the view-model to be called later on
    private fun setEditTextChangeListener() {
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
                    handleGalleryImage(uri)
                }
            }
    }

    // Set up the camera launcher
    private fun setupCameraLauncher() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview())
        { bitmap ->
            if (bitmap != null) {
                // Handle the captured image
                handleCameraImage(getImageUri(requireContext(), bitmap))
            } else {
                // Handle case when image capture was unsuccessful
                sendPrescriptionViewModel.imagePickerCallback?.onImageSelectionCancelled()
            }
        }
    }

    private fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            inContext.contentResolver,
            inImage,
            "Title",
            null
        )
        return Uri.parse(path)
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
    private fun handleGalleryImage(uri: Uri) {
        sendPrescriptionViewModel.imagePickerCallback?.onImageSelected(uri)
    }

    private fun handleCameraImage(uri: Uri) {
        sendPrescriptionViewModel.imagePickerCallback?.onImageSelected(uri)
    }


    // These method trigger when the Dialog Buttons are Clicked
    override fun onCameraClicked() {
        openCamera()
    }


    override fun onGalleryClicked() {
        openGallery()
    }

    //===========Firebase ,Store, Api , Any way to use the image================================================
    override fun onImageSelected(uri: Uri) {
        // save or send or do what ever with the image
        collectedBitmap = uri
        //send this to the viewModel
        sendPrescriptionViewModel.setUri(collectedBitmap)
        // set a preview from the changed bitmap value within the viewModel If it got changed
        binding.ivAddPrescriptionImage.setImageURI(uri)
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