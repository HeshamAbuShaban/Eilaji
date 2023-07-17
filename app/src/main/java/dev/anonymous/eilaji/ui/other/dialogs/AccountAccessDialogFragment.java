package dev.anonymous.eilaji.ui.other.dialogs;

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


public class AccountAccessDialogFragment extends DialogFragment {
    private AccountAccessListener accountAccessListener;
    private FragmentRequestPermissionsDialogBinding binding;
    // To Set Different Title
    private String title;
    private static final String DialogTitleKey = "DialogTitleKey";

    public AccountAccessDialogFragment() {
        // req empty
    }

    // Todo check the Riddle me this
    public static AccountAccessDialogFragment newInstance(String title) {
        Bundle args = new Bundle();
        args.putString(DialogTitleKey, title);
        AccountAccessDialogFragment fragment = new AccountAccessDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            accountAccessListener = (AccountAccessListener) getParentFragment();
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
            if (accountAccessListener != null) {
                accountAccessListener.onAllowClicked();
                dismiss();
            }
        });
        binding.denyPermissions.setOnClickListener(view -> {
            if (accountAccessListener != null) {
                accountAccessListener.onDenyClicked();
                dismiss();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        accountAccessListener = null;
    }

    public interface AccountAccessListener {
        void onAllowClicked();

        void onDenyClicked();
    }
}
