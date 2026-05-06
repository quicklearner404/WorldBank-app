package com.worldbank.app.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import com.worldbank.app.activities.home.HomeActivity;
import com.worldbank.app.utils.SessionManager;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvTotalTransactions, tvActiveAccounts;
    private BottomNavigationView bottomNavView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        db      = FirebaseFirestore.getInstance();
        auth    = FirebaseAuth.getInstance();
        session = new SessionManager(this);

        bindViews();
        loadStats();
        setupBottomNav();
    }

    private void bindViews() {
        tvTotalUsers        = findViewById(R.id.tvTotalUsers);
        tvTotalTransactions = findViewById(R.id.tvTotalTransactions);
        tvActiveAccounts    = findViewById(R.id.tvActiveAccounts);
        bottomNavView       = findViewById(R.id.adminBottomNavView);
    }

    private void loadStats() {
        db.collection("users").get()
                .addOnSuccessListener(snap -> tvTotalUsers.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e -> tvTotalUsers.setText("--"));

        db.collection("transactions").get()
                .addOnSuccessListener(snap -> tvTotalTransactions.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e -> tvTotalTransactions.setText("--"));

        db.collection("accounts")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(snap -> tvActiveAccounts.setText(String.valueOf(snap.size())))
                .addOnFailureListener(e -> tvActiveAccounts.setText("--"));
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.admin_nav_dashboard);
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.admin_nav_dashboard) {
                return true;
            } else if (id == R.id.admin_nav_users) {
                startActivity(new Intent(this, AdminUsersActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.admin_nav_transactions) {
                startActivity(new Intent(this, AdminTransactionsActivity.class));
                overridePendingTransition(0, 0);
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
        // back press is intentionally blocked inside admin view
        // admin must use the bottom nav exit button to return to user view
    }
}