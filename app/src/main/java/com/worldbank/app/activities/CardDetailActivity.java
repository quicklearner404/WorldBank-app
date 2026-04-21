package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import com.worldbank.app.models.Card;
import com.worldbank.app.utils.SessionManager;

/**
 * CardDetailActivity
 * ───────────────────
 * Owner  : Dev 2
 * Mockup : Page 9
 *
 * Receives "cardId" String from HomeActivity via Intent.
 * Loads card data from Firestore and displays:
 *  - Card widget (same as Home)
 *  - Monthly transfer limit with circular progress
 *  - 4 menu items
 *  - Add Card button
 */
public class CardDetailActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────
    private ImageButton ibBack;
    private TextView tvCardNumber;
    private TextView tvCardHolder;
    private TextView tvCardExpiry;
    private ProgressBar pbMonthlyLimit;
    private TextView tvLimitPercent;
    private TextView tvLimitAmount;
    private LinearLayout llConnectPayment;
    private LinearLayout llTransferHistory;
    private LinearLayout llChangeUsername;
    private LinearLayout llTransactionReport;
    private Button btnAddCard;

    // ── Firebase ───────────────────────────────────────────────────
    private FirebaseFirestore db;

    // ── Data ───────────────────────────────────────────────────────
    private String cardId;

    // ──────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_detail);

        // Get cardId passed from HomeActivity
        cardId = getIntent().getStringExtra("cardId");

        db = FirebaseFirestore.getInstance();

        bindViews();
        setupClickListeners();
        loadCardData();
    }

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════

    private void bindViews() {
        ibBack               = findViewById(R.id.ibBack);
        tvCardNumber         = findViewById(R.id.tvCardNumber);
        tvCardHolder         = findViewById(R.id.tvCardHolder);
        tvCardExpiry         = findViewById(R.id.tvCardExpiry);
        pbMonthlyLimit       = findViewById(R.id.pbMonthlyLimit);
        tvLimitPercent       = findViewById(R.id.tvLimitPercent);
        tvLimitAmount        = findViewById(R.id.tvLimitAmount);
        llConnectPayment     = findViewById(R.id.llConnectPayment);
        llTransferHistory    = findViewById(R.id.llTransferHistory);
        llChangeUsername     = findViewById(R.id.llChangeUsername);
        llTransactionReport  = findViewById(R.id.llTransactionReport);
        btnAddCard           = findViewById(R.id.btnAddCard);
    }

    private void setupClickListeners() {
        // Back button
        ibBack.setOnClickListener(v -> finish());

        // Menu items
        llConnectPayment.setOnClickListener(v ->
                Toast.makeText(this, "Connect to Payment Service", Toast.LENGTH_SHORT).show()
        );

        llTransferHistory.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class))
        );

        llChangeUsername.setOnClickListener(v ->
                Toast.makeText(this, "Change Card Username", Toast.LENGTH_SHORT).show()
        );

        llTransactionReport.setOnClickListener(v ->
                Toast.makeText(this, "Transaction Report", Toast.LENGTH_SHORT).show()
        );

        // Add Card
        btnAddCard.setOnClickListener(v ->
                Toast.makeText(this, "Add Card — coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    // ══════════════════════════════════════════════════════════════
    //  FIRESTORE
    // ══════════════════════════════════════════════════════════════

    private void loadCardData() {
        // If no cardId was passed (e.g. launched standalone in dev), bail out gracefully
        if (cardId == null || cardId.isEmpty()) {
            showDevPlaceholder();
            return;
        }

        db.collection("cards")
                .document(cardId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Card card = documentSnapshot.toObject(Card.class);
                        if (card != null) {
                            card.setCardId(documentSnapshot.getId());
                            displayCard(card);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load card", Toast.LENGTH_SHORT).show()
                );
    }

    private void displayCard(Card card) {
        // Card widget
        tvCardNumber.setText(card.getMaskedNumber());
        tvCardHolder.setText(card.getHolderName());
        tvCardExpiry.setText("Exp " + card.getExpiry());

        // Circular progress — monthly limit
        int percent = card.getLimitPercentage();
        pbMonthlyLimit.setProgress(percent);
        tvLimitPercent.setText(percent + "%");

        // Limit text e.g. "$6,080 out of 8,000"
        String limitText = String.format("$%,.0f out of %,.0f",
                card.getMonthlyUsed(), card.getMonthlyLimit());
        tvLimitAmount.setText(limitText);
    }

    /**
     * Called when no cardId is passed — fills in placeholder text
     * so the screen still renders during development.
     */
    private void showDevPlaceholder() {
        tvCardNumber.setText("1253  5432  3521  3090");
        tvCardHolder.setText("Soroush Nasrpour");
        tvCardExpiry.setText("Exp 09/24");
        pbMonthlyLimit.setProgress(61);
        tvLimitPercent.setText("61%");
        tvLimitAmount.setText("$6,080 out of 8,000");
    }
}
