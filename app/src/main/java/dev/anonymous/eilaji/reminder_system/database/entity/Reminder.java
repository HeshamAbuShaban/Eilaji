package dev.anonymous.eilaji.reminder_system.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import dev.anonymous.eilaji.storage.AppSharedPreferences;
import dev.anonymous.eilaji.utils.AppController;

@Entity(tableName = "reminders")
public class Reminder implements ReminderContract {

    @NonNull
    @PrimaryKey
    private String id;
    @ColumnInfo(name = "ReminderText")
    private String text;
    @ColumnInfo(name = "RemainingTime")
    private long delayedTime;
    @ColumnInfo(name = "NotificationId")
    private int notificationId;
    @ColumnInfo(name = "CreationTimestamp")
    private long creationTimestamp; // Store the creation timestamp here

    @ColumnInfo(name = "ReminderType")
    private int reminderType;


    public Reminder(@NonNull String id, String text, long delayedTime, int reminderType) {
        this.id = id;
        this.text = text;
        this.delayedTime = delayedTime;
        this.reminderType = reminderType;
        this.notificationId = generateNotificationId();
        this.creationTimestamp = System.currentTimeMillis(); // Set the current timestamp during object creation
    }

    @NonNull
    @Override
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }
    @Override
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public long getDelayedTime() {
        return delayedTime;
    }

    public void setDelayedTime(long delayedTime) {
        this.delayedTime = delayedTime;
    }

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public int getReminderType() {
        return reminderType;
    }

    public void setReminderType(int reminderType) {
        this.reminderType = reminderType;
    }

    // Additional Column for the notification Id
    @Override
    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    private int generateNotificationId() {
        AppSharedPreferences preferences = AppSharedPreferences.getInstance(AppController.getInstance());
        int lastNotificationId = preferences.getLastNotificationId();
        int newNotificationId = lastNotificationId + 1;
        preferences.putNewNotificationId(newNotificationId);
        return newNotificationId;
    }
}
