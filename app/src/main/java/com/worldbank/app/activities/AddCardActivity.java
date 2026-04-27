package com.worldbank.app.activities;

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
        etCardNumber.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString().replaceAll("\\s+", "");
                if (val.isEmpty()) {
                    tvPreviewNumber.setText("**** **** **** ****");
                } else {
                    // Format for preview: 1234 5678 ...
                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < val.length(); i++) {
                        if (i > 0 && i % 4 == 0) formatted.append("  ");
                        formatted.append(val.charAt(i));
                    }
                    tvPreviewNumber.setText(formatted.toString());
                }
            }
        });

        etHolderName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                tvPreviewHolder.setText(s.toString().isEmpty() ? "Card Holder" : s.toString().toUpperCase());
            }
        });
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());

        btnAddCard.setOnClickListener(v -> {
            String number = etCardNumber.getText().toString().trim().replaceAll("\\s+", "");
            String expiry = etExpiry.getText().toString().trim();
            String cvv = etCvv.getText().toString().trim();
            String name = etHolderName.getText().toString().trim();

            if (number.length() < 16) {
                Toast.makeText(this, "Card number must be 16 digits", Toast.LENGTH_SHORT).show();
                return;
            }
            if (expiry.isEmpty() || !expiry.contains("/")) {
                Toast.makeText(this, "Enter valid expiry (MM/YY)", Toast.LENGTH_SHORT).show();
                return;
            }
            if (cvv.length() < 3) {
                Toast.makeText(this, "Enter valid CVV", Toast.LENGTH_SHORT).show();
                return;
            }
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter holder name", Toast.LENGTH_SHORT).show();
                return;
            }

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
