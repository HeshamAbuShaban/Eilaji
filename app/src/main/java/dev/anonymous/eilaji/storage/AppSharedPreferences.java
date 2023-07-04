package dev.anonymous.eilaji.storage;

import android.content.Context;
import android.content.SharedPreferences;


public class AppSharedPreferences {
    private enum SharedPreferencesKeys {
        /*invoked*/onBoardingDone,isFirstTime
    }
    private static AppSharedPreferences Instance;
    private final SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private AppSharedPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    public static AppSharedPreferences getInstance(Context context) {
        if (Instance == null) {
            Instance = new AppSharedPreferences(context);
        }
        return Instance;
    }


    // FOR DATABASE NOTIFICATION ID CREATION
    public int getLastNotificationId() {
        return sharedPreferences.getInt("LAST_NOTIFICATION_ID", 0);
    }
    public void putNewNotificationId(int id) {
        editor = sharedPreferences.edit();
        editor.putInt("LAST_NOTIFICATION_ID", id);
        editor.apply();
    }

    //------------------------------------

    /*`public void invokeDummyData() {
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesKeys.invoked.name(), true);
        editor.apply();
    }

    public boolean isInvoked() {
        return sharedPreferences.getBoolean(SharedPreferencesKeys.invoked.name(), false);
    }*/

    public void doneWithOnBoarding() {
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesKeys.onBoardingDone.name(), true);
        editor.apply();
    }

    public boolean isDoneWithOnBoarding() {
        return sharedPreferences.getBoolean(SharedPreferencesKeys.onBoardingDone.name(), false);
    }

    public void setHeIsFirstTimeOut() {
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesKeys.isFirstTime.name(), true);
        editor.apply();
    }

    public void setHeIsFirstTimeDone() {
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesKeys.isFirstTime.name(), false);
        editor.apply();
    }

    public boolean isHeFirstTime() {
        return sharedPreferences.getBoolean(SharedPreferencesKeys.isFirstTime.name(), true);
    }

    // when user logout for instance
    public void clear() {
        editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

}
