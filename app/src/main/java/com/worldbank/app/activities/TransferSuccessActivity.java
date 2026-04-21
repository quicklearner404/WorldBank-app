package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;

public class TransferSuccessActivity extends AppCompatActivity {

    private TextView tvTotalAmount, tvTotalTransferAmount, tvAdminFeeAmount, tvTotalAmountGreen;
    private Button btnDone, btnShare;
    private ImageButton ibBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_success);

        bindViews();
        populateData();
        setupClickListeners();
    }

    private void bindViews() {
        ibBack                = findViewById(R.id.ibBack);
        tvTotalAmount         = findViewById(R.id.tvTotalAmount);
        tvTotalTransferAmount = findViewById(R.id.tvTotalTransferAmount);
        tvAdminFeeAmount      = findViewById(R.id.tvAdminFeeAmount);
        tvTotalAmountGreen    = findViewById(R.id.tvTotalAmountGreen);
        btnDone               = findViewById(R.id.btnDone);
        btnShare              = findViewById(R.id.btnShare);
    }

    private void populateData() {
        double amount   = getIntent().getDoubleExtra("amount",   900.00);
        double adminFee = getIntent().getDoubleExtra("adminFee", 1.00);
        double total    = getIntent().getDoubleExtra("total",    901.00);

        tvTotalAmount.setText(String.format("$%.2f", total));
        tvTotalTransferAmount.setText(String.format("$%.2f", amount));
        tvAdminFeeAmount.setText(String.format("$%.2f", adminFee));
        tvTotalAmountGreen.setText(String.format("$%.2f", total));
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());

        btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT,
                    "I just sent " + tvTotalTransferAmount.getText() + " via World Bank!");
            startActivity(Intent.createChooser(share, "Share receipt via"));
        });

        btnDone.setOnClickListener(v -> {
            // Go all the way back to Home, clear the back stack
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }
}
