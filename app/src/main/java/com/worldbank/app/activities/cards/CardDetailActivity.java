package com.worldbank.app.activities.cards;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

import com.worldbank.app.activities.transaction.TransactionHistoryActivity;
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
        ibBack.setOnClickListener(v -> finish());

        llConnectPayment.setOnClickListener(v ->
                Toast.makeText(this, "Connect to Payment Service", Toast.LENGTH_SHORT).show()
        );

        llTransferHistory.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class))
        );

        llChangeUsername.setOnClickListener(v ->
                Toast.makeText(this, "Feature coming soon", Toast.LENGTH_SHORT).show()
        );

        llTransactionReport.setOnClickListener(v ->
                Toast.makeText(this, "Report generated", Toast.LENGTH_SHORT).show()
        );


        btnAddCard.setOnClickListener(v ->
                startActivity(new Intent(this, AddCardActivity.class))
        );
    }

    private void displayCard(Card card) {
        // 1. Identify if it's Internal or External
        boolean isInternal = card.getAccountId() != null && !card.getAccountId().isEmpty();

        // 2. Set Card Data
        tvCardNumber.setText(card.getMaskedNumber());
        tvCardHolder.setText(card.getHolderName().toUpperCase());

        // 3. UI logic: If internal, show "Primary"; if external, show Expiry
        tvCardExpiry.setText(isInternal ? "Primary Account" : "Exp " + card.getExpiry());

        // 4. Update Theme: Purple for World Bank cards, Gray for others
        androidx.cardview.widget.CardView cvCard = findViewById(R.id.cvCard);
        if (isInternal) {
            cvCard.setCardBackgroundColor(getColor(R.color.purple_primary));
        } else {
            cvCard.setCardBackgroundColor(getColor(R.color.gray_text));
        }

        // 5. Smart Limit Section
        // Find the CardView containing the progress bar to hide/show it
        View limitCard = (View) pbMonthlyLimit.getParent().getParent();

        if (isInternal) {
            limitCard.setVisibility(View.VISIBLE);

            int percent = card.getLimitPercentage();
            pbMonthlyLimit.setProgress(percent);
            tvLimitPercent.setText(percent + "%");


            String limitText = String.format(java.util.Locale.getDefault(), "Rs. %,.0f out of %,.0f",
                    card.getMonthlyUsed(), card.getMonthlyLimit());
            tvLimitAmount.setText(limitText);
        } else {
            // Hide the limit section for external cards (HBL, etc.)
            limitCard.setVisibility(View.GONE);
        }
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




    private void showDevPlaceholder() {
        tvCardNumber.setText("1253  5432  3521  3090");
        tvCardHolder.setText("Soroush Nasrpour");
        tvCardExpiry.setText("Exp 09/24");
        pbMonthlyLimit.setProgress(61);
        tvLimitPercent.setText("61%");
        tvLimitAmount.setText("$6,080 out of 8,000");
    }
}
