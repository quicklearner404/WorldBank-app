package com.worldbank.app.activities.account;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.worldbank.app.R;
import com.worldbank.app.activities.auth.ForgotPasswordActivity;
public class PrivacySecurityActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy_security);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        LinearLayout llChangePassword = findViewById(R.id.llChangePassword);
        if (llChangePassword != null) {
            llChangePassword.setOnClickListener(v -> {
                Intent intent = new Intent(this, ForgotPasswordActivity.class);
                // tells ForgotPasswordActivity it was opened from settings
                intent.putExtra("mode", "change");
                startActivity(intent);
            });
        }
    }
}