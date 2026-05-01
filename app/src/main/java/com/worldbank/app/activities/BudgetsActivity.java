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

import java.util.ArrayList;
import java.util.List;

/**
 * BudgetsActivity
 * ───────────────
 * Real-time budget tracker. Classifies transactions into Groceries, Utilities, and Dining.
 */
public class BudgetsActivity extends AppCompatActivity {

    private static final String TAG = "BudgetsActivity";

    private ProgressBar pbTotalBudget;
    private TextView tvTotalBudgetStats;
    private View itemGroceries, itemUtilities, itemDining;
    private BottomNavigationView bottomNavView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    // Realistic PKR Monthly Limits
    private final double LIMIT_GROCERIES = 30000.0;
    private final double LIMIT_UTILITIES = 15000.0;
    private final double LIMIT_DINING    = 10000.0;
    private final double LIMIT_TOTAL     = 70000.0;

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

        // Snapshot listener for real-time budget updates
        db.collection("transactions")
                .whereEqualTo("uid", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) {
                        Log.e(TAG, "Budget listener failed", e);
                        return;
                    }

                    double spentGroceries = 0, spentUtilities = 0, spentDining = 0, totalSpent = 0;

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t.getAmount() <= 0 || !Transaction.TYPE_DEBIT.equals(t.getType())) continue;

                        totalSpent += t.getAmount();

                        String cat = t.getCategory() != null ? t.getCategory().toLowerCase() : "";
                        String rec = t.getRecipientName() != null ? t.getRecipientName().toLowerCase() : "";

                        // Smart Classification Engine (Keyword matching for Pakistan)
                        if (cat.contains("grocer") || rec.contains("imtiaz") || rec.contains("alfatah") || 
                            rec.contains("metro") || rec.contains("mart") || rec.contains("store") || rec.contains("pharmacy")) {
                            spentGroceries += t.getAmount();
                        } else if (cat.contains("util") || cat.contains("bill") || 
                                   rec.contains("lesco") || rec.contains("ke") || rec.contains("electric") || 
                                   rec.contains("sngpl") || rec.contains("ssgc") || rec.contains("ptcl") || 
                                   rec.contains("fiber") || rec.contains("nayatel")) {
                            spentUtilities += t.getAmount();
                        } else if (cat.contains("dining") || cat.contains("food") || 
                                   rec.contains("panda") || rec.contains("kfc") || rec.contains("mcdonald") || 
                                   rec.contains("restaurant") || rec.contains("cafe")) {
                            spentDining += t.getAmount();
                        }
                    }

                    updateUI(spentGroceries, spentUtilities, spentDining, totalSpent);
                });
    }

    private void updateUI(double groceries, double utilities, double dining, double total) {
        // Update Circular Overview
        int totalProgress = (int) ((total / LIMIT_TOTAL) * 100);
        pbTotalBudget.setProgress(Math.min(totalProgress, 100));
        tvTotalBudgetStats.setText(String.format("Rs. %,.0f Spent / Rs. %,.0f Budget", total, LIMIT_TOTAL));

        // Update Detailed Items
        updateCategoryItem(itemGroceries, "Groceries", R.drawable.ic_groceries, groceries, LIMIT_GROCERIES);
        updateCategoryItem(itemUtilities, "Utilities", R.drawable.ic_utilities, utilities, LIMIT_UTILITIES);
        updateCategoryItem(itemDining, "Dining Out", R.drawable.ic_dining, dining, LIMIT_DINING);
    }

    private void updateCategoryItem(View view, String name, int iconRes, double spent, double limit) {
        ((ImageView) view.findViewById(R.id.ivBudgetIcon)).setImageResource(iconRes);
        ((TextView) view.findViewById(R.id.tvBudgetName)).setText(name);
        ProgressBar pb = view.findViewById(R.id.pbBudget);
        int progress = (int) ((spent / limit) * 100);
        pb.setProgress(Math.min(progress, 100));
        ((TextView) view.findViewById(R.id.tvBudgetStats)).setText(String.format("Rs. %,.0f of Rs. %,.0f", spent, limit));
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.nav_budgets);
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, HomeActivity.class)); finish(); return true; }
            if (id == R.id.nav_statistic) { startActivity(new Intent(this, CardStatisticActivity.class)); finish(); return true; }
            if (id == R.id.nav_account) { startActivity(new Intent(this, AccountActivity.class)); finish(); return true; }
            return id == R.id.nav_budgets;
        });
    }
}
