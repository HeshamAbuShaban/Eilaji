package dev.anonymous.eilaji.reminder_system.database.db;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.anonymous.eilaji.reminder_system.database.Converters;
import dev.anonymous.eilaji.reminder_system.database.dao.ReminderDao;
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder;

@Database(entities = {Reminder.class}, version = 2, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class ReminderDatabase extends RoomDatabase {
    public abstract ReminderDao reminderDao();

    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
    private static volatile ReminderDatabase instance;

    public static ReminderDatabase getDatabase(Context context) {
        if (instance == null) {
            synchronized (ReminderDatabase.class) {
                if (instance == null) {
                    instance = buildDatabase(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /**
     * A schema bump must never wipe a patient's medication reminders: they are health data and
     * they are not stored on the server. Add a Migration for every version change instead of a
     * destructive fallback, and keep the backups the app writes when reminders change.
     */
    private static ReminderDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(context, ReminderDatabase.class, "reminder.db")
                .build();
    }
}

