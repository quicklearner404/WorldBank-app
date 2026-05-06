package com.worldbank.app.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.activities.home.HomeActivity;
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SessionManager session = new SessionManager(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent;

            if (session.isLoggedIn()) {
                // active session exists, skip to home
                intent = new Intent(SplashActivity.this, HomeActivity.class);
            } else {
                // no session, go to onboarding
                intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            }

            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}