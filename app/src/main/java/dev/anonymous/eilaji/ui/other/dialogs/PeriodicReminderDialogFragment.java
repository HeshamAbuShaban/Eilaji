package dev.anonymous.eilaji.ui.other.dialogs;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.concurrent.TimeUnit;

import dev.anonymous.eilaji.databinding.FragmentPeriodicReminderDialogBinding;

public class PeriodicReminderDialogFragment extends DialogFragment {
    private PeriodicReminderListener periodicReminderListener;
    private FragmentPeriodicReminderDialogBinding binding;

    public PeriodicReminderDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            periodicReminderListener = (PeriodicReminderListener) getParentFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException(context
                    + " must implement PeriodicReminderListener ");
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentPeriodicReminderDialogBinding.inflate(getLayoutInflater());
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
    public void setupListeners() {

        binding.saveUserSettingsBtn.setOnClickListener(view -> {
            var repeatInterval = binding.repeatIntervalET.getText().toString();
            var timeUnit = binding.timeUnitET.getText().toString();
            var fr = Long.parseLong(repeatInterval);
            TimeUnit ft = null;
            switch (timeUnit) {
                case "days" -> ft = TimeUnit.DAYS;
                case "hours" -> ft = TimeUnit.HOURS;
                case "minutes" -> ft = TimeUnit.MINUTES;
            }
            if (!repeatInterval.isEmpty() || !timeUnit.isEmpty()) {
                periodicReminderListener.collectUserPeriodicReminderListenerInputs(fr, ft);
                dismiss();
            }else {
                dismiss();
            }

        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        periodicReminderListener = null;
    }

    public interface PeriodicReminderListener {
        void collectUserPeriodicReminderListenerInputs(Long repeatInterval, TimeUnit timeUnit);
    }
}