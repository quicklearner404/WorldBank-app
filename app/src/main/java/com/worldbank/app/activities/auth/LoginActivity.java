package com.worldbank.app.activities.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.google.firebase.auth.FirebaseUser;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.activities.home.HomeActivity;
public class LoginActivity extends AppCompatActivity {

    FirebaseAuth auth;
    TextInputEditText etEmail, etPassword;
    Button btnLogin;
    CheckBox cbRememberMe;
    TextView tvForgotPassword, tvRegister;
    Toolbar toolbar;
    SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();

        tvRegister.setText("Don't have an account? Register");

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Login");
        }

        // prefill email if user had remember me on during last login
        String rememberedEmail = session.getRememberedEmail();
        if (!rememberedEmail.isEmpty()) {
            etEmail.setText(rememberedEmail);
            cbRememberMe.setChecked(true);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() { goToWelcome(); }
        });

        toolbar.setNavigationOnClickListener(v -> goToWelcome());

        btnLogin.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) { etEmail.setError("Email is required"); return; }
            if (password.isEmpty()) { etPassword.setError("Password is required"); return; }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            FirebaseUser user = authResult.getUser();

                            // block unverified users from entering the app
                            if (!user.isEmailVerified()) {
                                auth.signOut();
                                user.sendEmailVerification();
                                Toast.makeText(LoginActivity.this,
                                        "Please verify your email first. A new link has been sent.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            String uid       = user.getUid();
                            String userEmail = user.getEmail();
                            String userName  = user.getDisplayName() != null
                                    ? user.getDisplayName() : "";

                            // save or clear remembered email based on checkbox
                            if (cbRememberMe.isChecked()) {
                                session.saveRememberedEmail(userEmail);
                            } else {
                                session.clearRememberedEmail();
                            }

                            // always save active session so home screen works
                            session.saveSession(uid, userEmail, userName, cbRememberMe.isChecked());

                            Toast.makeText(LoginActivity.this, "Login successful",
                                    Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(LoginActivity.this, e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class)));

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
    }

    private void goToWelcome() {
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    void init() {
        etEmail          = findViewById(R.id.et_email);
        etPassword       = findViewById(R.id.et_password);
        btnLogin         = findViewById(R.id.btn_login);
        cbRememberMe     = findViewById(R.id.cb_remember_me);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvRegister       = findViewById(R.id.tv_register);
        toolbar          = findViewById(R.id.toolbar);
        auth             = FirebaseAuth.getInstance();
        session          = new SessionManager(this);
    }
}