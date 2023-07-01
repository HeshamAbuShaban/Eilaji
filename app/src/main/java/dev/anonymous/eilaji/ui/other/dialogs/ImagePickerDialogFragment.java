package dev.anonymous.eilaji.ui.other.dialogs;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dev.anonymous.eilaji.databinding.FragmentImagePickerDialogBinding;


public class ImagePickerDialogFragment extends DialogFragment {

    private ImagePickerListener imagePickerListener;

    private FragmentImagePickerDialogBinding binding;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            imagePickerListener = (ImagePickerListener) getParentFragment();
            Log.d("ImagePickerListener", "onAttach: Done for :"+imagePickerListener);
            /*
             *  Must BE "*childFragmentManager*"
             *  ImagePickerDialogFragment().show(childFragmentManager, "ImagePicking")
             *
             */

        } catch (ClassCastException exception) {
            throw new ClassCastException(getParentFragment()
                    + " must implement ImagePickerListener, exception: " + exception);
        }
    }

    public ImagePickerDialogFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentImagePickerDialogBinding.inflate(getLayoutInflater());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
    }

    private void setupListeners() {
        binding.btnCamera.setOnClickListener(view_camera -> {
            imagePickerListener.onCameraClicked();
            dismiss();
        });

        binding.btnGallery.setOnClickListener(view_gallery -> {
            imagePickerListener.onGalleryClicked();
            dismiss();
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        imagePickerListener = null;
    }

    public interface ImagePickerListener {
        void onCameraClicked();

        void onGalleryClicked();
    }
}