package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.worldbank.app.R;

public class BudgetsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budgets);

        setupTopBar();
        setupBudgetItems();
        setupBottomNav();
    }

    private void setupTopBar() {
        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }
    }

    private void setupBudgetItems() {
        // Groceries
        View groceries = findViewById(R.id.itemGroceries);
        ((ImageView) groceries.findViewById(R.id.ivBudgetIcon)).setImageResource(R.drawable.ic_groceries);
        ((TextView) groceries.findViewById(R.id.tvBudgetName)).setText("Groceries");
        ((ProgressBar) groceries.findViewById(R.id.pbBudget)).setProgress(83);
        ((TextView) groceries.findViewById(R.id.tvBudgetStats)).setText("$1,250 of $1,500");

        // Utilities
        View utilities = findViewById(R.id.itemUtilities);
        ((ImageView) utilities.findViewById(R.id.ivBudgetIcon)).setImageResource(R.drawable.ic_utilities);
        ((TextView) utilities.findViewById(R.id.tvBudgetName)).setText("Utilities");
        ((ProgressBar) utilities.findViewById(R.id.pbBudget)).setProgress(96);
        ((TextView) utilities.findViewById(R.id.tvBudgetStats)).setText("$480 of $500");

        // Dining
        View dining = findViewById(R.id.itemDining);
        ((ImageView) dining.findViewById(R.id.ivBudgetIcon)).setImageResource(R.drawable.ic_dining);
        ((TextView) dining.findViewById(R.id.tvBudgetName)).setText("Dining Out");
        ((ProgressBar) dining.findViewById(R.id.pbBudget)).setProgress(92);
        ((TextView) dining.findViewById(R.id.tvBudgetStats)).setText("$920 of $1,000");
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNavView = findViewById(R.id.bottomNavView);
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
