package com.worldbank.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SessionManager.java
 * ---------------------
 * Manages user login session using SharedPreferences.
 *
 * DEV BYPASS:
 *   Set DEV_BYPASS = true so Dev 2 can work on main app screens
 *   without needing a real Firebase login. When true, SplashActivity
 *   will skip directly to HomeActivity with fake user data.
 *
 *   ⚠️  Set DEV_BYPASS = false before final build / production release.
 */
public class SessionManager {

    // ─── DEV BYPASS FLAG ────────────────────────────────────────────────
    // Dev 2: set this to true to skip login screens during development
    // Dev 1: keep this false when working on auth flow
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

    /** Save a logged-in user session */
    public void saveSession(String userId, String email, String name, boolean rememberMe) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }

    /** Check if user is currently logged in */
    public boolean isLoggedIn() {
        if (DEV_BYPASS) return true;
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /** Get stored user ID */
    public String getUserId() {
        if (DEV_BYPASS) return "dev_user_001";
        return prefs.getString(KEY_USER_ID, "");
    }

    /** Get stored user email */
    public String getUserEmail() {
        if (DEV_BYPASS) return "dev@worldbank.com";
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    /** Get stored user name */
    public String getUserName() {
        if (DEV_BYPASS) return "Emmie Watson";
        return prefs.getString(KEY_USER_NAME, "");
    }

    /** Check if Remember Me was selected */
    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    /** Clear session on logout */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    /** Logout helper — clears session */
    public void logout() {
        clearSession();
    }
}
