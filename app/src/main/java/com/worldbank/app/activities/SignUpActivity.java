package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignUpActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseFirestore db;
    TextInputEditText etEmail, etPassword, etConfirmPassword;
    Button btnSignup;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        init();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        btnSignup.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String cpass = etConfirmPassword.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty() || cpass.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Some fields are empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(cpass)) {
                Toast.makeText(SignUpActivity.this, "Password mismatch", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            authResult.getUser().sendEmailVerification();
                            String uid = authResult.getUser().getUid();
                            String name = email.split("@")[0];
                            createNewUserData(uid, name, email);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(SignUpActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    void createNewUserData(String uid, String name, String email) {
        // create the user profile document first then chain account and card creation
        Map<String, Object> user = new HashMap<>();
        user.put("uid", uid);
        user.put("displayName", name);
        user.put("email", email);
        user.put("phone", "");
        user.put("city", "");
        user.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        createAccountData(uid, name);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignUpActivity.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void createAccountData(String uid, String name) {
        // create the savings account linked to this user
        Map<String, Object> account = new HashMap<>();
        account.put("uid", uid);
        account.put("accountNumber", generateIBAN());
        account.put("accountTitle", name);
        account.put("bankName", "World Bank");
        account.put("accountType", "SAVINGS");
        account.put("balance", 0);
        account.put("currency", "PKR");
        account.put("isActive", true);

        db.collection("accounts").add(account)
                .addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.DocumentReference ref) {
                        String accountId = ref.getId();
                        createCardData(uid, accountId, name);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignUpActivity.this, "Failed to create account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void createCardData(String uid, String accountId, String name) {
        // create the debit card document linked to the account above
        Map<String, Object> card = new HashMap<>();
        card.put("uid", uid);
        card.put("accountId", accountId);
        card.put("maskedNumber", "4532 **** **** 3090");
        card.put("holderName", name);
        card.put("expiry", "12/28");
        card.put("cardType", "VISA");
        card.put("isActive", true);
        card.put("monthlyLimit", 200000);
        card.put("monthlyUsed", 0);

        db.collection("cards").add(card)
                .addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.DocumentReference unused) {
                        Toast.makeText(SignUpActivity.this, "Account Created!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignUpActivity.this, "Failed to create card: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    String generateIBAN() {
        // generates a random account number in the local IBAN format
        Random random = new Random();
        long number = (long) (random.nextDouble() * 9_000_000_000_000L) + 1_000_000_000_000L;
        return "PK36WBNK" + number;
    }

    private void init() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignup = findViewById(R.id.btn_signup);
        toolbar = findViewById(R.id.toolbar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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