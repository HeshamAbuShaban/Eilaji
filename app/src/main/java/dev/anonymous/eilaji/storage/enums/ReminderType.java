package dev.anonymous.eilaji.storage.enums;

public enum ReminderType {

    OneTime(1),
    Periodic(2);

    public final int reminderType;
    ReminderType(int reminderType){
        this.reminderType = reminderType;
    }
}
