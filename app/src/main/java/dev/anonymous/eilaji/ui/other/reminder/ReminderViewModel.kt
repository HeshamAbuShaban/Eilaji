package dev.anonymous.eilaji.ui.other.reminder

import android.widget.TextClock
import android.widget.TimePicker
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.anonymous.eilaji.R
import dev.anonymous.eilaji.databinding.FragmentReminderBinding
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class ReminderViewModel:ViewModel() {
    //****************** value container for reminderScheduler ******************

    private val _reminderScheduler = MutableLiveData<ReminderScheduler>()
    val reminderScheduler :LiveData<ReminderScheduler> = _reminderScheduler

    // setter for the reminder Scheduler
    fun setReminderScheduler(reminderScheduler: ReminderScheduler){
        _reminderScheduler.value = reminderScheduler
    }

    //****************** value container for databaseViewModel ******************

    private val _databaseViewModel = MutableLiveData<ReminderDatabaseViewModel>()
    private val databaseViewModel :LiveData<ReminderDatabaseViewModel> = _databaseViewModel

    // setter for the reminder Scheduler
    fun setDatabaseViewModel(databaseViewModel: ReminderDatabaseViewModel){
        _databaseViewModel.value = databaseViewModel
    }

    //****************** methods that serve the View ******************

    // false means day // true means night
    fun determinedTheBackGround(binding: FragmentReminderBinding){
        fun isDayOrNight() :Boolean {
            val cal = Calendar.getInstance()
            val hour = cal[Calendar.HOUR_OF_DAY]
            return hour < 6 || hour > 18
        }
        if (isDayOrNight()) binding.root.setBackgroundResource(R.drawable.bg_night) else binding.root.setBackgroundResource(R.drawable.bg_day)
    }
    // the actual time of the timepicker inputs
    fun calculateDelay(binding: FragmentReminderBinding): Long {
        // get to the widget
        val timePicker = binding.reminderTimePicker

        val currentTime = Calendar.getInstance()
        val reminderTime = Calendar.getInstance()

        // Set the reminder time based on the selected hour and minute from the TimePicker
        reminderTime.set(Calendar.HOUR_OF_DAY, timePicker.hour)
        reminderTime.set(Calendar.MINUTE, timePicker.minute)
        reminderTime.set(Calendar.SECOND, 0)

        // Calculate the delay in minutes
        val delayMillis = reminderTime.timeInMillis - currentTime.timeInMillis

//        Log.d(TAG, "delayMinutes= $delayMinutes")

        return TimeUnit.MILLISECONDS.toMinutes(delayMillis)
    }
    // shows the remainingTime unital the Worker fire up
    fun showRemainingTime(binding: FragmentReminderBinding) {
        val timePicker : TimePicker
        val remainingTimeTV : TextClock
        // init the widgets in the viewModel
        with(binding){
            timePicker =reminderTimePicker
            remainingTimeTV = remainingTimeTextView
        }

        // init the Calendar Values
        val currentTime = Calendar.getInstance()
        val reminderTime = Calendar.getInstance()

        // Set the reminder time based on the selected hour and minute from the TimePicker
        reminderTime.set(Calendar.HOUR_OF_DAY, timePicker.hour)
        reminderTime.set(Calendar.MINUTE, timePicker.minute)
        reminderTime.set(Calendar.SECOND, 0)

        // calculate the actual value
        val remainingMillis = reminderTime.timeInMillis - currentTime.timeInMillis

        // Convert remaining time to hours and minutes
        val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
        val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60

        val remainingTime = String.format("%02d:%02d", remainingHours, remainingMinutes)
        // Display the remaining time to the user
        remainingTimeTV.text = remainingTime
    }
    // clear the user inputs
    fun clearInputs(binding: FragmentReminderBinding) {
        with(binding){
            // Clear input fields for anther Queue request
            reminderNameEditText.text?.clear()
            reminderTimePicker.clearFocus()
        }

    }
    // creates random Unique id
    fun randomUUIDString(): String {
        return UUID.randomUUID().toString()
    }

    /*fun storeReminderIntoDatabase(reminderText: String, delayMinutes: Long) {
        //Lets save it to the database :
        val generatedId = "eilaji_reminder_${randomUUIDString()}"
        databaseViewModel.value?.insertReminder(
            Reminder(
                generatedId,
                reminderText,
                delayMinutes.toString()
            )
        )
    }*/

    // temp
    fun storeReminderIntoDatabase(reminder: Reminder) {
        //Lets save it to the database :
        databaseViewModel.value?.insertReminder(reminder)
    }

    // These Are Moved to the RemindersListFragment.kt
   /* // when you want to cancel a reminder by de activate it from the UI Buttons
    fun cancelReminderById(reminder: Reminder){
        reminderScheduler.value?.cancelReminderById(reminder)
    }

    // use it if you want to check the workInfo before deletion
    fun fetchWorkInfoByTag(reminder: Reminder) : LiveData<List<WorkInfo>> {
        return reminderScheduler.value?.fetchWorkInfoByTag(reminder)!!
    }*/
}