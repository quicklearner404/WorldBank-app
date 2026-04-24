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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.TransactionAdapter;
import com.worldbank.app.models.Account;
import com.worldbank.app.models.Card;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.FirestoreSeeder;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeActivity — UPGRADED
 * ────────────────────────
 * Now uses real-time Firestore snapshot listeners.
 */
public class HomeActivity extends AppCompatActivity {

    private TextView tvTotalBalance, tvBalanceAmount, tvCardNumber, tvCardHolder, tvCardExpiry, tvSeeMore;
    private RecyclerView rvTransactions;
    private BottomNavigationView bottomNavView;
    private View btnTransfer, btnPayments, btnTopUp, btnDetails;

    private TransactionAdapter transactionAdapter;
    private final List<Transaction> transactionList = new ArrayList<>();
    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private ListenerRegistration accountListener;
    private ListenerRegistration transactionListener;

    private String currentCardId    = "";
    private String currentAccountId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        repo           = new TransactionRepository();

        bindViews();
        setupRecyclerView();
        setupQuickActions();
        setupBottomNav();

        // ── FORCE SEED TRIGGER ──
        // Click the "Total Balance" label to manually seed the DB if it's empty
        tvTotalBalance.setOnClickListener(v -> {
            FirestoreSeeder.seedDevData(this);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (accountListener != null) accountListener.remove();
        if (transactionListener != null) transactionListener.remove();
    }

    private void attachListeners() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) return;

        accountListener = repo.getAccountQuery(uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    if (snapshots.isEmpty()) {
                        // DB is empty for this user — auto seed once
                        if (SessionManager.DEV_BYPASS) FirestoreSeeder.seedDevData(this);
                        return;
                    }

                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
                    Account account = doc.toObject(Account.class);
                    account.setAccountId(doc.getId());
                    currentAccountId = doc.getId();
                    tvBalanceAmount.setText(account.getFormattedBalance());
                    loadCardForAccount(uid);
                });

        transactionListener = repo.getTransactionsQuery(uid, 10)
                .addSnapshotListener((snapshots, e) -> {
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

    private void loadCardForAccount(String uid) {
        repo.getCardsQuery(uid).limit(1).get().addOnSuccessListener(snapshots -> {
            if (!snapshots.isEmpty()) {
                QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
                Card card = doc.toObject(Card.class);
                card.setCardId(doc.getId());
                currentCardId = doc.getId();
                displayCard(card);
            }
        });
    }

    private void displayCard(Card card) {
        tvCardNumber.setText(card.getMaskedNumber());
        tvCardHolder.setText(card.getHolderName());
        tvCardExpiry.setText("Exp " + card.getExpiry());
    }

    private void bindViews() {
        tvTotalBalance  = findViewById(R.id.tvTotalBalance);
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
        rvTransactions.setNestedScrollingEnabled(false);
    }

    private void setupQuickActions() {
        btnTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendMoneyActivity.class);
            intent.putExtra("cardId", currentCardId);
            intent.putExtra("accountId", currentAccountId);
            startActivity(intent);
        });
        btnPayments.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentsActivity.class);
            intent.putExtra("cardId", currentCardId);
            intent.putExtra("accountId", currentAccountId);
            startActivity(intent);
        });
        btnTopUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, TopUpActivity.class);
            intent.putExtra("cardId", currentCardId);
            intent.putExtra("accountId", currentAccountId);
            startActivity(intent);
        });
        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, CardDetailActivity.class);
            intent.putExtra("cardId", currentCardId);
            startActivity(intent);
        });
        tvSeeMore.setOnClickListener(v -> startActivity(new Intent(this, TransactionHistoryActivity.class)));
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

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return sessionManager.getUserId();
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
