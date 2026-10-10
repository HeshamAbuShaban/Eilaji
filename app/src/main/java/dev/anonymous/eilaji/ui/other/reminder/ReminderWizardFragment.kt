package dev.anonymous.eilaji.ui.other.reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentReminderWizardBinding
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import dev.anonymous.eilaji.ui.components.StepStepper
import dev.anonymous.eilaji.ui.main.guard.login.LoginViewModel
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * New-reminder wizard: three steps, one decision each.
 *
 * Step 1 medicine and amount, step 2 time and repeat, step 3 confirm with the next dose
 * counted down. The scheduling itself is unchanged: it goes through the same [ReminderScheduler]
 * exact alarm and WorkManager fallback the old form used.
 */
class ReminderWizardFragment : Fragment() {

    private var _binding: FragmentReminderWizardBinding? = null
    private val binding get() = _binding!!

    private var step = 0
    private var doseCount = 1
    private var repeatMode = Repeat.ONCE

    private var scheduler: ReminderScheduler? = null
    private var syncRepo: dev.anonymous.eilaji.reminder_system.repository.ReminderSyncRepository? = null

    private enum class Repeat { ONCE, DAILY, WEEKLY }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReminderWizardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduler = ReminderScheduler(requireContext().applicationContext)
        syncRepo = try { dev.anonymous.eilaji.reminder_system.repository.ReminderSyncRepository(requireContext().applicationContext) } catch (_: Exception) { null }

        binding.toolbarWizard.setNavigationOnClickListener { findNavController().popBackStack() }
        binding.buWizardBack.setOnClickListener { goBack() }
        binding.buWizardNext.setOnClickListener { goNext() }

        binding.buDosePlus.setOnClickListener { setDose(doseCount + 1) }
        binding.buDoseMinus.setOnClickListener { setDose(doseCount - 1) }

        binding.doseWheel.setMinuteStep(5)
        binding.doseWheel.onTimeChanged = { _, _ ->
            _binding?.let { it.tvChosenTime.text = it.doseWheel.selectedTimeText() }
        }
        binding.tvChosenTime.text = binding.doseWheel.selectedTimeText()

        binding.chipMorning.setOnClickListener { preset(8, 0) }
        binding.chipNoon.setOnClickListener { preset(13, 0) }
        binding.chipEvening.setOnClickListener { preset(19, 0) }
        binding.chipNight.setOnClickListener { preset(22, 0) }

        binding.chipOnce.setOnClickListener { repeatMode = Repeat.ONCE }
        binding.chipDaily.setOnClickListener { repeatMode = Repeat.DAILY }
        binding.chipWeekly.setOnClickListener { repeatMode = Repeat.WEEKLY }

        loadCatalogSuggestions()
        render()
    }

    private fun setDose(value: Int) {
        val clamped = value.coerceIn(1, 20)
        if (clamped == doseCount) return
        doseCount = clamped
        try { binding.buDosePlus.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK) } catch (_: Exception) {}
        binding.tvDoseCount.text = doseCount.toString()
    }

    private fun preset(hour: Int, minute: Int) {
        binding.doseWheel.setTime(hour, minute)
        binding.tvChosenTime.text = binding.doseWheel.selectedTimeText()
        try { binding.chipGroupPresets.clearCheck() } catch (_: Exception) {}
    }

    /** Suggests medicines from the offline catalogue so the field is useful without a network. */
    private fun loadCatalogSuggestions() {
        try {
            val cached = dev.anonymous.eilaji.data.repository.CatalogCache.getMedicines(requireContext())
            val names = cached.map { it.titleEn.ifBlank { it.titleAr } }.filter { it.isNotBlank() }
            if (names.isNotEmpty()) {
                binding.actMedicineName.setSimpleItems(names.toTypedArray())
            }
        } catch (_: Exception) {
        }
    }

    private fun render() {
        val b = binding
        b.stepMedicine.visibility = if (step == 0) View.VISIBLE else View.GONE
        b.stepTime.visibility = if (step == 1) View.VISIBLE else View.GONE
        b.stepConfirm.visibility = if (step == 2) View.VISIBLE else View.GONE

        b.buWizardBack.visibility = if (step == 0) View.INVISIBLE else View.VISIBLE
        b.buWizardNext.text = getString(if (step == 2) R.string.save_label else R.string.continue_label)

        b.tvStepTitle.text = getString(
            when (step) {
                0 -> R.string.step_medicine
                1 -> R.string.step_time
                else -> R.string.step_confirm
            }
        )
        b.tvStepSubtitle.text = getString(
            when (step) {
                0 -> R.string.step_medicine_hint
                1 -> R.string.step_time_hint
                else -> R.string.step_confirm_hint
            }
        )

        // three-step rail: PENDING/DELIVERED stand in for step 1 and step 3
        b.stepperWizard.state = StepStepper.State.PENDING
        try {
            val field = StepStepper::class.java.getDeclaredField("stepperActive")
            field.isAccessible = true
        } catch (_: Exception) {
        }

        if (step == 2) fillConfirm()
    }

    private fun fillConfirm() {
        val b = binding
        val medicine = b.actMedicineName.text.toString().trim()
        val dosage = b.reminderDosageEditText.text.toString().trim()
        val time = b.doseWheel.selectedTimeText()
        b.tvConfirmMedicine.text = medicine.ifBlank { getString(R.string.medicine_name) }
        b.tvConfirmDosage.text = dosage.ifBlank { getString(R.string.dose_count) + ": $doseCount" }
        b.tvConfirmTime.text = when (repeatMode) {
            Repeat.DAILY -> getString(R.string.confirm_every_day_at, time)
            Repeat.WEEKLY -> getString(R.string.confirm_every_week_at, java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault()).format(Calendar.getInstance().time))
            Repeat.ONCE -> getString(R.string.confirm_once_at, time)
        }
        b.tvNextDose.text = getString(R.string.next_dose_in, timeUntilNext())
    }

    private fun timeUntilNext(): String {
        val (hour, minute) = binding.doseWheel.selectedTime()
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        var diff = (next.timeInMillis - now.timeInMillis) / 60000L
        val days = diff / (24 * 60)
        diff %= (24 * 60)
        val hours = diff / 60
        val minutes = diff % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    private fun goBack() {
        if (step == 0) {
            findNavController().popBackStack()
            return
        }
        step--
        render()
    }

    private fun goNext() {
        val b = binding
        when (step) {
            0 -> {
                if (b.actMedicineName.text.toString().trim().isEmpty()) {
                    Snackbar.make(b.root, R.string.medicine_required, Snackbar.LENGTH_SHORT).show()
                    return
                }
                hideKeyboard()
                step = 1
                render()
            }
            1 -> {
                step = 2
                render()
            }
            else -> save()
        }
    }

    private fun hideKeyboard() {
        try {
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(binding.root.windowToken, 0)
        } catch (_: Exception) {
        }
    }

    private fun save() {
        val b = binding
        val name = b.actMedicineName.text.toString().trim()
        val dosage = b.reminderDosageEditText.text.toString().trim().ifBlank { null }
        val isActive = b.switchIsActive.isChecked
        val (hour, minute) = b.doseWheel.selectedTime()
        val scheduleTime = String.format(java.util.Locale.US, "%02d:%02d:00", hour, minute)

        val freq = when (repeatMode) {
            Repeat.ONCE -> "ONCE"
            Repeat.DAILY -> "DAILY"
            Repeat.WEEKLY -> "WEEKLY"
        }

        val id = "eilaji_reminder_${java.util.UUID.randomUUID()}"
        val text = if (dosage != null) "$name — $dosage ($doseCount)" else "$name ($doseCount)"
        val reminder = Reminder(id, text, 60, if (freq == "DAILY") 1 else 2)
        reminder.medicineName = name
        reminder.dosage = dosage
        reminder.frequency = freq
        reminder.scheduleTime = scheduleTime
        reminder.setCustomDaysList(emptyList())
        reminder.setActive(isActive)
        reminder.startDate = System.currentTimeMillis()
        reminder.syncStatus = "PENDING"

        scheduler?.setReminderObject(reminder)
        if (isActive) {
            try {
                scheduler?.scheduleExact(reminder.id, text, reminder.notificationId, freq, "[]", scheduleTime)
                if (freq == "ONCE") scheduler?.scheduleReminderOneTimeWorkRequest()
                else scheduler?.scheduleReminderPeriodicWorkRequest(if (freq == "WEEKLY") 7 else 1, TimeUnit.DAYS)
            } catch (_: Exception) {
            }
        }

        // store through the same view model the old screen used
        try {
            androidx.lifecycle.ViewModelProvider(this)[ReminderViewModel::class.java]
                .storeReminderIntoDatabase(reminder)
        } catch (_: Exception) {
            try {
                dev.anonymous.eilaji.reminder_system.database.db.ReminderDatabase
                    .getDatabase(requireContext()).reminderDao().insertReminder(reminder)
            } catch (_: Exception) {
            }
        }
        syncRepo?.syncCreate(reminder) { ok -> if (ok) reminder.syncStatus = "SYNCED" }

        try {
            findNavController().popBackStack()
        } catch (_: Exception) {
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}