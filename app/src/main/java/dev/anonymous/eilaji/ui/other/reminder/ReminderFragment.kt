package dev.anonymous.eilaji.ui.other.reminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import dev.anonymous.eilaji.databinding.FragmentReminderBinding
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import dev.anonymous.eilaji.ui.other.dialogs.PeriodicReminderDialogFragment
import dev.anonymous.eilaji.ui.other.dialogs.PeriodicReminderDialogFragment.PeriodicReminderListener
import java.util.concurrent.TimeUnit

class ReminderFragment : Fragment() ,PeriodicReminderListener  {

    private lateinit var reminderViewModel: ReminderViewModel
    private lateinit var binding: FragmentReminderBinding
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var reminderDatabaseViewModel: ReminderDatabaseViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        with(binding) {
            // the button submit 
            remindOneTimeButton.setOnClickListener {
                // TODO: Build an Object of the model and save it to the database and send it data to the worker
                //  { pill_id, pill_name ,pill dosage, repeated_time , every-which-time }

                // user text
                var reminderText = binding.reminderNameEditText.text.toString()

                // the time that the worker will wait for to do it work
                val delayMinutes = reminderViewModel.calculateDelay(binding)

                // if OneTimeWorkRequest or
                // guard the null
                if (reminderText.isEmpty()) reminderText = "set_to_null"
                val generatedId = "eilaji_reminder_${reminderViewModel.randomUUIDString()}"
                val reminder = Reminder(generatedId,reminderText,delayMinutes.toString())

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

            remindRepeatedlyButton.setOnClickListener {

                // TODO : this now in the Dialog Listener
                /*// Sets it for a repeated time
                 reminderViewModel.reminderScheduler.value?.scheduleReminderPeriodicWorkRequest()*/
                PeriodicReminderDialogFragment().show(childFragmentManager,"PeriodicReminder")

            }

        }
    }

    //DO NOT FORGET TO SET THE REMINDER OBJECT TO ReminderScheduler Class before any action
    override fun collectUserPeriodicReminderListenerInputs(repeatInterval: Long?, timeUnit: TimeUnit?) {
        // user text
        var reminderText = binding.reminderNameEditText.text.toString()

        // the time that the worker will wait for to do it work
        val delayMinutes = reminderViewModel.calculateDelay(binding)


        // guard the null
        if (reminderText.isEmpty()) reminderText = "set_to_null"
        val generatedId = "eilaji_reminder_${reminderViewModel.randomUUIDString()}"
        val reminder = Reminder(generatedId,reminderText,delayMinutes.toString())

        // Set The Scheduler for the user reminder fromViewModel
        reminderViewModel.reminderScheduler.value?.setReminderObject(reminder = reminder)

        val isNull:Boolean = reminderViewModel.reminderScheduler.value?.isReminderObjNull() == true
        if (!isNull){
            // Sets it for a repeated time
            reminderViewModel.reminderScheduler.value?.scheduleReminderPeriodicWorkRequest(repeatInterval!!,timeUnit!!)
        }
        reminderViewModel.storeReminderIntoDatabase(reminder)
        // Show Remaining Time in TextClock
        reminderViewModel.showRemainingTime(binding)

        // toast that shows the request been made and the reminder been queued
        Toast.makeText(requireContext(), "Reminder Saved.", Toast.LENGTH_SHORT).show()

        // helps to clear the inputs
        reminderViewModel.clearInputs(binding)
    }

}