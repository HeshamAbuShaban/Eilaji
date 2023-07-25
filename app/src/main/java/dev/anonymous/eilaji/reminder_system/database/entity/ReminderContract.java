package dev.anonymous.eilaji.reminder_system.database.entity;

/** Embracing the principles of dependency injection
 *  and relying on abstractions like interfaces
 *  is a great practice that can lead to more maintainable and flexible code
 *  in the long run. Here are some benefits of using dependency injection and abstractions:
 * */
public interface ReminderContract {
    String getId();
    String getText();
    int getNotificationId();
    long getDelayedTime();
}
