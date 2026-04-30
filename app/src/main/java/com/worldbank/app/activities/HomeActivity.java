package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.CardAdapter;
import com.worldbank.app.adapters.TransactionAdapter;
import com.worldbank.app.models.Account;
import com.worldbank.app.models.Card;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements CardAdapter.OnCardClickListener {

    TextView tvBalanceAmount;
    TextView tvSeeMore;
    RecyclerView rvTransactions;
    RecyclerView rvCards;
    BottomNavigationView bottomNavView;
    View btnTransfer;
    View btnPayments;
    View btnTopUp;
    View btnAddCard;

    TransactionAdapter transactionAdapter;
    CardAdapter cardAdapter;

    final List<Transaction> transactionList = new ArrayList<>();
    final List<Card> cardList = new ArrayList<>();

    TransactionRepository repo;
    FirebaseAuth auth;
    SessionManager sessionManager;

    ListenerRegistration accountListener;
    ListenerRegistration cardListener;
    ListenerRegistration transactionListener;

    // Account and card IDs are stored so they can be passed to SendMoneyActivity
    String currentAccountId = "";
    String currentCardId    = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        repo           = new TransactionRepository();

        init();
        setupRecyclers();
        setupQuickActions();
        setupBottomNav();
        setupSnapHelper();
        setupBackPress();
    }

    private void setupSnapHelper() {
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvCards);
    }

    private void setupBackPress() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Move to background instead of destroying the activity
                moveTaskToBack(true);
            }
        });
    }

    @Override
    public void onCardClick(Card card, int position) {
        // Store the tapped card so the transfer screen always has the right cardId
        currentCardId = card.getCardId() != null ? card.getCardId() : "";

        Intent intent = new Intent(this, CardDetailActivity.class);
        intent.putExtra("cardId", card.getCardId());
        startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (accountListener    != null) accountListener.remove();
        if (cardListener       != null) cardListener.remove();
        if (transactionListener != null) transactionListener.remove();
    }

    private void attachListeners() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) return;

        // Listen for balance changes and store the account ID
        accountListener = repo.getAccountQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null || snapshots.isEmpty()) return;
            QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
            Account account = doc.toObject(Account.class);
            currentAccountId = doc.getId();
            tvBalanceAmount.setText(account.getFormattedBalance());
        });

        // Listen for card updates and keep the first card ID ready for transfers
        cardListener = repo.getCardsQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            cardList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Card card = doc.toObject(Card.class);
                card.setCardId(doc.getId());
                cardList.add(card);
            }
            // Keep the first card as the default for the transfer screen
            if (!cardList.isEmpty() && currentCardId.isEmpty()) {
                currentCardId = cardList.get(0).getCardId();
            }
            cardAdapter.notifyDataSetChanged();
        });

        // Listen for the ten most recent transactions
        transactionListener = repo.getTransactionsQuery(uid, 10).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            transactionList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Transaction txn = doc.toObject(Transaction.class);
                txn.setTxnId(doc.getId());
                transactionList.add(txn);
            }
            transactionAdapter.notifyDataSetChanged();
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════

    private void init() {
        tvBalanceAmount = findViewById(R.id.tvBalanceAmount);
        tvSeeMore       = findViewById(R.id.tvSeeMore);
        rvTransactions  = findViewById(R.id.rvTransactions);
        rvCards         = findViewById(R.id.rvCards);
        bottomNavView   = findViewById(R.id.bottomNavView);
        btnTransfer     = findViewById(R.id.btnTransfer);
        btnPayments     = findViewById(R.id.btnPayments);
        btnTopUp        = findViewById(R.id.btnTopUp);
        btnAddCard      = findViewById(R.id.btnAddCard);
    }

    private void setupRecyclers() {
        transactionAdapter = new TransactionAdapter(this, transactionList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(transactionAdapter);
        rvTransactions.setNestedScrollingEnabled(false);

        cardAdapter = new CardAdapter(this, cardList, this);
        rvCards.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCards.setAdapter(cardAdapter);
    }

    private void setupQuickActions() {
        btnTransfer.setOnClickListener(v -> {
            // Pass both accountId and cardId so SendMoneyActivity can load the card and deduct balance
            Intent intent = new Intent(this, SendMoneyActivity.class);
            intent.putExtra("accountId", currentAccountId);
            intent.putExtra("cardId",    currentCardId);
            startActivity(intent);
        });

        btnPayments.setOnClickListener(v ->
                startActivity(new Intent(this, PaymentsActivity.class)));

        btnTopUp.setOnClickListener(v ->
                startActivity(new Intent(this, TopUpActivity.class)));

        btnAddCard.setOnClickListener(v ->
                startActivity(new Intent(this, AddCardActivity.class)));

        tvSeeMore.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class)));
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.nav_home);
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            if (id == R.id.nav_statistic) {
                startActivity(new Intent(this, CardStatisticActivity.class));
                return true;
            }
            if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            }
            if (id == R.id.nav_budgets) {
                startActivity(new Intent(this, BudgetsActivity.class));
                return true;
            }
            return false;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return "dev_user_001";
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }
}