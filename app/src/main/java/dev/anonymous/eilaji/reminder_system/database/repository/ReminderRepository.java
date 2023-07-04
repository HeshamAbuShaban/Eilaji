package dev.anonymous.eilaji.reminder_system.database.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

import dev.anonymous.eilaji.reminder_system.database.dao.ReminderDao;
import dev.anonymous.eilaji.reminder_system.database.db.ReminderDatabase;
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder;

public class ReminderRepository {
    private final ReminderDao reminderDao;

    public ReminderRepository(Application application) {
        ReminderDatabase db = ReminderDatabase.getDatabase(application);
        reminderDao = db.reminderDao();
    }

    public void insertReminder(Reminder reminder) {
        ReminderDatabase.databaseWriteExecutor.execute(() ->
                reminderDao.insertReminder(reminder)
        );
    }

    public void deleteReminder(Reminder reminder) {
        ReminderDatabase.databaseWriteExecutor.execute(() ->
                reminderDao.deleteReminder(reminder)
        );
    }

    public LiveData<List<Reminder>> getAllReminders() {
        return reminderDao.getAllReminders();
    }

}

