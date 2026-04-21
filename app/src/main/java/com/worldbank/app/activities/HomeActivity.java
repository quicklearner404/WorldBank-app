package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.TransactionAdapter;
import com.worldbank.app.models.Card;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeActivity
 * ─────────────
 * Owner  : Dev 2
 * Mockup : Page 8
 *
 * Shows:
 *  - Total balance
 *  - Purple VISA card widget
 *  - 4 quick action buttons (Transfer, Payments, Top Up, Details)
 *  - Recent transactions list (last 10 from Firestore)
 *  - Bottom navigation bar
 */
public class HomeActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────
    private TextView tvBalanceAmount;
    private TextView tvCardNumber;
    private TextView tvCardHolder;
    private TextView tvCardExpiry;
    private TextView tvSeeMore;
    private RecyclerView rvTransactions;
    private BottomNavigationView bottomNavView;

    // Quick action buttons
    private View btnTransfer;
    private View btnPayments;
    private View btnTopUp;
    private View btnDetails;

    // ── Data ───────────────────────────────────────────────────────
    private TransactionAdapter transactionAdapter;
    private final List<Transaction> transactionList = new ArrayList<>();

    // ── Firebase ───────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    // ── Session ────────────────────────────────────────────────────
    private SessionManager sessionManager;

    // ── Current card data ──────────────────────────────────────────
    private String currentCardId = "";

    // ──────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initFirebase();
        bindViews();
        setupRecyclerView();
        setupQuickActions();
        setupBottomNav();
        loadCardData();
    }

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
    }

    private void bindViews() {
        tvBalanceAmount = findViewById(R.id.tvBalanceAmount);
        tvCardNumber    = findViewById(R.id.tvCardNumber);
        tvCardHolder    = findViewById(R.id.tvCardHolder);
        tvCardExpiry    = findViewById(R.id.tvCardExpiry);
        tvSeeMore       = findViewById(R.id.tvSeeMore);
        rvTransactions  = findViewById(R.id.rvTransactions);
        bottomNavView   = findViewById(R.id.bottomNavView);
        btnTransfer     = findViewById(R.id.btnTransfer);
        btnPayments     = findViewById(R.id.btnPayments);
        btnTopUp        = findViewById(R.id.btnTopUp);
        btnDetails      = findViewById(R.id.btnDetails);
    }

    private void setupRecyclerView() {
        transactionAdapter = new TransactionAdapter(this, transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
        // Disable nested scrolling so NestedScrollView handles all scrolling
        rvTransactions.setNestedScrollingEnabled(false);
    }

    // ══════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ══════════════════════════════════════════════════════════════

    private void setupBottomNav() {
        // Mark Home as selected
        bottomNavView.setSelectedItemId(R.id.nav_home);

        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // Already here
                return true;

            } else if (id == R.id.nav_statistic) {
                startActivity(new Intent(this, CardStatisticActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                return true;

            } else if (id == R.id.nav_budgets) {
                startActivity(new Intent(this, BudgetsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  QUICK ACTIONS
    // ══════════════════════════════════════════════════════════════

    private void setupQuickActions() {

        btnTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendMoneyActivity.class);
            intent.putExtra("cardId", currentCardId);
            startActivity(intent);
        });

        btnPayments.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentsActivity.class);
            intent.putExtra("cardId", currentCardId);
            startActivity(intent);
        });

        btnTopUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, TopUpActivity.class);
            intent.putExtra("cardId", currentCardId);
            startActivity(intent);
        });

        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, CardDetailActivity.class);
            intent.putExtra("cardId", currentCardId);
            startActivity(intent);
        });

        tvSeeMore.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class))
        );
    }

    // ══════════════════════════════════════════════════════════════
    //  FIRESTORE — Load card + transactions
    // ══════════════════════════════════════════════════════════════

    private void loadCardData() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) return;

        // 1. Load the user's first card
        db.collection("cards")
                .whereEqualTo("uid", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        QueryDocumentSnapshot doc =
                                (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                        Card card = doc.toObject(Card.class);
                        card.setCardId(doc.getId());
                        currentCardId = doc.getId();
                        displayCard(card);
                        // 2. After card is loaded, load transactions for that card
                        loadTransactions(uid);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(HomeActivity.this, "Failed to load card data", Toast.LENGTH_SHORT).show()
                );
    }

    private void displayCard(Card card) {
        // Balance
        tvBalanceAmount.setText(String.format("$%.2f", card.getBalance()));

        // Card number — show full masked number from Firestore
        tvCardNumber.setText(card.getMaskedNumber());

        // Holder name
        tvCardHolder.setText(card.getHolderName());

        // Expiry — show as "Exp 09/24"
        tvCardExpiry.setText("Exp " + card.getExpiry());
    }

    private void loadTransactions(String uid) {
        // Load last 10 transactions for this user, newest first
        db.collection("transactions")
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    transactionList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Transaction txn = doc.toObject(Transaction.class);
                        txn.setTxnId(doc.getId());
                        transactionList.add(txn);
                    }
                    transactionAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(HomeActivity.this, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                );
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    /**
     * Returns the current user's UID.
     * Uses DEV_BYPASS fake ID if in dev mode, otherwise uses FirebaseAuth.
     */
    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) {
            return sessionManager.getUserId(); // returns "dev_user_001"
        }
        if (auth.getCurrentUser() != null) {
            return auth.getCurrentUser().getUid();
        }
        return "";
    }

    // ══════════════════════════════════════════════════════════════
    //  BACK PRESS — Disable back on Home (it's the root screen)
    // ══════════════════════════════════════════════════════════════
    @Override
    public void onBackPressed() {
        // Double-tap back to exit, or just do nothing
        // For now, minimize app instead of going back to login
        moveTaskToBack(true);
    }
}
