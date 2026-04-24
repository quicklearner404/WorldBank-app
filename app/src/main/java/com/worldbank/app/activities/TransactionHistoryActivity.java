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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.TransactionAdapter;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import java.util.ArrayList;
import java.util.List;

public class TransactionHistoryActivity extends AppCompatActivity {

    private ImageButton ibBack, ibClearSearch;
    private EditText etSearch;
    private RecyclerView rvTransactions;

    private TransactionAdapter adapter;
    private final List<Transaction> allTransactions  = new ArrayList<>();
    private final List<Transaction> filteredList     = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        db             = FirebaseFirestore.getInstance();
        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupRecyclerView();
        setupSearch();
        loadTransactions();

        ibBack.setOnClickListener(v -> finish());
    }

    private void bindViews() {
        ibBack        = findViewById(R.id.ibBack);
        ibClearSearch = findViewById(R.id.ibClearSearch);
        etSearch      = findViewById(R.id.etSearch);
        rvTransactions = findViewById(R.id.rvTransactions);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(this, filteredList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                ibClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                filterTransactions(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
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
                String name = txn.getRecipientName() != null ? txn.getRecipientName().toLowerCase() : "";
                String cat  = txn.getCategory()      != null ? txn.getCategory().toLowerCase()      : "";
                if (name.contains(lower) || cat.contains(lower)) {
                    filteredList.add(txn);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadTransactions() {
        String uid = SessionManager.DEV_BYPASS
                ? sessionManager.getUserId()
                : (auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "");

        if (uid.isEmpty()) return;

        db.collection("transactions")
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    allTransactions.clear();
                    for (QueryDocumentSnapshot doc : snapshot) {
                        Transaction txn = doc.toObject(Transaction.class);
                        txn.setTxnId(doc.getId());
                        allTransactions.add(txn);
                    }
                    filteredList.clear();
                    filteredList.addAll(allTransactions);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load transactions", Toast.LENGTH_SHORT).show());
    }
}
