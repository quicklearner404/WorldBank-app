package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;

/**
 * SplashActivity
 * --------------
 * Shows logo for 2 seconds then routes:
 *   - If DEV_BYPASS = true  → HomeActivity (Dev 2 skip)
 *   - If already logged in  → HomeActivity
 *   - Otherwise             → OnboardingActivity
 *
 * Owner: Dev 1
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager sessionManager = new SessionManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;
            if (sessionManager.isLoggedIn()) {
                // DEV_BYPASS = true also routes here
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
