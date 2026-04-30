package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;

public class LoginActivity extends AppCompatActivity {

    FirebaseAuth auth;
    TextInputEditText etEmail, etPassword;
    Button btnLogin;
    TextView tvForgotPassword, tvRegister;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        btnLogin.setOnClickListener((v) -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email is required");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Password is required");
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            // save session so splash skips login on next app open
                            String uid = authResult.getUser().getUid();
                            String userEmail = authResult.getUser().getEmail();
                            String userName = authResult.getUser().getDisplayName() != null
                                    ? authResult.getUser().getDisplayName()
                                    : "";

                            SessionManager session = new SessionManager(LoginActivity.this);
                            session.saveSession(uid, userEmail, userName, true);

                            Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvForgotPassword.setOnClickListener((v) -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        tvRegister.setOnClickListener((v) -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });
    }

    void init() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegister = findViewById(R.id.tv_register);
        toolbar = findViewById(R.id.toolbar);
        auth = FirebaseAuth.getInstance();
    }
}