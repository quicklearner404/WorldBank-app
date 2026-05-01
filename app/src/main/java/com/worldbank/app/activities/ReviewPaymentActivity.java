package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

/**
 * ReviewPaymentActivity — UPGRADED
 * ────────────────────────────────
 * Handles final confirmation with robust balance checking.
 */
public class ReviewPaymentActivity extends AppCompatActivity {

    private TextView tvReviewInitials, tvReviewName, tvReviewAmount;
    private TextView tvBreakdownAmount, tvBreakdownFee, tvBreakdownTotal;
    private EditText etReviewNote;
    private Button btnSendPaymentFinal;
    private ImageButton ibBack;

    private TransactionRepository repo;
    private SessionManager sessionManager;

    private String recipientName, recipientAccount, recipientBank, recipientUid, recipientAccountId, cardId, accountId, transferType;
    private double amount, fee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_payment);

        repo = new TransactionRepository();
        sessionManager = new SessionManager(this);

        getIntentData();
        bindViews();
        displayData();
        setupClickListeners();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        recipientName = intent.getStringExtra("recipientName");
        recipientAccount = intent.getStringExtra("recipientAccount");
        recipientBank = intent.getStringExtra("recipientBank");
        recipientUid = intent.getStringExtra("recipientUid");
        recipientAccountId = intent.getStringExtra("recipientAccountId");
        cardId = intent.getStringExtra("cardId");
        accountId = intent.getStringExtra("accountId");
        transferType = intent.getStringExtra("transferType");
        amount = intent.getDoubleExtra("amount", 0.0);
        fee = intent.getDoubleExtra("fee", 0.0);
    }

    private void bindViews() {
        tvReviewInitials = findViewById(R.id.tvReviewInitials);
        tvReviewName = findViewById(R.id.tvReviewName);
        tvReviewAmount = findViewById(R.id.tvReviewAmount);
        tvBreakdownAmount = findViewById(R.id.tvBreakdownAmount);
        tvBreakdownFee = findViewById(R.id.tvBreakdownFee);
        tvBreakdownTotal = findViewById(R.id.tvBreakdownTotal);
        etReviewNote = findViewById(R.id.etReviewNote);
        btnSendPaymentFinal = findViewById(R.id.btnSendPaymentFinal);
        ibBack = findViewById(R.id.ibBack);
    }

    private void displayData() {
        tvReviewName.setText(recipientName);
        tvReviewInitials.setText(getInitials(recipientName));
        tvReviewAmount.setText(String.format("Rs. %,.0f", amount));
        
        tvBreakdownAmount.setText(String.format("Rs. %,.2f", amount));
        tvBreakdownFee.setText(String.format("Rs. %,.2f", fee));
        tvBreakdownTotal.setText(String.format("Rs. %,.2f", amount + fee));
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());
        btnSendPaymentFinal.setOnClickListener(v -> validateAndExecute());
    }

    private void validateAndExecute() {
        btnSendPaymentFinal.setEnabled(false);
        btnSendPaymentFinal.setText("Checking Balance...");

        // ── 1. Fetch Latest Balance for Pre-check ─────────────────
        repo.getAccount(accountId).addOnSuccessListener(doc -> {
            Double currentBalance = doc.getDouble("balance");
            double totalNeeded = amount + fee;

            if (currentBalance == null || currentBalance < totalNeeded) {
                btnSendPaymentFinal.setEnabled(true);
                btnSendPaymentFinal.setText("Send Payment");
                Toast.makeText(this, "Insufficient balance. Available: Rs. " + 
                    String.format("%,.0f", currentBalance != null ? currentBalance : 0), 
                    Toast.LENGTH_LONG).show();
            } else {
                executeRealTransfer();
            }
        }).addOnFailureListener(e -> {
            btnSendPaymentFinal.setEnabled(true);
            btnSendPaymentFinal.setText("Send Payment");
            Toast.makeText(this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void executeRealTransfer() {
        btnSendPaymentFinal.setText("Sending PKR...");

        Transaction txn = new Transaction();
        txn.setUid(sessionManager.getUserId());
        txn.setSenderUid(sessionManager.getUserId());
        txn.setRecipientUid(recipientUid);
        txn.setRecipientAccountId(recipientAccountId);
        txn.setRecipientAccount(recipientAccount);
        txn.setRecipientName(recipientName);
        txn.setRecipientBank(recipientBank);
        txn.setAmount(amount);
        txn.setAdminFee(fee);
        txn.setTotalDeducted(amount + fee);
        txn.setType(Transaction.TYPE_DEBIT);
        txn.setCategory(Transaction.CAT_TRANSFER);
        if (Transaction.CAT_BILL.equals(transferType)) {
            txn.setCategory(Transaction.CAT_BILL);
        }
        txn.setTransferType(transferType);
        txn.setDescription(etReviewNote.getText().toString().trim());

        // FIX: Changed executeTransfer to sendMoney to match TransactionRepository.java
        repo.sendMoney(txn, accountId).addOnSuccessListener(ref -> {
            Intent intent = new Intent(this, TransferSuccessActivity.class);
            intent.putExtra("amount", amount);
            intent.putExtra("adminFee", fee);
            intent.putExtra("referenceNumber", ref);
            intent.putExtra("recipientName", recipientName);
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            btnSendPaymentFinal.setEnabled(true);
            btnSendPaymentFinal.setText("Send Payment");
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }
}
