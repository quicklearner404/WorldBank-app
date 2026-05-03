package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

public class ConfirmTopUpActivity extends AppCompatActivity {

    private TextView tvConfirmAmount, tvConfirmMethod, tvConfirmTotal;
    private Button btnConfirmTopUp, btnCancel;
    private ImageButton ibBack;

    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private String accountId;
    private double amount;
    private String methodName;
    private String cardId; // ✅ Added this!

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm_top_up);

        repo = new TransactionRepository();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        getIntentData();
        bindViews();
        displayData();
        setupClickListeners();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        accountId = intent.getStringExtra("accountId");
        amount = intent.getDoubleExtra("amount", 0.0);
        methodName = intent.getStringExtra("methodName");
        cardId = getIntent().getStringExtra("cardId");
    }

    private void bindViews() {
        tvConfirmAmount = findViewById(R.id.tvConfirmAmount);
        tvConfirmMethod = findViewById(R.id.tvConfirmMethod);
        tvConfirmTotal = findViewById(R.id.tvConfirmTotal);
        btnConfirmTopUp = findViewById(R.id.btnConfirmTopUp);
        btnCancel = findViewById(R.id.btnCancel);
        ibBack = findViewById(R.id.ibBack);
    }

    private void displayData() {
        String formattedAmount = String.format("Rs. %,.2f", amount);
        tvConfirmAmount.setText(formattedAmount);
        tvConfirmMethod.setText(methodName != null ? methodName : "Linked Card");
        tvConfirmTotal.setText(formattedAmount);
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());

        btnConfirmTopUp.setOnClickListener(v -> {
            btnConfirmTopUp.setEnabled(false);
            btnConfirmTopUp.setText("Processing...");

            String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();

            // Change this line:
            repo.topUp(uid, accountId, cardId, amount)
                .addOnSuccessListener(ref -> {
                    Intent intent = new Intent(this, TransferSuccessActivity.class);
                    intent.putExtra("amount", amount);
                    intent.putExtra("adminFee", 0.0);
                    intent.putExtra("referenceNumber", ref);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmTopUp.setEnabled(true);
                    btnConfirmTopUp.setText("Confirm and Top Up");
                    Toast.makeText(this, "Top-up failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
        });
    }
}
