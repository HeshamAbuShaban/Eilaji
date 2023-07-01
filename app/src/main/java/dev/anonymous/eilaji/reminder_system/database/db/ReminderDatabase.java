package dev.anonymous.eilaji.reminder_system.database.db;


import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.anonymous.eilaji.reminder_system.database.dao.ReminderDao;
import dev.anonymous.eilaji.reminder_system.database.entity.Reminder;

@Database(entities = {Reminder.class}, version = 1, exportSchema = false)
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

    private static ReminderDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(context, ReminderDatabase.class, "reminder.db")
                //.fallbackToDestructiveMigration()
                .build();
    }
}

