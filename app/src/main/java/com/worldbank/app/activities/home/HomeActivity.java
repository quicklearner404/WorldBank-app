package com.worldbank.app.activities.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
import java.util.Locale;
import com.worldbank.app.activities.account.AccountActivity;
import com.worldbank.app.activities.budget.BudgetsActivity;
import com.worldbank.app.activities.cards.AddCardActivity;
import com.worldbank.app.activities.cards.CardDetailActivity;
import com.worldbank.app.activities.cards.CardStatisticActivity;
import com.worldbank.app.activities.payments.PaymentsActivity;
import com.worldbank.app.activities.transaction.SendMoneyActivity;
import com.worldbank.app.activities.transaction.TransactionHistoryActivity;
import com.worldbank.app.activities.payments.TopUpActivity;
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
    private double globalAccountBalance = 0.0; // Source of truth for PKR balance
    private int currentSelectedPosition = 0; // Tracks which card is active
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
        // Keeps cards centered, but we'll use clicks for navigation to be stable
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvCards);
    }

    @Override
    public void onCardClick(Card card, int position) {
        currentCardId = card.getCardId();
        currentSelectedPosition = position; // Save the position!

        updateBalanceUI(card);

        Intent intent = new Intent(this, CardDetailActivity.class);
        intent.putExtra("cardId", card.getCardId());
        startActivity(intent);
    }

    private void updateBalanceUI(Card card) {
        boolean isInternal = card.getAccountId() != null && !card.getAccountId().isEmpty();
        if (isInternal) {
            // Internal cards show the actual account balance from Firestore
            tvBalanceAmount.setText(String.format(Locale.getDefault(), "Rs. %,.2f", globalAccountBalance));
        } else {
            // External cards show 0.00 since we don't have their balance data
            tvBalanceAmount.setText("Rs. 0.00");
        }
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
            globalAccountBalance = account.getBalance();

            // Use the saved position instead of always index 0
            if (!cardList.isEmpty() && currentSelectedPosition < cardList.size()) {
                updateBalanceUI(cardList.get(currentSelectedPosition));
            }
        });

        cardListener = repo.getCardsQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            cardList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Card card = doc.toObject(Card.class);
                card.setCardId(doc.getId());
                cardList.add(card);
            }

            if (!cardList.isEmpty()) {
                // Restore selection if the list is still valid
                if (currentSelectedPosition >= cardList.size()) currentSelectedPosition = 0;

                Card selectedCard = cardList.get(currentSelectedPosition);
                currentCardId = selectedCard.getCardId();

                // Sync the adapter's highlight
                cardAdapter.setSelectedPosition(currentSelectedPosition);
                updateBalanceUI(selectedCard);
            }
            cardAdapter.notifyDataSetChanged();
        });

        // 3. Transactions Listener
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
        super.onBackPressed();
        moveTaskToBack(true);
    }
}
