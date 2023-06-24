package dev.anonymous.eilaji.storage;

import android.content.Context;
import android.content.SharedPreferences;


public class AppSharedPreferences {
    private enum SharedPreferencesKeys {
        invoked,onBoardingDone
    }
    private static AppSharedPreferences Instance;
    private final SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    private AppSharedPreferences(Context context) {
        sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
    }

    public static AppSharedPreferences getInstance(Context context) {
        if (Instance == null) {
            Instance = new AppSharedPreferences(context);
        }
        return Instance;
    }

    /*`public void save(Object o) {
        editor = sharedPreferences.edit();
        editor.putString("", o.toString());
        editor.apply();
    }*/

    public void invokeDummyData() {
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesKeys.invoked.name(), true);
        editor.apply();
    }

    public boolean isInvoked() {
        return sharedPreferences.getBoolean(SharedPreferencesKeys.invoked.name(), false);
    }

    public void doneWithOnBoarding() {
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesKeys.onBoardingDone.name(), true);
        editor.apply();
    }

    public boolean isDoneWithOnBoarding() {
        return sharedPreferences.getBoolean(SharedPreferencesKeys.onBoardingDone.name(), false);
    }

    /*`public void save(Student student) {
        editor = sharedPreferences.edit();
        editor.putBoolean(PrefKeys.logged_in.name(), true);
        editor.putInt(PrefKeys.id.name(), student.id);
        editor.putString(PrefKeys.fullName.name(), student.fullName);
        editor.putString(PrefKeys.email.name(), student.email);
        editor.putString(PrefKeys.token.name(), "Bearer " +student.token);
        editor.apply();
    }*/

    /*`public String getStringForKey(PrefKeys key) {
        return sharedPreferences.getString(key.name(), "NotFound");
    }*/

    /*`public String  getToken(){
        return sharedPreferences.getString(PrefKeys.token.name(), "");
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(PrefKeys.logged_in.name(), false);
    }*/

    public void clear() {
        editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

}
