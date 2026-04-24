package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import com.worldbank.app.models.Card;
import com.worldbank.app.utils.SessionManager;

public class SendMoneyActivity extends AppCompatActivity {

    private TextView tvCardNumber, tvCardHolder, tvCardExpiry;
    private TextView tvRecipientName, tvRecipientAccount, tvTransferAmount;
    private Button btnSendMoney;
    private TextView btnChange;
    private ImageButton ibBack;

    private FirebaseFirestore db;
    private String cardId;

    // Hardcoded demo recipient — replace with a recipient picker screen later
    private static final String DEMO_RECIPIENT_NAME    = "William Jameson";
    private static final String DEMO_RECIPIENT_ACCOUNT = "**** **** 9809";
    private static final double DEMO_AMOUNT            = 900.00;
    private static final double ADMIN_FEE              = 1.00;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_money);

        cardId = getIntent().getStringExtra("cardId");
        db = FirebaseFirestore.getInstance();

        bindViews();
        loadCardData();
        setupClickListeners();
        showDemoRecipient();
    }

    private void bindViews() {
        ibBack              = findViewById(R.id.ibBack);
        tvCardNumber        = findViewById(R.id.tvCardNumber);
        tvCardHolder        = findViewById(R.id.tvCardHolder);
        tvCardExpiry        = findViewById(R.id.tvCardExpiry);
        tvRecipientName     = findViewById(R.id.tvRecipientName);
        tvRecipientAccount  = findViewById(R.id.tvRecipientAccount);
        tvTransferAmount    = findViewById(R.id.tvTransferAmount);
        btnSendMoney        = findViewById(R.id.btnSendMoney);
        btnChange           = findViewById(R.id.btnChange);
    }

    private void showDemoRecipient() {
        tvRecipientName.setText(DEMO_RECIPIENT_NAME);
        tvRecipientAccount.setText(DEMO_RECIPIENT_ACCOUNT);
        tvTransferAmount.setText(String.format("$%.2f", DEMO_AMOUNT));
    }

    private void loadCardData() {
        if (cardId == null || cardId.isEmpty()) {
            tvCardNumber.setText("1253  5432  3521  3090");
            tvCardHolder.setText("Soroush Nasrpour");
            tvCardExpiry.setText("Exp 09/24");
            return;
        }
        db.collection("cards").document(cardId).get()
                .addOnSuccessListener(doc -> {
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

        btnChange.setOnClickListener(v ->
                Toast.makeText(this, "Recipient picker — coming soon", Toast.LENGTH_SHORT).show());

        btnSendMoney.setOnClickListener(v -> {
            // Pass totals to TransferSuccessActivity
            Intent intent = new Intent(this, TransferSuccessActivity.class);
            intent.putExtra("amount",    DEMO_AMOUNT);
            intent.putExtra("adminFee",  ADMIN_FEE);
            intent.putExtra("total",     DEMO_AMOUNT + ADMIN_FEE);
            startActivity(intent);
        });
    }
}
