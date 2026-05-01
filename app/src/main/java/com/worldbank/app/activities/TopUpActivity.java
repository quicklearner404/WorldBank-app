package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.CardAdapter;
import com.worldbank.app.models.Card;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * TopUpActivity — PKR UPDATED & FIXED
 * ──────────────────────────
 * Functional PKR top-up with Rs. 100, Rs. 500, Rs. 2000 quick options.
 */
public class TopUpActivity extends AppCompatActivity implements CardAdapter.OnCardClickListener {

    private RecyclerView rvCards;
    private CardAdapter cardAdapter;
    private List<Card> cardList = new ArrayList<>();
    private EditText etOtherAmount;
    private Button btnTopUpNow;
    private Button btn100, btn500, btn2000;

    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private String currentAccountId = "";
    private Card selectedFundingCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_top_up);

        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        repo = new TransactionRepository();

        bindViews();
        setupRecyclerView();
        setupAmountButtons();
        loadInitialData();
    }

    private void bindViews() {
        rvCards = findViewById(R.id.rvCards);
        etOtherAmount = findViewById(R.id.etOtherAmount);
        btnTopUpNow = findViewById(R.id.btnTopUpNow);
        
        btn100 = findViewById(R.id.btn100);
        btn500 = findViewById(R.id.btn500);
        btn2000 = findViewById(R.id.btn2000);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        btnTopUpNow.setOnClickListener(v -> performTopUp());
    }

    private void setupRecyclerView() {
        // Initializing with 'this' listener to handle card selection
        cardAdapter = new CardAdapter(this, cardList, this);
        rvCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCards.setAdapter(cardAdapter);
    }

    @Override
    public void onCardClick(Card card, int position) {
        selectedFundingCard = card;
        Toast.makeText(this, "Source Selected: " + card.getMaskedNumber(), Toast.LENGTH_SHORT).show();
    }

    private void setupAmountButtons() {
        View.OnClickListener amountListener = v -> {
            Button b = (Button) v;
            // Get value from "Rs. 500" -> "500"
            String val = b.getText().toString().replaceAll("[^0-9]", "");
            etOtherAmount.setText(val);
        };

        if (btn100 != null) btn100.setOnClickListener(amountListener);
        if (btn500 != null) btn500.setOnClickListener(amountListener);
        if (btn2000 != null) btn2000.setOnClickListener(amountListener);
    }

    private void loadInitialData() {
        String uid = getCurrentUserId();
        if (uid.isEmpty()) return;

        repo.getAccountQuery(uid).get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) {
                currentAccountId = snapshots.getDocuments().get(0).getId();
            }
        });

        repo.getCardsQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            cardList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Card card = doc.toObject(Card.class);
                card.setCardId(doc.getId());
                cardList.add(card);
            }
            cardAdapter.notifyDataSetChanged();
            
            // Default selection to first card if exists
            if (!cardList.isEmpty() && selectedFundingCard == null) {
                selectedFundingCard = cardList.get(0);
            }
        });
    }

    private void performTopUp() {
        String amountStr = etOtherAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter PKR amount", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentAccountId.isEmpty()) {
            Toast.makeText(this, "Account still syncing...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedFundingCard == null) {
            Toast.makeText(this, "Please select a card to pay from", Toast.LENGTH_SHORT).show();
            return;
        }

        btnTopUpNow.setEnabled(false);
        btnTopUpNow.setText("Adding PKR...");

        String uid = getCurrentUserId();
        repo.topUp(uid, currentAccountId, amount)
                .addOnSuccessListener(ref -> {
                    Intent intent = new Intent(this, TransferSuccessActivity.class);
                    intent.putExtra("amount", amount);
                    intent.putExtra("adminFee", 0.0);
                    intent.putExtra("referenceNumber", ref);
                    intent.putExtra("recipientName", "Balance Top-Up");
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnTopUpNow.setEnabled(true);
                    btnTopUpNow.setText("Top Up Now");
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return "dev_user_001";
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }
}
