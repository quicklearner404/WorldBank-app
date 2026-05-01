package com.worldbank.app.activities;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.models.Card;
import com.worldbank.app.models.Contact;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class SendMoneyActivity extends AppCompatActivity {

    private ImageButton ibBack;
    private TextView tvCardNumber, tvCardHolder, tvCardExpiry;
    private TextView tvRecipientName, tvRecipientAccount, tvTransferAmount;
    private TextView btnChange;
    private Button btnSendMoney;
    private EditText etAmount;
    
    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private String cardId = "";
    private String accountId = "";

    private String selectedRecipientName = "";
    private String selectedRecipientAccount = "";
    private String selectedRecipientBank = "";
    private String selectedRecipientUid = "";
    private String selectedRecipientAccountId = "";
    private String selectedTransferType = Transaction.TRANSFER_IBFT;

    static final String IBAN_PREFIX = "PK36WBNK";
    private boolean isFormatting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        cardId = getIntent().getStringExtra("cardId");
        accountId = getIntent().getStringExtra("accountId");

        repo = new TransactionRepository();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        loadCardData();
        setupClickListeners();
        setupAmountWatcher();
        promptRecipientIfEmpty();
    }

    private void bindViews() {
        ibBack = findViewById(R.id.ibBack);
        tvCardNumber = findViewById(R.id.tvCardNumber);
        tvCardHolder = findViewById(R.id.tvCardHolder);
        tvCardExpiry = findViewById(R.id.tvCardExpiry);
        tvRecipientName = findViewById(R.id.tvRecipientName);
        tvRecipientAccount = findViewById(R.id.tvRecipientAccount);
        tvTransferAmount = findViewById(R.id.tvTransferAmount);
        btnChange = findViewById(R.id.btnChange);
        btnSendMoney = findViewById(R.id.btnSendMoney);
        etAmount = findViewById(R.id.etAmount);
    }

    private void loadCardData() {
        if (cardId == null || cardId.isEmpty()) return;
        repo.getCard(cardId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Card card = doc.toObject(Card.class);
                if (card != null) {
                    tvCardNumber.setText(card.getMaskedNumber());
                    tvCardHolder.setText(card.getHolderName());
                    tvCardExpiry.setText("Exp " + card.getExpiry());
                }
            }
        });
    }

    private void setupClickListeners() {
        ibBack.setOnClickListener(v -> finish());
        btnChange.setOnClickListener(v -> showRecipientPicker());
        btnSendMoney.setOnClickListener(v -> goToReview());
    }

    private void setupAmountWatcher() {
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable s) {
                updateAmountDisplay(s.toString());
            }
        });
    }

    private void updateAmountDisplay(String raw) {
        try {
            double amount = Double.parseDouble(raw.replaceAll("[^0-9.]", ""));
            tvTransferAmount.setText(String.format("Rs. %,.0f", amount));
        } catch (Exception e) {
            tvTransferAmount.setText("Rs. 0");
        }
    }

    private void promptRecipientIfEmpty() {
        if (selectedRecipientAccount.isEmpty()) showRecipientPicker();
    }

    private void showRecipientPicker() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_recipient_picker);
        dialog.getWindow().setLayout(android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.WRAP_CONTENT);

        EditText etAcc = dialog.findViewById(R.id.etRecipientAccount);
        EditText etName = dialog.findViewById(R.id.etRecipientName);
        Button btnConfirm = dialog.findViewById(R.id.btnConfirmRecipient);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseDialog);

        etAcc.setText(IBAN_PREFIX);
        etAcc.setSelection(IBAN_PREFIX.length());

        // ── AUTO-LOOKUP LOGIC ──
        etAcc.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable s) {
                String iban = s.toString().trim();
                if (iban.length() == 24 && iban.startsWith(IBAN_PREFIX)) {
                    etName.setHint("Searching database...");
                    repo.findAccountByIban(iban).addOnSuccessListener(snapshots -> {
                        if (!snapshots.isEmpty()) {
                            String name = snapshots.getDocuments().get(0).getString("accountTitle");
                            selectedRecipientUid = snapshots.getDocuments().get(0).getString("uid");
                            selectedRecipientAccountId = snapshots.getDocuments().get(0).getId();
                            etName.setText(name);
                            Toast.makeText(SendMoneyActivity.this, "World Bank User Found!", Toast.LENGTH_SHORT).show();
                        } else {
                            etName.setHint("Recipient Name (Not found)");
                        }
                    });
                }
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String acc = etAcc.getText().toString().trim();
            if (name.isEmpty() || acc.isEmpty()) return;
            
            selectedRecipientName = name;
            selectedRecipientAccount = acc;
            tvRecipientName.setText(name);
            tvRecipientAccount.setText(acc);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void goToReview() {
        String amtStr = etAmount.getText().toString().trim();
        if (amtStr.isEmpty() || selectedRecipientAccount.isEmpty()) {
            Toast.makeText(this, "Enter amount and recipient", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amtStr);
        double fee = TransactionRepository.getAdminFee(selectedTransferType);

        Intent intent = new Intent(this, ReviewPaymentActivity.class);
        intent.putExtra("recipientName", selectedRecipientName);
        intent.putExtra("recipientAccount", selectedRecipientAccount);
        intent.putExtra("recipientBank", selectedRecipientBank);
        intent.putExtra("recipientUid", selectedRecipientUid);
        intent.putExtra("recipientAccountId", selectedRecipientAccountId);
        intent.putExtra("amount", amount);
        intent.putExtra("fee", fee);
        intent.putExtra("transferType", selectedTransferType);
        intent.putExtra("cardId", cardId);
        intent.putExtra("accountId", accountId);
        startActivity(intent);
    }

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return "dev_user_001";
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }
}
