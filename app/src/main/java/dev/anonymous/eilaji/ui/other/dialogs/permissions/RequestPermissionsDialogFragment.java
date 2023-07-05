package dev.anonymous.eilaji.ui.other.dialogs.permissions;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import dev.anonymous.eilaji.databinding.FragmentRequestPermissionsDialogBinding;


public class RequestPermissionsDialogFragment extends DialogFragment {
    private RequestPermissionsListener requestPermissionsListener;
    private FragmentRequestPermissionsDialogBinding binding;
    // To Set Different Title
    private String title;
    private static final String DialogTitleKey = "DialogTitleKey";

    public RequestPermissionsDialogFragment() {
        // req empty
    }

    // Todo check the Riddle me this
    public static RequestPermissionsDialogFragment newInstance(String title) {
        Bundle args = new Bundle();
        args.putString(DialogTitleKey, title);
        RequestPermissionsDialogFragment fragment = new RequestPermissionsDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            requestPermissionsListener = (RequestPermissionsListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement ChangeSoundListener Exception: " + e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        var dialog = getDialog();
        if (dialog!= null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentRequestPermissionsDialogBinding.inflate(getLayoutInflater());
        var args = getArguments();
        if (args != null) {
            title = args.getString(DialogTitleKey);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.setCancelable(false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // set the title for the dialog
        binding.requestPermissionsTitle.setText(title);
        // set the click logic
        setupListeners();
    }

    public void setupListeners() {
        binding.allowPermissions.setOnClickListener(view -> {
            if (requestPermissionsListener != null) {
                requestPermissionsListener.onAllowClicked();
                dismiss();
            }
        });
        binding.denyPermissions.setOnClickListener(view -> {
            if (requestPermissionsListener != null) {
                requestPermissionsListener.onDenyClicked();
                dismiss();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        requestPermissionsListener = null;
    }

    public interface RequestPermissionsListener {
        void onAllowClicked();

        void onDenyClicked();
    }
}
