package com.worldbank.app.activities.cards;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.HashMap;
import java.util.Map;

public class AddCardActivity extends AppCompatActivity {

    private EditText etCardNumber, etExpiry, etCvv, etHolderName;
    private TextView tvPreviewNumber, tvPreviewHolder;
    private Button btnAddCard;
    private ImageButton ibBack;

    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_card);

        repo = new TransactionRepository();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupWatchers();
        setupClickListeners();
    }

    private void bindViews() {
        etCardNumber = findViewById(R.id.etCardNumber);
        etExpiry = findViewById(R.id.etExpiry);
        etCvv = findViewById(R.id.etCvv);
        etHolderName = findViewById(R.id.etHolderName);
        tvPreviewNumber = findViewById(R.id.tvPreviewNumber);
        tvPreviewHolder = findViewById(R.id.tvPreviewHolder);
        btnAddCard = findViewById(R.id.btnAddCard);
        ibBack = findViewById(R.id.ibBack);
    }

    private void setupWatchers() {
        // 1. Auto-format Card Number (1234 5678 9012 3456)
        etCardNumber.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                // Remove everything except numbers
                String digits = s.toString().replaceAll("[^0-9]", "");

                // Add spaces every 4 digits
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i > 0 && i % 4 == 0) {
                        formatted.append(" ");
                    }
                    formatted.append(digits.charAt(i));
                }

                etCardNumber.setText(formatted.toString());
                etCardNumber.setSelection(formatted.length()); // Move cursor to end
                tvPreviewNumber.setText(formatted.length() == 0 ? "**** **** **** ****" : formatted.toString());

                isFormatting = false;
                // EARLY ERROR CHECK: Clear error if they reach 16 digits
                String digitss = s.toString().replaceAll("[^0-9]", "");
                if (digitss.length() == 16) {
                    etCardNumber.setError(null);
                }
            }
        });

        // 2. Auto-format Expiry (MM/YY)
        etExpiry.addTextChangedListener(new TextWatcher() {
            private boolean isFormatting = false;

            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;
                isFormatting = true;

                String digits = s.toString().replaceAll("[^0-9]", "");

                // Add slash after 2nd digit
                StringBuilder formatted = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i == 2) {
                        formatted.append("/");
                    }
                    formatted.append(digits.charAt(i));
                }

                etExpiry.setText(formatted.toString());
                etExpiry.setSelection(formatted.length());

                isFormatting = false;
                // EARLY ERROR CHECK: Validate month logic as they type
                String digitss = s.toString().replaceAll("[^0-9]", "");
                if (digits.length() >= 2) {
                    int month = Integer.parseInt(digitss.substring(0, 2));
                    if (month > 12 || month < 1) {
                        etExpiry.setError("Invalid Month (01-12)");
                    } else {
                        etExpiry.setError(null);
                    }
                }
            }
        });

        // 3. Holder Name Auto-Caps Preview
        etHolderName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tvPreviewHolder.setText(s.toString().isEmpty() ? "CARD HOLDER" : s.toString().toUpperCase());
            }
        });
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());

        btnAddCard.setOnClickListener(v -> {
            String number = etCardNumber.getText().toString().replaceAll("\\s+", "");
            String expiry = etExpiry.getText().toString().trim();
            String cvv = etCvv.getText().toString().trim();
            String name = etHolderName.getText().toString().trim();

            // 1. Precise Card Number Error
            if (number.length() < 16) {
                etCardNumber.setError("Card number must be 16 digits");
                etCardNumber.requestFocus();
                return;
            }

            // 2. Precise Expiry Error
            if (expiry.length() < 5) {
                etExpiry.setError("Complete the expiry date (MM/YY)");
                etExpiry.requestFocus();
                return;
            }

            try {
                String[] parts = expiry.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);

                if (month < 1 || month > 12) {
                    etExpiry.setError("Month must be 01-12");
                    etExpiry.requestFocus();
                    return;
                }

                // Block years in the past (e.g., 2023 and below)
                if (year < 24) {
                    etExpiry.setError("This card has expired");
                    etExpiry.requestFocus();
                    return;
                }
            } catch (Exception e) {
                etExpiry.setError("Invalid format");
                etExpiry.requestFocus();
                return;
            }

            // 3. Precise CVV Error
            if (cvv.length() < 3) {
                etCvv.setError("CVV must be 3 or 4 digits");
                etCvv.requestFocus();
                return;
            }

            // 4. Precise Name Error
            if (name.isEmpty()) {
                etHolderName.setError("Enter the name printed on the card");
                etHolderName.requestFocus();
                return;
            }

            // Success!
            saveCardToFirestore(number, expiry, name);
        });
    }

    private void saveCardToFirestore(String number, String expiry, String name) {
        btnAddCard.setEnabled(false);
        btnAddCard.setText("Linking Card...");

        String uid = getCurrentUserId();
        // Mask: show only last 4 digits
        String masked = "**** **** **** " + number.substring(number.length() - 4);
        
        // Match the model exactly: cardId, uid, cardNumber, maskedNumber, holderName, expiry, cardType, balance, monthlyLimit, monthlyUsed
        String type = number.startsWith("4") ? "VISA" : "MASTERCARD";

        Map<String, Object> cardMap = new HashMap<>();
        cardMap.put("uid", uid);
        cardMap.put("cardNumber", number); // Friend's field
        cardMap.put("maskedNumber", masked); // Friend's field
        cardMap.put("holderName", name);
        cardMap.put("expiry", expiry);
        cardMap.put("cardType", type);
        cardMap.put("balance", 0.0); // External cards start with 0 in our app context
        cardMap.put("monthlyLimit", 50000.0); // Default limit for external linked cards
        cardMap.put("monthlyUsed", 0.0);

        repo.addCard(cardMap).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "External Card Linked Successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }).addOnFailureListener(e -> {
            btnAddCard.setEnabled(true);
            btnAddCard.setText("Save Card");
            Toast.makeText(this, "Link Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return sessionManager.getUserId();
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }
}
