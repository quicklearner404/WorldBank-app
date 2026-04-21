package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.worldbank.app.R;

public class PaymentsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        BottomNavigationView bottomNavView = findViewById(R.id.bottomNavView);
        if (bottomNavView != null) {
            bottomNavView.setSelectedItemId(R.id.nav_statistic); // Highlight statistic for now as per mockup logic
            bottomNavView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }
}
