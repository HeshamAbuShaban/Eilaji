package dev.anonymous.eilaji.ui.other.reminder.list

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.WorkInfo
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder
import dev.anonymous.eilaji.reminder_system.database.viewModel.ReminderDatabaseViewModel
import dev.anonymous.eilaji.reminder_system.worker.ReminderScheduler

class RemindersListViewModel:ViewModel() {
    //****************** value container for reminderScheduler ******************

    private val _reminderScheduler = MutableLiveData<ReminderScheduler>()
    private val reminderScheduler :LiveData<ReminderScheduler> = _reminderScheduler

    // setter for the reminder Scheduler
    fun setReminderScheduler(reminderScheduler: ReminderScheduler){
        _reminderScheduler.value = reminderScheduler
    }

    //****************** value container for databaseViewModel ******************
    private val _reminderDatabaseViewModel = MutableLiveData<ReminderDatabaseViewModel>()
    private val reminderDatabaseViewModel: LiveData<ReminderDatabaseViewModel> = _reminderDatabaseViewModel

    fun setupReminderDataViewModel(reminderDatabaseViewModel:ReminderDatabaseViewModel){
        _reminderDatabaseViewModel.value = reminderDatabaseViewModel
    }



    // Data Management

    fun getAllReminders() :LiveData<List<Reminder>>{
        return reminderDatabaseViewModel.value!!.allReminders
    }

    fun deleteReminder(reminder: Reminder){
        reminderDatabaseViewModel.value?.deleteReminder(reminder)

        //TODO Call the Cancel from the ReminderScheduler.kt

        Log.d("Tag","DataOfDeletedReminder , data: ${fetchWorkInfoByTag(reminder)}")
        cancelReminderById(reminder)
    }

    // when you want to cancel a reminder by de activate it from the UI Buttons
    private fun cancelReminderById(reminder: Reminder){
        reminderScheduler.value?.cancelReminderById(reminder)
    }

    // use it if you want to check the workInfo before deletion
    private fun fetchWorkInfoByTag(reminder: Reminder) : LiveData<List<WorkInfo>> {
        return reminderScheduler.value?.fetchWorkInfoByTag(reminder)!!
    }

}