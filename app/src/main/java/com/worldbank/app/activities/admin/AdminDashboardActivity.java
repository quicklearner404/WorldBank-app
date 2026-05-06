package com.worldbank.app.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.worldbank.app.R;
import com.worldbank.app.activities.home.HomeActivity;
import com.worldbank.app.models.Transaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminTransactionsActivity extends AppCompatActivity {

    private RecyclerView rvTransactions;
    private TextView tvEmpty;
    private BottomNavigationView bottomNavView;

    private FirebaseFirestore db;
    private final List<DocumentSnapshot> txnList = new ArrayList<>();
    private TransactionsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_transactions);

        db = FirebaseFirestore.getInstance();

        rvTransactions = findViewById(R.id.rvTransactions);
        tvEmpty        = findViewById(R.id.tvEmpty);
        bottomNavView  = findViewById(R.id.adminBottomNavView);

        adapter = new TransactionsAdapter(txnList);
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        setupBottomNav();
        loadTransactions();
    }

    private void loadTransactions() {
        db.collection("transactions")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener(snap -> {
                    txnList.clear();
                    txnList.addAll(snap.getDocuments());
                    adapter.notifyDataSetChanged();

                    tvEmpty.setVisibility(txnList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvTransactions.setVisibility(txnList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.admin_nav_transactions);
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.admin_nav_dashboard) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.admin_nav_users) {
                startActivity(new Intent(this, AdminUsersActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.admin_nav_transactions) {
                return true;
            } else if (id == R.id.admin_nav_exit) {
                showExitDialog();
                return false;
            }
            return false;
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Admin View")
                .setMessage("Are you sure you want to return to the user view?")
                .setPositiveButton("Exit", (dialog, which) -> switchToUserView())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void switchToUserView() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // back press blocked inside admin view
    }

    // adapter unchanged below this line
    private static class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.VH> {

        private final List<DocumentSnapshot> list;

        TransactionsAdapter(List<DocumentSnapshot> list) {
            this.list = list;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_transaction, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = list.get(position);

            Double amount    = doc.getDouble("amount");
            String type      = doc.getString("type");
            String recipient = doc.getString("recipientName");
            String senderUid = doc.getString("senderUid");
            String ref       = doc.getString("referenceNumber");

            if (amount == null)    amount    = 0.0;
            if (type == null)      type      = "";
            if (recipient == null) recipient = "";
            if (senderUid == null) senderUid = "";
            if (ref == null)       ref       = "";

            holder.tvAmount.setText(String.format("Rs. %,.0f", amount));
            holder.tvRecipient.setText(recipient);
            holder.tvSender.setText(senderUid);
            holder.tvRef.setText(ref);

            boolean isCredit = Transaction.TYPE_CREDIT.equals(type);
            holder.tvType.setText(type);
            holder.tvType.setTextColor(isCredit
                    ? holder.itemView.getContext().getColor(R.color.green_credit)
                    : holder.itemView.getContext().getColor(R.color.red_debit));

            com.google.firebase.Timestamp ts = doc.getTimestamp("timestamp");
            if (ts != null) {
                String formatted = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        .format(ts.toDate());
                holder.tvTimestamp.setText(formatted);
            } else {
                holder.tvTimestamp.setText("");
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvAmount, tvType, tvRecipient, tvSender, tvRef, tvTimestamp;

            VH(View v) {
                super(v);
                tvAmount    = v.findViewById(R.id.tvTxnAmount);
                tvType      = v.findViewById(R.id.tvTxnType);
                tvRecipient = v.findViewById(R.id.tvTxnRecipient);
                tvSender    = v.findViewById(R.id.tvTxnSender);
                tvRef       = v.findViewById(R.id.tvTxnRef);
                tvTimestamp = v.findViewById(R.id.tvTxnTimestamp);
            }
        }
    }
}