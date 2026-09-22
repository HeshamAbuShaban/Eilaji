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
            // system + dynamic both follow night state; dynamic additionally tints Material colors
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    public static boolean isDynamicTheme(android.content.Context context) {
        try {
            return AppSharedPreferences.Theme.dynamic.name().equals(AppSharedPreferences.getInstance(context).getTheme());
        } catch (Exception e) {
            return false;
        }
    }

    /** Material You opt-in: applies wallpaper-seeded colors to this activity. Safe no-op pre-S. */
    public static void applyDynamicIfEnabled(android.app.Activity activity) {
        try {
            if (activity != null && isDynamicTheme(activity)) {
                com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(activity);
            }
        } catch (Exception ignored) {}
    }

    public static AppController getInstance() {
        if (Instance != null) {
            return Instance;
        }
        return null;
    }
}
