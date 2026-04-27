package com.worldbank.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager.java
 * ---------------------
 * Manages user login session using SharedPreferences.
 */
public class SessionManager {

    // ─── REAL DATA MODE ────────────────────────────────────────────────
    // Set to false to use real Firebase Auth and real Firestore data.
    public static final boolean DEV_BYPASS = false;
    // ────────────────────────────────────────────────────────────────────

    private static final String PREF_NAME = "worldbank_session";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveSession(String userId, String email, String name, boolean rememberMe) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }

    public boolean isLoggedIn() {
        if (DEV_BYPASS) return true;
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserId() {
        if (DEV_BYPASS) return "dev_user_001";
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getUserEmail() {
        if (DEV_BYPASS) return "dev@worldbank.com";
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    public String getUserName() {
        if (DEV_BYPASS) return "Emmie Watson";
        return prefs.getString(KEY_USER_NAME, "");
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}