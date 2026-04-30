package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import com.worldbank.app.models.Card;
import com.worldbank.app.models.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignUpActivity extends AppCompatActivity {

    FirebaseAuth auth;
    FirebaseFirestore db;
    TextInputEditText etName, etCnic, etPhone, etEmail, etPassword, etConfirmPassword;
    Button btnSignup;
    Toolbar toolbar;

    // tracks whether the cnic watcher is already running to avoid infinite loop
    boolean isFormattingCnic = false;

    // tracks whether the phone watcher is already running to avoid infinite loop
    boolean isFormattingPhone = false;

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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        toolbar.setNavigationOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        // auto format cnic as user types so it looks like 35201-1234567-1
        etCnic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingCnic) return;
                isFormattingCnic = true;

                String digits = s.toString().replaceAll("[^0-9]", "");

                if (digits.length() > 13) {
                    digits = digits.substring(0, 13);
                }

                String formatted = digits;
                if (digits.length() > 5) {
                    formatted = digits.substring(0, 5) + "-" + digits.substring(5);
                }
                if (digits.length() > 12) {
                    formatted = digits.substring(0, 5) + "-" + digits.substring(5, 12) + "-" + digits.substring(12);
                }

                etCnic.setText(formatted);
                etCnic.setSelection(formatted.length());
                isFormattingCnic = false;
            }
        });

        // lock the phone field to always start with +92 so user cant delete the prefix
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingPhone) return;
                isFormattingPhone = true;

                String current = s.toString();

                if (!current.startsWith("+92 ")) {
                    String userDigits = current.replaceAll("[^0-9]", "");

                    if (userDigits.startsWith("92")) {
                        userDigits = userDigits.substring(2);
                    }

                    if (userDigits.length() > 10) {
                        userDigits = userDigits.substring(0, 10);
                    }

                    String formatted = "+92 " + userDigits;
                    etPhone.setText(formatted);
                    etPhone.setSelection(formatted.length());
                } else {
                    String afterPrefix = current.substring(4);
                    String digitsOnly = afterPrefix.replaceAll("[^0-9]", "");

                    if (digitsOnly.length() > 10) {
                        digitsOnly = digitsOnly.substring(0, 10);
                        String formatted = "+92 " + digitsOnly;
                        etPhone.setText(formatted);
                        etPhone.setSelection(formatted.length());
                    }
                }

                isFormattingPhone = false;
            }
        });

        // set the phone field to already show +92 when user taps into it
        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etPhone.getText().toString().isEmpty()) {
                etPhone.setText("+92 ");
                etPhone.setSelection(etPhone.getText().length());
            }
        });

        btnSignup.setOnClickListener((v) -> {
            String name = etName.getText().toString().trim();
            String cnic = etCnic.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            String cpass = etConfirmPassword.getText().toString().trim();

            if (name.isEmpty() || cnic.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty() || cpass.isEmpty()) {
                Toast.makeText(SignUpActivity.this, getString(R.string.error_fields_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!name.contains(" ") || name.split(" ").length < 2) {
                Toast.makeText(SignUpActivity.this, "Please enter your full name", Toast.LENGTH_SHORT).show();
                return;
            }

            String cnicDigits = cnic.replaceAll("[^0-9]", "");
            if (cnicDigits.length() != 13) {
                Toast.makeText(SignUpActivity.this, getString(R.string.error_cnic_invalid), Toast.LENGTH_SHORT).show();
                return;
            }

            String phoneDigits = phone.replaceAll("[^0-9]", "");
            if (phoneDigits.length() != 12) {
                Toast.makeText(SignUpActivity.this, "Enter a valid Pakistani phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(SignUpActivity.this, getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.length() < 6) {
                Toast.makeText(SignUpActivity.this, getString(R.string.error_password_short), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(cpass)) {
                Toast.makeText(SignUpActivity.this, getString(R.string.error_passwords_dont_match), Toast.LENGTH_SHORT).show();
                return;
            }

            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            authResult.getUser().sendEmailVerification();
                            String uid = authResult.getUser().getUid();
                            createNewUserData(uid, name, cnic, phone, email);
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

    void createNewUserData(String uid, String name, String cnic, String phone, String email) {
        // build a User object so firestore can deserialize it back into the model later
        User user = new User(uid, email, name, cnic, phone);

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
                        Toast.makeText(SignUpActivity.this, getString(R.string.error_user_save_failed) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void createAccountData(String uid, String name) {
        // account still uses a map because there is no Account model yet
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
                        createCardData(uid, ref.getId(), name);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignUpActivity.this, getString(R.string.error_account_create_failed) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    void createCardData(String uid, String accountId, String name) {
        // build a Card object so firestore can deserialize it back into the model later
        Card card = new Card(uid, accountId, "4532 **** **** 3090", name, "12/28", "VISA", 200000);

        db.collection("cards").add(card)
                .addOnSuccessListener(new OnSuccessListener<com.google.firebase.firestore.DocumentReference>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.DocumentReference unused) {
                        Toast.makeText(SignUpActivity.this, getString(R.string.success_account_created), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignUpActivity.this, getString(R.string.error_card_create_failed) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    String generateIBAN() {
        // random 13 digit number appended to the bank prefix to make a unique account number
        Random random = new Random();
        long number = (long) (random.nextDouble() * 9_000_000_000_000L) + 1_000_000_000_000L;
        return "PK36WBNK" + number;
    }

    void init() {
        etName = findViewById(R.id.et_name);
        etCnic = findViewById(R.id.et_cnic);
        etPhone = findViewById(R.id.et_phone);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnSignup = findViewById(R.id.btn_signup);
        toolbar = findViewById(R.id.toolbar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }
}