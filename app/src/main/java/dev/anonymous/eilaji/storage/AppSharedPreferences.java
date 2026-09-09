package dev.anonymous.eilaji.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;


public class AppSharedPreferences {
    private enum SharedPreferencesKeys {
        onBoardingDone, isFirstTime, token, refreshToken, fcmToken, fullName, imageUrl, currentUserChattingUid, userId, phone, role, isVerified, isActive
    }

    private static AppSharedPreferences Instance;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private AppSharedPreferences(Context context) {
        SharedPreferences tmp = null;
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
            tmp = EncryptedSharedPreferences.create(
                context,
                "secure_app_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            tmp = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        }
        sharedPreferences = tmp;
    }

    public static AppSharedPreferences getInstance(Context context) {
        if (Instance == null) {
            Instance = new AppSharedPreferences(context);
        }
        return Instance;
    }

    public String getCurrentUserChattingUID() {
        return sharedPreferences.getString(SharedPreferencesKeys.currentUserChattingUid.name(), "");
    }

    public void putCurrentUserChattingUID(String uid) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.currentUserChattingUid.name(), uid);
        editor.apply();
    }

    public void removeCurrentUserChattingUID() {
        editor = sharedPreferences.edit();
        editor.remove(SharedPreferencesKeys.currentUserChattingUid.name());
        editor.apply();
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

    public String getRefreshToken() {
        return sharedPreferences.getString(SharedPreferencesKeys.refreshToken.name(), null);
    }

    public void putRefreshToken(String token) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.refreshToken.name(), token);
        editor.apply();
    }

    public String getFcmToken() {
        return sharedPreferences.getString(SharedPreferencesKeys.fcmToken.name(), null);
    }

    public void putFcmToken(String token) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.fcmToken.name(), token);
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
    // Secure user data methods
    //------------------------------------

    public String getUserId() {
        return sharedPreferences.getString(SharedPreferencesKeys.userId.name(), null);
    }

    public void putUserId(String userId) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.userId.name(), userId);
        editor.apply();
    }

    public String getPhone() {
        return sharedPreferences.getString(SharedPreferencesKeys.phone.name(), null);
    }

    public void putPhone(String phone) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.phone.name(), phone);
        editor.apply();
    }

    public String getRole() {
        return sharedPreferences.getString(SharedPreferencesKeys.role.name(), null);
    }

    public void putRole(String role) {
        editor = sharedPreferences.edit();
        editor.putString(SharedPreferencesKeys.role.name(), role);
        editor.apply();
    }

    public boolean isVerified() {
        return sharedPreferences.getBoolean(SharedPreferencesKeys.isVerified.name(), false);
    }

    public void putIsVerified(boolean isVerified) {
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesKeys.isVerified.name(), isVerified);
        editor.apply();
    }

    public boolean isActive() {
        return sharedPreferences.getBoolean(SharedPreferencesKeys.isActive.name(), true);
    }

    public void putIsActive(boolean isActive) {
        editor = sharedPreferences.edit();
        editor.putBoolean(SharedPreferencesKeys.isActive.name(), isActive);
        editor.apply();
    }

    public void clearAll() {
        editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    //------------------------------------
    // Legacy methods that were commented out
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
