package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.worldbank.app.R;

public class VerifyOtpActivity extends AppCompatActivity {

    Button btnConfirm;
    TextView tvResendTimer;
    Toolbar toolbar;
    CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify_otp);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        startCountdown();

        // confirm goes back to login since firebase handles reset by email link
        btnConfirm.setOnClickListener(v -> {
            startActivity(new Intent(VerifyOtpActivity.this, LoginActivity.class));
            finish();
        });
    }

    void startCountdown() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                tvResendTimer.setText("Resend in 00:" + String.format("%02d", seconds));
            }

            @Override
            public void onFinish() {
                tvResendTimer.setText("Resend");
                // let user tap resend once timer is done
                tvResendTimer.setOnClickListener(v -> {
                    tvResendTimer.setOnClickListener(null);
                    startCountdown();
                });
            }
        }.start();
    }

    private void init() {
        btnConfirm = findViewById(R.id.btn_confirm);
        tvResendTimer = findViewById(R.id.tv_resend_timer);
        toolbar = findViewById(R.id.toolbar);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // cancel timer when activity closes to avoid memory leak
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}