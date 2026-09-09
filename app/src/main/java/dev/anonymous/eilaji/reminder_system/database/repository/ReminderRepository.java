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
    public void insertReminder(Reminder r) { ReminderDatabase.databaseWriteExecutor.execute(() -> reminderDao.insertReminder(r)); }
    public void deleteReminder(Reminder r) { ReminderDatabase.databaseWriteExecutor.execute(() -> reminderDao.deleteReminder(r)); }
    public void updateReminder(Reminder r) { ReminderDatabase.databaseWriteExecutor.execute(() -> reminderDao.updateReminder(r)); }
    public LiveData<List<Reminder>> getAllReminders() { return reminderDao.getAllReminders(); }
    public LiveData<List<Reminder>> getActiveReminders() { return reminderDao.getActiveReminders(); }
    public List<Reminder> getAllSync() { return reminderDao.getAllSync(); }
}
