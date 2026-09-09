package dev.anonymous.eilaji.utils;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import dev.anonymous.eilaji.storage.AppSharedPreferences;

public class AppController extends Application {

    private static AppController Instance;

    @Override
    public void onCreate() {
        super.onCreate();
        Instance = this;
        applySavedTheme();
    }

    private void applySavedTheme() {
        String theme = AppSharedPreferences.getInstance(this).getTheme();
        applyTheme(theme);
    }

    public static void applyTheme(String theme) {
        int mode;
        if (AppSharedPreferences.Theme.dark.name().equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if (AppSharedPreferences.Theme.light.name().equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
        } else {
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static AppController getInstance() {
        if (Instance != null) {
            return Instance;
        }
        return null;
    }
}
