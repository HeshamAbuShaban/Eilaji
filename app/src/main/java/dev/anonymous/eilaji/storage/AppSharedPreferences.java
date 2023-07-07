package dev.anonymous.eilaji.storage;

import android.content.Context;
import android.content.SharedPreferences;


public class AppSharedPreferences {
    private enum SharedPreferencesKeys {
        /*invoked*/onBoardingDone, isFirstTime, token, fullName, imageUrl
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

    public String getImageUrl() {
        return sharedPreferences.getString(SharedPreferencesKeys.imageUrl.name(), "default");
    }

    public void putImageUrl(String token) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.imageUrl.name(), token);
        editor.apply();
    }

    public String getFullName() {
        return sharedPreferences.getString(SharedPreferencesKeys.fullName.name(), null);
    }

    public void putFullName(String token) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.fullName.name(), token);
        editor.apply();
    }

    public String getToken() {
        return sharedPreferences.getString(SharedPreferencesKeys.token.name(), null);
    }

    public void putToken(String token) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.token.name(), token);
        editor.apply();
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
