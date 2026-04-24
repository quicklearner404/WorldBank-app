package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;

/**
 * OnboardingActivity
 * TODO: Implement UI and logic based on project documentation.
 */
public class OnboardingActivity extends AppCompatActivity {
    Button btn_getStarted;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);
        init();
    }
    protected void init() {
        btn_getStarted = findViewById(R.id.btn_get_started);
        btn_getStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(OnboardingActivity.this, WelcomeActivity.class);
                startActivity(intent);
            }
        });
    }
}
