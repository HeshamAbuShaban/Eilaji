package dev.anonymous.eilaji.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import dev.anonymous.eilaji.prefs.enums.PrefKeys;


public class AppSharedPreferences {
    private static AppSharedPreferences Instance;

    SharedPreferences sharedPreferences;
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

    /*`public void save(Student student) {
        editor = sharedPreferences.edit();
        editor.putBoolean(PrefKeys.logged_in.name(), true);
        editor.putInt(PrefKeys.id.name(), student.id);
        editor.putString(PrefKeys.fullName.name(), student.fullName);
        editor.putString(PrefKeys.email.name(), student.email);
        editor.putString(PrefKeys.token.name(), "Bearer " +student.token);
        editor.apply();
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
