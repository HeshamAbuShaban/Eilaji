package dev.anonymous.eilaji.reminder_system.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import dev.anonymous.eilaji.storage.AppSharedPreferences;
import dev.anonymous.eilaji.utils.AppController;
import dev.anonymous.eilaji.reminder_system.database.Converters;
import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "reminders")
public class Reminder implements ReminderContract {

    @NonNull @PrimaryKey private String id;
    @ColumnInfo(name = "ReminderText") private String text;
    @ColumnInfo(name = "RemainingTime") private long delayedTime;
    @ColumnInfo(name = "NotificationId") private int notificationId;
    @ColumnInfo(name = "CreationTimestamp") private long creationTimestamp;
    @ColumnInfo(name = "ReminderType") private int reminderType;

    @ColumnInfo(name = "medicineName") private String medicineName;
    @ColumnInfo(name = "dosage") private String dosage;
    @ColumnInfo(name = "frequency") private String frequency;
    @ColumnInfo(name = "scheduleTime") private String scheduleTime;
    @ColumnInfo(name = "customDays") private String customDays;
    @ColumnInfo(name = "notes") private String notes;
    @ColumnInfo(name = "isActive") private boolean isActive;
    @ColumnInfo(name = "startDate") private Long startDate;
    @ColumnInfo(name = "endDate") private Long endDate;
    @ColumnInfo(name = "backendId") private String backendId;
    @ColumnInfo(name = "syncStatus") private String syncStatus;

    public Reminder(@NonNull String id, String text, long delayedTime, int reminderType) {
        this.id = id;
        this.text = text;
        this.delayedTime = delayedTime;
        this.reminderType = reminderType;
        this.notificationId = generateNotificationId();
        this.creationTimestamp = System.currentTimeMillis();
        this.medicineName = text;
        this.frequency = reminderType == 2 ? "DAILY" : "DAILY";
        this.scheduleTime = "08:00:00";
        this.customDays = "[]";
        this.isActive = true;
        this.syncStatus = "PENDING";
        this.startDate = System.currentTimeMillis();
    }

    @Ignore
    public Reminder(@NonNull String id, String medicineName, String dosage, String frequency, String scheduleTime, List<String> days, String notes, boolean isActive) {
        this.id = id;
        this.text = medicineName;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.frequency = frequency == null ? "DAILY" : frequency.toUpperCase();
        this.scheduleTime = scheduleTime == null ? "08:00:00" : scheduleTime;
        this.customDays = Converters.fromList(days);
        this.notes = notes;
        this.isActive = isActive;
        this.reminderType = frequency != null && frequency.equalsIgnoreCase("CUSTOM") ? 2 : 1;
        this.delayedTime = 0;
        this.notificationId = generateNotificationId();
        this.creationTimestamp = System.currentTimeMillis();
        this.syncStatus = "PENDING";
        this.startDate = System.currentTimeMillis();
    }

    @NonNull @Override public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }
    @Override public String getText() { return text; }
    public void setText(String text) { this.text = text; if (medicineName==null||medicineName.isEmpty()) medicineName=text; }
    @Override public long getDelayedTime() { return delayedTime; }
    public void setDelayedTime(long v) { this.delayedTime = v; }
    public long getCreationTimestamp() { return creationTimestamp; }
    public void setCreationTimestamp(long v) { this.creationTimestamp = v; }
    public int getReminderType() { return reminderType; }
    public void setReminderType(int v) { this.reminderType = v; }
    @Override public int getNotificationId() { return notificationId; }
    public void setNotificationId(int v) { this.notificationId = v; }
    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String v) { medicineName = v; if (text==null||text.isEmpty()) text=v; }
    public String getDosage() { return dosage; }
    public void setDosage(String v) { dosage = v; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String v) { frequency = v==null? "DAILY": v.toUpperCase(); }
    public String getScheduleTime() { return scheduleTime; }
    public void setScheduleTime(String v) { scheduleTime = v; }
    public String getCustomDays() { return customDays; }
    public void setCustomDays(String v) { customDays = v==null?"[]":v; }
    public List<String> getCustomDaysList() { return Converters.toList(customDays); }
    public void setCustomDaysList(List<String> l) { customDays = Converters.fromList(l); }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    @Ignore public boolean getIsActive() { return isActive; }
    public boolean isActive() { return isActive; }
    @Ignore public void setIsActive(boolean v) { isActive = v; }
    public void setActive(boolean v) { isActive = v; }
    public Long getStartDate() { return startDate; }
    public void setStartDate(Long v) { startDate = v; }
    public Long getEndDate() { return endDate; }
    public void setEndDate(Long v) { endDate = v; }
    public String getBackendId() { return backendId; }
    public void setBackendId(String v) { backendId = v; }
    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String v) { syncStatus = v; }

    private int generateNotificationId() {
        try {
            AppSharedPreferences p = AppSharedPreferences.getInstance(AppController.getInstance());
            int last = p.getLastNotificationId();
            int nid = last + 1;
            p.putNewNotificationId(nid);
            return nid;
        } catch (Exception e) { return (int)(System.currentTimeMillis()%2147483647); }
    }
}
