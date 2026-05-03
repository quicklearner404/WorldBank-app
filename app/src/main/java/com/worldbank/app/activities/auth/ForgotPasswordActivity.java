package com.worldbank.app.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.worldbank.app.R;

public class ForgotPasswordActivity extends AppCompatActivity {

    FirebaseAuth auth;
    TextInputEditText etEmail;
    Button btnConfirm;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        init();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        btnConfirm.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Please enter your email");
                etEmail.requestFocus();
                return;
            }

            btnConfirm.setEnabled(false);
            btnConfirm.setText("Sending...");

            auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this,
                                "Reset link sent, check your inbox",
                                Toast.LENGTH_LONG).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("Confirm");
                        Toast.makeText(this,
                                e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void init() {
        etEmail  = findViewById(R.id.et_email);
        btnConfirm = findViewById(R.id.btn_confirm);
        toolbar  = findViewById(R.id.toolbar);
        auth     = FirebaseAuth.getInstance();
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}