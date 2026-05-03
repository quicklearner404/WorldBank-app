package com.worldbank.app.utils;

import android.content.Context;
import android.content.SharedPreferences;


public class SessionManager {

    // set to false to use real Firebase and Firestore
    public static final boolean DEV_BYPASS = false;

    private static final String PREF_NAME             = "worldbank_session";
    private static final String KEY_IS_LOGGED_IN      = "is_logged_in";
    private static final String KEY_USER_ID           = "user_id";
    private static final String KEY_USER_EMAIL        = "user_email";
    private static final String KEY_USER_NAME         = "user_name";
    private static final String KEY_REMEMBER_ME       = "remember_me";
    private static final String KEY_REMEMBERED_EMAIL  = "remembered_email";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs  = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    // saves active login session
    public void saveSession(String userId, String email, String name, boolean rememberMe) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putBoolean(KEY_REMEMBER_ME, rememberMe);
        editor.apply();
    }

    // saves email separately so it survives logout and session clears
    public void saveRememberedEmail(String email) {
        editor.putString(KEY_REMEMBERED_EMAIL, email);
        editor.putBoolean(KEY_REMEMBER_ME, true);
        editor.apply();
    }

    // clears only the remembered email preference
    public void clearRememberedEmail() {
        editor.remove(KEY_REMEMBERED_EMAIL);
        editor.putBoolean(KEY_REMEMBER_ME, false);
        editor.apply();
    }

    // returns saved email or empty string if none
    public String getRememberedEmail() {
        return prefs.getString(KEY_REMEMBERED_EMAIL, "");
    }

    public boolean isRemembered() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
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

    // wipes active session but remembered email stays intact
    public void clearSession() {
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.remove(KEY_USER_ID);
        editor.remove(KEY_USER_NAME);
        editor.apply();
    }

    // wipes everything including remembered email on full logout
    public void clearAll() {
        editor.clear();
        editor.apply();
    }
}