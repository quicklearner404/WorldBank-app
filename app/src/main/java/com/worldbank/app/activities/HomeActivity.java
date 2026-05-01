package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

    private TextView tvBalanceAmount, tvSeeMore;
    private RecyclerView rvTransactions, rvCards;
    private BottomNavigationView bottomNavView;
    private View btnTransfer, btnPayments, btnTopUp, btnAddCard;

    private TransactionAdapter transactionAdapter;
    private CardAdapter cardAdapter;
    
    private final List<Transaction> transactionList = new ArrayList<>();
    private final List<Card> cardList = new ArrayList<>();
    
    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private ListenerRegistration accountListener;
    private ListenerRegistration cardListener;
    private ListenerRegistration transactionListener;

    private String currentAccountId = "";
    private String currentCardId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        repo           = new TransactionRepository();

        bindViews();
        setupRecyclers();
        setupQuickActions();
        setupBottomNav();
        setupSnapHelper();
    }

    private void setupSnapHelper() {
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvCards);

        rvCards.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View centerView = snapHelper.findSnapView(rvCards.getLayoutManager());
                    if (centerView != null) {
                        int pos = rvCards.getLayoutManager().getPosition(centerView);
                        if (pos < cardList.size()) {
                            currentCardId = cardList.get(pos).getCardId();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onCardClick(Card card, int position) {
        currentCardId = card.getCardId();
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
        if (accountListener != null) accountListener.remove();
        if (cardListener != null) cardListener.remove();
        if (transactionListener != null) transactionListener.remove();
    }

    private void attachListeners() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) return;

        accountListener = repo.getAccountQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null || snapshots.isEmpty()) return;
            QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
            Account account = doc.toObject(Account.class);
            currentAccountId = doc.getId();
            tvBalanceAmount.setText(account.getFormattedBalance());
        });

        cardListener = repo.getCardsQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            cardList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Card card = doc.toObject(Card.class);
                card.setCardId(doc.getId());
                cardList.add(card);
            }
            if (!cardList.isEmpty() && currentCardId.isEmpty()) {
                currentCardId = cardList.get(0).getCardId();
            }
            cardAdapter.notifyDataSetChanged();
        });

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

    private void bindViews() {
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
        rvCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCards.setAdapter(cardAdapter);
    }

    private void setupQuickActions() {
        btnTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(this, SendMoneyActivity.class);
            intent.putExtra("accountId", currentAccountId);
            intent.putExtra("cardId", currentCardId);
            startActivity(intent);
        });
        
        btnPayments.setOnClickListener(v -> {
            Intent intent = new Intent(this, PaymentsActivity.class);
            intent.putExtra("accountId", currentAccountId);
            intent.putExtra("cardId", currentCardId);
            startActivity(intent);
        });

        btnTopUp.setOnClickListener(v -> {
            Intent intent = new Intent(this, TopUpActivity.class);
            intent.putExtra("accountId", currentAccountId);
            startActivity(intent);
        });

        btnAddCard.setOnClickListener(v -> startActivity(new Intent(this, AddCardActivity.class)));
        
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
        if (SessionManager.DEV_BYPASS) return "dev_user_001";
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
