package dev.anonymous.eilaji.reminder_system.database.viewModel;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import dev.anonymous.eilaji.reminder_system.database.entity.Reminder;
import dev.anonymous.eilaji.reminder_system.database.repository.ReminderRepository;


public class ReminderDatabaseViewModel extends AndroidViewModel {
    private final ReminderRepository repository;

    public ReminderDatabaseViewModel(@NonNull Application application) {
        super(application);
        repository = new ReminderRepository(application);
    }

    LiveData<List<Reminder>> getAllReminders(){
        return repository.getAllReminders();
    }

    public void insertReminder(Reminder reminder) {
        repository.insertReminder(reminder);
    }

    public void deleteReminder(Reminder reminder) {
        repository.deleteReminder(reminder);
    }

}