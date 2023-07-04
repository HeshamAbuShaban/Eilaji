package dev.anonymous.eilaji.ui.other.dialogs;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import java.util.HashMap;
import java.util.Map;
import dev.anonymous.eilaji.R;
import dev.anonymous.eilaji.databinding.FragmentChangeReminderSoundDialogBinding;
import dev.anonymous.eilaji.storage.enums.SoundNumbers;

public class ChangeSoundDialogFragment extends DialogFragment {
    private ChangeSoundListener changeSoundListener;
    private FragmentChangeReminderSoundDialogBinding binding;

    public ChangeSoundDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            changeSoundListener = (ChangeSoundListener) getParentFragment();
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
        binding = FragmentChangeReminderSoundDialogBinding.inflate(getLayoutInflater());
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
        // Define a map to map radio button IDs to time unit strings
        Map<Integer, Integer> radioButtonMap = new HashMap<>() {
            {
                put(R.id.bellSoundRadioButton, SoundNumbers.SoundBell.soundNumber);
                put(R.id.talkingSoundRadioButton, SoundNumbers.SoundTalking.soundNumber);
                put(R.id.longSoundRadioButton, SoundNumbers.SoundLong.soundNumber);
                put(R.id.coolSoundRadioButton, SoundNumbers.SoundNice.soundNumber);
            }
        };

        binding.saveUserSettingsBtn.setOnClickListener(view -> {

            // Get the ID of the checked radio button
            int checkedRadioButtonId = binding.soundsRadioGroup.getCheckedRadioButtonId();
            // Retrieve the corresponding time unit string from the map
            // Retrieve the corresponding time unit string from the map
            Integer soundChosen = radioButtonMap.get(checkedRadioButtonId);

            if (soundChosen != null) {
                changeSoundListener.collectUserReminderSoundListenerInputs(soundChosen);
            } else {
                // Handle the case where no radio button is selected
                // You can show an error message or take appropriate action
                Toast.makeText(getContext(), "Enter a value to change the Reminder sound", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        changeSoundListener = null;
    }

    public interface ChangeSoundListener {
        void collectUserReminderSoundListenerInputs(int soundId);
    }
}