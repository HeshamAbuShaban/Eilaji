package dev.anonymous.eilaji.ui.other.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import dev.anonymous.eilaji.R;
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
    public void onStart() {
        super.onStart();
        var dialog = getDialog();
        if (dialog!= null){
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupListeners();
    }

    public void setupListeners() {
        binding.saveUserSettingsBtn.setOnClickListener(view -> {
            String _repeatInterval = binding.repeatIntervalET.getText().toString();
//            String timeUnit = binding.timeUnitET.getText().toString();

            long repeatInterval = TextUtils.isEmpty(_repeatInterval) ? 1L : Long.parseLong(_repeatInterval);

            // Define a map to map radio button IDs to time unit strings
            Map<Integer, TimeUnit> radioButtonMap = new HashMap<>() {
                {
                    put(R.id.hoursRadioButton, TimeUnit.HOURS);
                    put(R.id.daysRadioButton, TimeUnit.DAYS);
                    put(R.id.minutesRadioButton, TimeUnit.MINUTES);
                }
            };
            // Get the ID of the checked radio button
            int checkedRadioButtonId = binding.timeUnitRadioGroup.getCheckedRadioButtonId();
            // Retrieve the corresponding time unit string from the map
            TimeUnit timeUnit = radioButtonMap.get(checkedRadioButtonId);


            if (timeUnit != null) {
                periodicReminderListener.collectUserPeriodicReminderListenerInputs(repeatInterval, timeUnit);
            } else {
                // Handle the case where no radio button is selected
                // You can show an error message or take appropriate action
                Toast.makeText(getContext(), "Enter a value to create a Reminder", Toast.LENGTH_SHORT).show();
            }
            dismiss();
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