package dev.anonymous.eilaji.reminder_system.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import dev.anonymous.eilaji.reminder_system.database.entity.Reminder;

@Dao
public interface ReminderDao {

    @Query("SELECT * FROM reminders")
    LiveData<List<Reminder>> getAllReminders();


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReminder(Reminder reminder);

    @Delete
    void deleteReminder(Reminder reminder);

    @Update
    void updateReminder(Reminder reminder);

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    LiveData<List<Reminder>> getActiveReminders();

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    Reminder getByIdSync(String id);

    @Query("SELECT * FROM reminders")
    List<Reminder> getAllSync();

}