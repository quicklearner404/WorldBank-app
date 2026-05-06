package com.worldbank.app.activities.transaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.worldbank.app.R;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import com.worldbank.app.activities.home.HomeActivity;

public class TransferSuccessActivity extends AppCompatActivity {

    private TextView tvTotalAmount, tvRecipientName, tvTransactionDate, tvReferenceNumber, tvAdminFeeAmount;
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
        // Matches the IDs in activity_transfer_success.xml
        tvTotalAmount      = findViewById(R.id.tvTotalAmount);
        tvRecipientName    = findViewById(R.id.tvRecipientName);
        tvTransactionDate  = findViewById(R.id.tvTransactionDate);
        tvReferenceNumber  = findViewById(R.id.tvReferenceNumber);
        tvAdminFeeAmount   = findViewById(R.id.tvAdminFeeAmount);
        btnShare           = findViewById(R.id.btnShare);
        btnDone            = findViewById(R.id.btnDone);
        ibBack             = findViewById(R.id.ibBack);
    }

    private void loadDataFromIntent() {
        Intent intent = getIntent();
        if (intent == null) return;

        double amount   = intent.getDoubleExtra("amount", 0.0);
        double adminFee = intent.getDoubleExtra("adminFee", 0.0);
        String ref      = intent.getStringExtra("referenceNumber");
        String name     = intent.getStringExtra("recipientName");

        double total = amount + adminFee;

        // Display total in PKR format
        tvTotalAmount.setText(String.format(Locale.getDefault(), "Rs. %,.0f", total));
        tvRecipientName.setText(name != null ? name : "Self Top-Up");
        tvReferenceNumber.setText(ref != null ? ref : "N/A");
        tvAdminFeeAmount.setText(String.format(Locale.getDefault(), "Rs. %,.2f", adminFee));

        // Display current transaction date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvTransactionDate.setText(sdf.format(new Date()));
    }

    private void setupClickListeners() {
        if (ibBack != null) ibBack.setOnClickListener(v -> goToHome());
        btnDone.setOnClickListener(v -> goToHome());

        btnShare.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "World Bank Transaction Receipt\n" +
                    "Recipient: " + tvRecipientName.getText() + "\n" +
                    "Total Amount: " + tvTotalAmount.getText() + "\n" +
                    "Reference: " + tvReferenceNumber.getText());
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
        super.onBackPressed();
        goToHome();
    }
}
