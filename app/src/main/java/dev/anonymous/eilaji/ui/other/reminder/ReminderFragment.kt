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
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import dev.anonymous.eilaji.storage.enums.ReminderType
import dev.anonymous.eilaji.ui.other.dialogs.ChangeSoundDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.ChangeSoundDialogFragment.ChangeSoundListener
import dev.anonymous.eilaji.ui.other.dialogs.PeriodicReminderDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.PeriodicReminderDialogFragment.PeriodicReminderListener
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.permissions.RequestPermissionsDialogFragment.RequestPermissionsListener
import java.util.concurrent.TimeUnit

class ReminderFragment : Fragment(), PeriodicReminderListener, ChangeSoundListener,
    RequestPermissionsListener {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var reminderViewModel: ReminderViewModel
    private lateinit var binding: FragmentReminderBinding
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var reminderDatabaseViewModel: ReminderDatabaseViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // init the binding
        binding = FragmentReminderBinding.inflate(layoutInflater)
        // set up the view model and the ReminderScheduler Instance Object
        setupComponents()
        // set the back ground from the view model
        reminderViewModel.determinedTheBackGround(binding)
        return binding.root
    }

    // init the reminderViewModel(for fragment) , reminderScheduler , ReminderDatabaseViewModel (database container)
    private fun setupComponents() {
        // view ViewModel
        reminderViewModel = ViewModelProvider(this)[ReminderViewModel::class.java]
        // rs Reminder Scheduler
        reminderScheduler = ReminderScheduler(requireContext().applicationContext)
        // set it to the v_view model
        reminderViewModel.setReminderScheduler(reminderScheduler)
        //  DATABASE VIEW_MODEL
        reminderDatabaseViewModel =
            ViewModelProvider(this)[ReminderDatabaseViewModel::class.java]
        reminderViewModel.setDatabaseViewModel(reminderDatabaseViewModel)
    }

    // TODO(Under Testing)
    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(this) { findNavController().popBackStack() }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        // initialize the requestPermissionLauncher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                // Check if all permissions are granted
                val allGranted = permissions.all { it.value }
                if (allGranted) {
                    // All permissions granted, you can proceed with sending notifications
                    Toast.makeText(
                        requireContext(),
                        "Great Now you are all set to use The Reminder", Toast.LENGTH_LONG).show()
                } else {
                    // Permission denied, handle accordingly
                    // At least one permission denied, handle accordingly (e.g., show a message or disable certain features)
                    Toast.makeText(requireContext(), "Permission denied. Cannot create reminder.", Toast.LENGTH_SHORT).show()
                }
            }
        // Request permissions if not granted
        if (!arePermissionsGranted()) {
            RequestPermissionsDialogFragment.newInstance(getString(R.string.permissions_message_starter)).show(childFragmentManager, "StarterInform")
        }
        // if the BatteryOptimization Enabled
        if (isBatteryOptimizationEnabled()) {
            showBatteryOptimizationDialog()
        }
    }

    // are the permissions granted ?
    private fun arePermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /*private fun isBatteryOptimizationEnabled(): Boolean {
        val powerManager = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isPowerSaveMode
    }*/
    private fun isBatteryOptimizationEnabled(): Boolean {
        val powerManager = requireActivity().getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = requireContext().packageName
        return !powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    // this dialog if user is having our app in the battery optimization system
    private fun showBatteryOptimizationDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Battery Optimization")
        dialogBuilder.setMessage(getString(R.string.battery_optimization))
        dialogBuilder.setPositiveButton("Go to Settings") { _, _ ->
            // Open battery optimization settings screen
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
        dialogBuilder.setCancelable(false) // this is to prevent the user from killing the dialog
        dialogBuilder.setNegativeButton("Cancel") { _, _ ->
            Toast.makeText(requireContext(), getString(R.string.battery_optimization_still_running), Toast.LENGTH_LONG).show()
        }
        dialogBuilder.create().show()
    }


    // Handle the permission request result
    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    // set the user click from UI ------------
    private fun setupListeners() {
        with(binding) {
            remindOneTimeButton.setOnClickListener {
                if (arePermissionsGranted()) {
                    createOneTimeReminder()
                } else {
                    RequestPermissionsDialogFragment.newInstance(getString(R.string.permissions_message_we_are_sorry))
                        .show(childFragmentManager, "WeAreSorry")
                }
            }

            remindRepeatedlyButton.setOnClickListener {
                PeriodicReminderDialogFragment().show(childFragmentManager, "PeriodicReminder")
            }

            fabChangeSound.setOnClickListener {
                ChangeSoundDialogFragment().show(childFragmentManager, "ChangeReminderSound")
            }
        }
    }

    // Creation for the Reminder $$$$$$$$$$$$$$$
    private fun createPeriodicReminder(repeatInterval: Long?, timeUnit: TimeUnit?) {
        // user text
        var reminderText = binding.reminderNameEditText.text.toString()

        // the time that the worker will wait for to do it work
        val delayMinutes = reminderViewModel.calculateDelay(binding)


        // guard the null
        if (reminderText.isEmpty()) reminderText = getString(R.string.placeholder)
        val generatedId = "eilaji_reminder_${reminderViewModel.randomUUIDString()}"
        val reminder = Reminder(
            generatedId,
            reminderText,
            delayMinutes,
            ReminderType.Periodic.reminderType
        )

        // Set The Scheduler for the user reminder fromViewModel
        reminderViewModel.reminderScheduler.value?.setReminderObject(reminder = reminder)

        // Sets it for a repeated time
        reminderViewModel.reminderScheduler.value?.scheduleReminderPeriodicWorkRequest(
            repeatInterval!!,
            timeUnit!!
        ) // don't worry for the force-null cuz i got some init values in the main obj

        reminderViewModel.storeReminderIntoDatabase(reminder)
        // Show Remaining Time in TextClock
        reminderViewModel.showRemainingTime(binding)

        // toast that shows the request been made and the reminder been queued
        Toast.makeText(requireContext(), "Reminder Saved.", Toast.LENGTH_SHORT).show()

        // helps to clear the inputs
        reminderViewModel.clearInputs(binding)
    }

    private fun createOneTimeReminder() {
        // user text
        var reminderText = binding.reminderNameEditText.text.toString()

        // the time that the worker will wait for to do it work
        val delayMinutes = reminderViewModel.calculateDelay(binding)

        // if OneTimeWorkRequest or
        // guard the null
        if (reminderText.isEmpty()) reminderText = getString(R.string.placeholder)
        val generatedId = "eilaji_reminder_${reminderViewModel.randomUUIDString()}"
        val reminder = Reminder(
            generatedId,
            reminderText,
            delayMinutes,
            ReminderType.OneTime.reminderType
        )

        // Set The Scheduler for the user reminder fromViewModel
        reminderViewModel.reminderScheduler.value?.setReminderObject(reminder = reminder)
        reminderViewModel.reminderScheduler.value?.scheduleReminderOneTimeWorkRequest()

        // Show Remaining Time in TextClock
        reminderViewModel.showRemainingTime(binding)


        //Lets save it to the database :
        reminderViewModel.storeReminderIntoDatabase(reminder)
        // reminderViewModel.storeReminderIntoDatabase(reminderText,delayMinutes)

        // toast that shows the request been made and the reminder been queued
        Toast.makeText(requireContext(), "Reminder Saved.", Toast.LENGTH_SHORT).show()

        // helps to clear the inputs
        reminderViewModel.clearInputs(binding)
    }

    // ## Dialog Interfaces Listeners ##############
    override fun collectUserPeriodicReminderListenerInputs(
        repeatInterval: Long?,
        timeUnit: TimeUnit?,
    ) {
        // Check if the required permissions are granted
        if (arePermissionsGranted()) {
            createPeriodicReminder(repeatInterval, timeUnit)
        } else {
            RequestPermissionsDialogFragment.newInstance(getString(R.string.permissions_message_we_are_sorry))
                .show(childFragmentManager, "WeAreSorry")
        }
    }

    override fun collectUserReminderSoundListenerInputs(soundId: Int) {
        reminderViewModel.reminderScheduler.value?.setReminderSound(soundId)
    }

    // Permissions Handler from the user interact with the dialog
    override fun onAllowClicked() {
        requestPermissions()
    }

    override fun onDenyClicked() {
        Toast.makeText(requireContext(), getString(R.string.permissions_message_sorry_you_can_not), Toast.LENGTH_SHORT).show()
    }

    companion object {
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.VIBRATE,
                )
            } else {
                arrayOf(
                    Manifest.permission.VIBRATE,
                )
            }
    }
}