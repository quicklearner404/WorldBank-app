package com.worldbank.app.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;

public class AboutUsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }
    }
}
