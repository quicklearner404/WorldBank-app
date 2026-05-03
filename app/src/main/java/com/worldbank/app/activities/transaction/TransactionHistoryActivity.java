package com.worldbank.app.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.TransactionAdapter;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;


public class TransactionHistoryActivity extends AppCompatActivity {

    private ImageButton ibBack, ibClearSearch;
    private EditText etSearch;
    private RecyclerView rvTransactions;

    private TransactionAdapter adapter;
    private final List<Transaction> allTransactions = new ArrayList<>();
    private final List<Transaction> filteredList    = new ArrayList<>();

    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    // Real-time listener
    private ListenerRegistration transactionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        repo           = new TransactionRepository();
        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupRecyclerView();
        setupSearch();

        ibBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (transactionListener != null) transactionListener.remove();
    }

    private void attachListener() {
        String uid = SessionManager.DEV_BYPASS
                ? sessionManager.getUserId()
                : (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "");

        if (uid.isEmpty()) return;

        // Load up to 50 transactions — more history than home screen
        transactionListener = repo.getTransactionsQuery(uid, 50)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Toast.makeText(this, "Could not load transactions",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    allTransactions.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Transaction txn = doc.toObject(Transaction.class);
                        txn.setTxnId(doc.getId());
                        allTransactions.add(txn);
                    }

                    // Re-apply current search filter
                    String currentQuery = etSearch.getText().toString();
                    filterTransactions(currentQuery);
                });
    }

    private void bindViews() {
        ibBack         = findViewById(R.id.ibBack);
        ibClearSearch  = findViewById(R.id.ibClearSearch);
        etSearch       = findViewById(R.id.etSearch);
        rvTransactions = findViewById(R.id.rvTransactions);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this, filteredList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                ibClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterTransactions(s.toString());
            }
        });
        ibClearSearch.setOnClickListener(v -> etSearch.setText(""));
    }

    private void filterTransactions(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(allTransactions);
        } else {
            String lower = query.toLowerCase();
            for (Transaction txn : allTransactions) {
                String name = txn.getRecipientName() != null
                        ? txn.getRecipientName().toLowerCase() : "";
                String cat  = txn.getCategory() != null
                        ? txn.getCategory().toLowerCase() : "";
                String ref  = txn.getReferenceNumber() != null
                        ? txn.getReferenceNumber().toLowerCase() : "";
                if (name.contains(lower) || cat.contains(lower) || ref.contains(lower)) {
                    filteredList.add(txn);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}