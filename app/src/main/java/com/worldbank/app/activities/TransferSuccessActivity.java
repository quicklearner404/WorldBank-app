package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;

/**
 * TransferSuccessActivity
 * ────────────────────────
 * Screen shown after a successful transfer or top-up.
 * Displays total amount, breakdown, and confirmation.
 */
public class TransferSuccessActivity extends AppCompatActivity {

    private TextView tvTotalAmount, tvTotalTransferAmount, tvAdminFeeAmount, tvTotalAmountGreen;
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
        btnShare              = findViewById(R.id.btnShare);
        btnDone               = findViewById(R.id.btnDone);
        ibBack                = findViewById(R.id.ibBack);
    }

    private void loadDataFromIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        double amount   = intent.getDoubleExtra("amount", 0.0);
        double adminFee = intent.getDoubleExtra("adminFee", 0.0);
        double total    = amount + adminFee;

        String formattedTotal    = String.format("$%,.2f", total);
        String formattedAmount   = String.format("$%,.2f", amount);
        String formattedAdminFee = String.format("$%,.2f", adminFee);

        tvTotalAmount.setText(formattedTotal);
        tvTotalAmountGreen.setText(formattedTotal);
        tvTotalTransferAmount.setText(formattedAmount);
        tvAdminFeeAmount.setText(formattedAdminFee);
    }

    private void setupClickListeners() {
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> goToHome());
        }

        btnDone.setOnClickListener(v -> goToHome());

        btnShare.setOnClickListener(v -> {
            // Placeholder share logic
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Transfer successful: " + tvTotalAmount.getText());
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
