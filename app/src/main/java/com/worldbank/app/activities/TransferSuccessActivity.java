package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.worldbank.app.R;

/**
 * TransferSuccessActivity shows the receipt after a successful transfer.
 * All amounts display in PKR not USD.
 */
public class TransferSuccessActivity extends AppCompatActivity {

    // receipt display text views
    TextView tvTotalAmount;
    TextView tvTotalTransferAmount;
    TextView tvAdminFeeAmount;
    TextView tvTotalAmountGreen;
    TextView tvRecipientName;
    TextView tvReferenceNumber;

    // action buttons at the bottom
    Button btnShare;
    Button btnDone;

    // back arrow in the top bar
    ImageButton ibBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_success);

        init();
        loadDataFromIntent();
        setupClickListeners();
    }

    // reads all receipt values from the launching intent
    private void loadDataFromIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        double amount   = intent.getDoubleExtra("amount",   0.0);
        double adminFee = intent.getDoubleExtra("adminFee", 0.0);
        double total    = amount + adminFee;

        String recipientName   = intent.getStringExtra("recipientName");
        String referenceNumber = intent.getStringExtra("referenceNumber");

        // format all three amounts as pkr strings
        String formattedTotal    = String.format("Rs. %,.0f", total);
        String formattedAmount   = String.format("Rs. %,.0f", amount);
        String formattedAdminFee = String.format("Rs. %,.0f", adminFee);

        tvTotalAmount.setText(formattedTotal);
        tvTotalAmountGreen.setText(formattedTotal);
        tvTotalTransferAmount.setText(formattedAmount);
        tvAdminFeeAmount.setText(formattedAdminFee);

        // show recipient name if the view exists
        if (tvRecipientName != null && recipientName != null) {
            tvRecipientName.setText(recipientName);
        }

        // show reference number if the view exists
        if (tvReferenceNumber != null && referenceNumber != null) {
            tvReferenceNumber.setText(referenceNumber);
        }
    }

    // wires back button done button and share button
    private void setupClickListeners() {
        // back press always goes home not back in the stack
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goToHome();
            }
        });

        if (ibBack != null) {
            ibBack.setOnClickListener(v ->
                    getOnBackPressedDispatcher().onBackPressed());
        }

        // done button clears the stack and returns to home
        btnDone.setOnClickListener(v -> goToHome());

        // share button sends a plain text receipt via the system sheet
        btnShare.setOnClickListener(v -> {
            String receipt = buildShareText();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, receipt);
            startActivity(Intent.createChooser(shareIntent, "Share Receipt"));
        });
    }

    // clears the activity stack and returns the user to home
    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // builds a short plain text receipt string for sharing
    private String buildShareText() {
        String ref   = tvReferenceNumber  != null ? tvReferenceNumber.getText().toString()  : "";
        String name  = tvRecipientName    != null ? tvRecipientName.getText().toString()    : "";
        String total = tvTotalAmountGreen != null ? tvTotalAmountGreen.getText().toString() : "";

        return "World Bank Transfer Receipt\n"
                + "Recipient: " + name  + "\n"
                + "Amount: "   + total  + "\n"
                + "Ref: "      + ref;
    }

    // finds all views by id
    private void init() {
        tvTotalAmount         = findViewById(R.id.tvTotalAmount);
        tvTotalTransferAmount = findViewById(R.id.tvTotalTransferAmount);
        tvAdminFeeAmount      = findViewById(R.id.tvAdminFeeAmount);
        tvTotalAmountGreen    = findViewById(R.id.tvTotalAmountGreen);
        tvRecipientName       = findViewById(R.id.tvRecipientName);
        tvReferenceNumber     = findViewById(R.id.tvReferenceNumber);
        btnShare              = findViewById(R.id.btnShare);
        btnDone               = findViewById(R.id.btnDone);
        ibBack                = findViewById(R.id.ibBack);
    }
}