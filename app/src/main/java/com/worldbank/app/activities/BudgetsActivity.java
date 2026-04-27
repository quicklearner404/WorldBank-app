package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;

import java.util.HashMap;
import java.util.Map;

/**
 * BudgetsActivity
 * ───────────────
 * Calculates real spending vs budget limits by category.
 */
public class BudgetsActivity extends AppCompatActivity {

    private static final String TAG = "BudgetsActivity";

    // Overall stats
    private ProgressBar pbTotalBudget;
    private TextView tvTotalBudgetStats;

    // Category progress bars and stats
    private View itemGroceries, itemUtilities, itemDining;
    private BottomNavigationView bottomNavView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    // Hardcoded Budget Limits (In real apps, these are stored in a 'budgets' collection)
    private final double LIMIT_GROCERIES = 15000.0;
    private final double LIMIT_UTILITIES = 10000.0;
    private final double LIMIT_DINING    = 8000.0;
    private final double LIMIT_TOTAL     = 50000.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budgets);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        loadRealBudgetData();
        setupBottomNav();
    }

    private void bindViews() {
        pbTotalBudget      = findViewById(R.id.pbTotalBudget);
        tvTotalBudgetStats = findViewById(R.id.tvTotalBudgetStats);
        itemGroceries      = findViewById(R.id.itemGroceries);
        itemUtilities      = findViewById(R.id.itemUtilities);
        itemDining         = findViewById(R.id.itemDining);
        bottomNavView      = findViewById(R.id.bottomNavView);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());
    }

    private void loadRealBudgetData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;

        // Fetch all transactions for this user to calculate category spending
        db.collection("transactions")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(snapshots -> {
                    double spentGroceries = 0, spentUtilities = 0, spentDining = 0, totalSpent = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String type = doc.getString("type");
                        String category = doc.getString("category");
                        Double amount = doc.getDouble("amount");

                        if (amount == null || type == null || category == null) continue;

                        // Only count Debits (Spending)
                        if (Transaction.TYPE_DEBIT.equals(type)) {
                            totalSpent += amount;
                            if (category.toLowerCase().contains("grocer")) spentGroceries += amount;
                            else if (category.toLowerCase().contains("util")) spentUtilities += amount;
                            else if (category.toLowerCase().contains("dining") || category.toLowerCase().contains("food")) spentDining += amount;
                        }
                    }

                    updateUI(spentGroceries, spentUtilities, spentDining, totalSpent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error calculating budgets", e);
                    Toast.makeText(this, "Failed to load budget data", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI(double groceries, double utilities, double dining, double total) {
        // 1. Total Spending Overview
        int totalProgress = (int) ((total / LIMIT_TOTAL) * 100);
        pbTotalBudget.setProgress(Math.min(totalProgress, 100));
        tvTotalBudgetStats.setText(String.format("Rs. %,.0f Spent / Rs. %,.0f Budget", total, LIMIT_TOTAL));

        // 2. Groceries
        updateCategoryItem(itemGroceries, "Groceries", R.drawable.ic_groceries, groceries, LIMIT_GROCERIES);
        
        // 3. Utilities
        updateCategoryItem(itemUtilities, "Utilities", R.drawable.ic_utilities, utilities, LIMIT_UTILITIES);
        
        // 4. Dining
        updateCategoryItem(itemDining, "Dining Out", R.drawable.ic_dining, dining, LIMIT_DINING);
    }

    private void updateCategoryItem(View view, String name, int iconRes, double spent, double limit) {
        ((ImageView) view.findViewById(R.id.ivBudgetIcon)).setImageResource(iconRes);
        ((TextView) view.findViewById(R.id.tvBudgetName)).setText(name);
        
        ProgressBar pb = view.findViewById(R.id.pbBudget);
        int progress = (int) ((spent / limit) * 100);
        pb.setProgress(Math.min(progress, 100));
        
        ((TextView) view.findViewById(R.id.tvBudgetStats)).setText(
                String.format("Rs. %,.0f of Rs. %,.0f", spent, limit));
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.nav_budgets);
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_statistic) {
                startActivity(new Intent(this, CardStatisticActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_budgets;
        });
    }
}
