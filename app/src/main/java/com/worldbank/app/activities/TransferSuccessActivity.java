package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;

/**
 * TransferSuccessActivity — RECEIPT SCREEN
 * ─────────────────────────────────────────
 * Modern, light-themed receipt showing transfer details.
 */
public class TransferSuccessActivity extends AppCompatActivity {

    private TextView tvTotalAmount, tvTotalTransferAmount, tvAdminFeeAmount, tvTotalAmountGreen;
    private TextView tvReferenceNumber, tvRecipientName;
    private Button btnShare, btnDone;
    private ImageButton ibBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_success);

        bindViews();
        loadDataFromIntent();
        setupClickListeners();
    }

    private void bindViews() {
        tvTotalAmount         = findViewById(R.id.tvTotalAmount);
        tvTotalTransferAmount = findViewById(R.id.tvTotalTransferAmount);
        tvAdminFeeAmount      = findViewById(R.id.tvAdminFeeAmount);
        tvTotalAmountGreen    = findViewById(R.id.tvTotalAmountGreen);
        tvReferenceNumber     = findViewById(R.id.tvReferenceNumber);
        tvRecipientName       = findViewById(R.id.tvRecipientName);
        btnShare              = findViewById(R.id.btnShare);
        btnDone               = findViewById(R.id.btnDone);
        ibBack                = findViewById(R.id.ibBack);
    }

    private void loadDataFromIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        double amount   = intent.getDoubleExtra("amount", 0.0);
        double adminFee = intent.getDoubleExtra("adminFee", 0.0);
        String ref      = intent.getStringExtra("referenceNumber");
        String name     = intent.getStringExtra("recipientName");

        double total    = amount + adminFee;

        tvTotalAmount.setText(String.format("Rs. %,.0f", total));
        tvTotalAmountGreen.setText(String.format("Rs. %,.0f", total));
        tvTotalTransferAmount.setText(String.format("Rs. %,.0f", amount));
        tvAdminFeeAmount.setText(String.format("Rs. %,.0f", adminFee));
        
        tvReferenceNumber.setText(ref != null ? ref : "N/A");
        tvRecipientName.setText(name != null ? name : "Self Top-Up");
    }

    private void setupClickListeners() {
        if (ibBack != null) ibBack.setOnClickListener(v -> goToHome());
        btnDone.setOnClickListener(v -> goToHome());
        
        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Payment Receipt: " + tvTotalAmount.getText() + " Ref: " + tvReferenceNumber.getText());
            startActivity(Intent.createChooser(shareIntent, "Share Receipt"));
        });
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        goToHome();
    }
}
