package dev.anonymous.eilaji.ui.other.reminder

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.anonymous.eilaji.databinding.FragmentReminderBinding
//import dev.anonymous.eilaji.reminder_system.database.Medication
//import dev.anonymous.eilaji.reminder_system.database.MedicationViewModel
import java.util.Calendar

class ReminderFragment : Fragment() {
//    private lateinit var medicationViewModel: MedicationViewModel
    private lateinit var binding: FragmentReminderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReminderBinding.inflate(layoutInflater)
//        medicationViewModel = ViewModelProvider(requireActivity())[MedicationViewModel::class.java]
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.reminderSubmitButton.setOnClickListener {
            val name = binding.medicineNameET.text.toString()
            val dosage = binding.dosageET.text.toString()
            val reminderTime = getReminderTimeFromPicker(binding.reminderTimePicker)

//            val medication = Medication(name = name, dosage = dosage, reminderTime = reminderTime)
//            medicationViewModel.saveMedication(medication)
//            medicationViewModel.scheduleReminder(medication)

            Toast.makeText(requireContext(), "Medication reminder saved.", Toast.LENGTH_SHORT)
                .show()
            // Clear input fields
            binding.medicineNameET.text.clear()
            binding.dosageET.text.clear()
            binding.reminderTimePicker.clearFocus()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getReminderTimeFromPicker(timePicker: TimePicker): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
        calendar.set(Calendar.MINUTE, timePicker.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}