package dev.anonymous.eilaji.ui.other.reminder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentReminderBinding
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
import dev.anonymous.eilaji.reminder_system.repository.ReminderSyncRepository
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import dev.anonymous.eilaji.ui.other.dialogs.ChangeSoundDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.ChangeSoundDialogFragment.ChangeSoundListener
import dev.anonymous.eilaji.ui.other.dialogs.PeriodicReminderDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.PeriodicReminderDialogFragment.PeriodicReminderListener
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment.RequestPermissionsListener
import java.util.concurrent.TimeUnit

class ReminderFragment : Fragment(), PeriodicReminderListener, ChangeSoundListener, RequestPermissionsListener {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var reminderViewModel: ReminderViewModel
    private lateinit var binding: FragmentReminderBinding
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var reminderDatabaseViewModel: ReminderDatabaseViewModel
    private lateinit var syncRepo: ReminderSyncRepository
    private var selectedFrequency = "DAILY"
    private var customDays: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReminderBinding.inflate(layoutInflater)
        setupComponents()
        reminderViewModel.determinedTheBackGround(binding)
        return binding.root
    }
    private fun setupComponents() {
        reminderViewModel = ViewModelProvider(this)[ReminderViewModel::class.java]
        reminderScheduler = ReminderScheduler(requireContext().applicationContext)
        reminderViewModel.setReminderScheduler(reminderScheduler)
        reminderDatabaseViewModel = ViewModelProvider(this)[ReminderDatabaseViewModel::class.java]
        reminderViewModel.setDatabaseViewModel(reminderDatabaseViewModel)
        syncRepo = ReminderSyncRepository(requireContext().applicationContext)
    }
    override fun onResume() { super.onResume(); requireActivity().onBackPressedDispatcher.addCallback(this) { findNavController().popBackStack() } }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            if (perms.all { it.value }) Toast.makeText(requireContext(), "Great Now you are all set to use The Reminder", Toast.LENGTH_LONG).show()
            else Toast.makeText(requireContext(), "Permission denied. Cannot create reminder.", Toast.LENGTH_SHORT).show()
        }
        if (!arePermissionsGranted()) RequestPermissionsDialogFragment.newInstance(getString(R.string.permissions_message_starter)).show(childFragmentManager, "StarterInform")
        if (isBatteryOptimizationEnabled()) showBatteryOptimizationDialog()
        binding.switchIsActive.setOnCheckedChangeListener { _, _ -> }
        binding.textFrequencyPill.setOnClickListener { PeriodicReminderDialogFragment().show(childFragmentManager, "PeriodicReminder") }
    }
    private fun arePermissionsGranted() = REQUIRED_PERMISSIONS.all { ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED }
    private fun isBatteryOptimizationEnabled(): Boolean {
        val pm = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        return !pm.isIgnoringBatteryOptimizations(requireContext().packageName)
    }
    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(requireContext()).setTitle("Battery Optimization").setMessage(getString(R.string.battery_optimization))
            .setPositiveButton("Go to Settings") { _, _ -> startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .setCancelable(false).setNegativeButton("Cancel") { _, _ -> Toast.makeText(requireContext(), getString(R.string.battery_optimization_still_running), Toast.LENGTH_LONG).show() }.create().show()
    }
    private fun requestPermissions() { requestPermissionLauncher.launch(REQUIRED_PERMISSIONS) }
    private fun setupListeners() {
        with(binding) {
            remindOneTimeButton.setOnClickListener {
                selectedFrequency = "DAILY"; customDays = emptyList()
                textFrequencyPill.text = "DAILY"
                if (arePermissionsGranted()) createOneTimeReminder() else RequestPermissionsDialogFragment.newInstance(getString(R.string.permissions_message_we_are_sorry)).show(childFragmentManager, "WeAreSorry")
            }
            remindRepeatedlyButton.setOnClickListener { PeriodicReminderDialogFragment().show(childFragmentManager, "PeriodicReminder") }
            fabChangeSound.setOnClickListener { ChangeSoundDialogFragment().show(childFragmentManager, "ChangeReminderSound") }
        }
    }
    private fun createPeriodicReminder(repeatInterval: Long?, timeUnit: TimeUnit?) {
        selectedFrequency = when (timeUnit) {
            TimeUnit.DAYS -> "WEEKLY"
            TimeUnit.HOURS -> "DAILY"
            else -> if (repeatInterval != null && repeatInterval > 1) "CUSTOM" else "DAILY"
        }
        if (selectedFrequency == "CUSTOM") customDays = listOf("MONDAY","WEDNESDAY","FRIDAY") else customDays = emptyList()
        binding.textFrequencyPill.text = selectedFrequency
        createOneTimeReminder()
    }
    private fun createOneTimeReminder() {
        var txt = binding.reminderNameEditText.text.toString().trim()
        if (txt.isEmpty()) txt = getString(R.string.placeholder)
        val delay = reminderViewModel.calculateDelay(binding)
        val scheduleTime = reminderViewModel.buildScheduleTime(binding)
        val isActive = try { binding.switchIsActive.isChecked } catch (_: Exception) { true }
        val id = "eilaji_reminder_${reminderViewModel.randomUUIDString()}"
        val reminder = Reminder(id, txt, delay, if (selectedFrequency == "DAILY") 1 else 2)
        reminder.medicineName = txt
        reminder.dosage = null
        reminder.frequency = selectedFrequency
        reminder.scheduleTime = scheduleTime
        reminder.setCustomDaysList(customDays)
        reminder.setActive(isActive)
        reminder.startDate = System.currentTimeMillis()
        reminder.syncStatus = "PENDING"
        reminderViewModel.reminderScheduler.value?.setReminderObject(reminder)
        if (isActive) {
            if (selectedFrequency == "DAILY" || selectedFrequency == "WEEKLY" || selectedFrequency == "CUSTOM") reminderViewModel.reminderScheduler.value?.scheduleReminderPeriodicWorkRequest(1, TimeUnit.DAYS)
            else reminderViewModel.reminderScheduler.value?.scheduleReminderOneTimeWorkRequest()
        }
        reminderViewModel.storeReminderIntoDatabase(reminder)
        syncRepo.syncCreate(reminder) { ok -> if (ok) reminder.syncStatus = "SYNCED" }
        reminderViewModel.showRemainingTime(binding)
        Toast.makeText(requireContext(), "Reminder Saved.", Toast.LENGTH_SHORT).show()
        reminderViewModel.clearInputs(binding)
    }
    override fun collectUserPeriodicReminderListenerInputs(repeatInterval: Long?, timeUnit: TimeUnit?) {
        if (arePermissionsGranted()) createPeriodicReminder(repeatInterval, timeUnit) else RequestPermissionsDialogFragment.newInstance(getString(R.string.permissions_message_we_are_sorry)).show(childFragmentManager, "WeAreSorry")
    }
    override fun collectUserReminderSoundListenerInputs(soundId: Int) { reminderViewModel.reminderScheduler.value?.setReminderSound(soundId) }
    override fun onAllowClicked() { requestPermissions() }
    override fun onDenyClicked() { Toast.makeText(requireContext(), getString(R.string.permissions_message_sorry_you_can_not), Toast.LENGTH_SHORT).show() }
    companion object {
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.VIBRATE) else arrayOf(Manifest.permission.VIBRATE)
    }
}
