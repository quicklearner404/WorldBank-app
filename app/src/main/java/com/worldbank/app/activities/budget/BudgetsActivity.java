package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class BudgetsActivity extends AppCompatActivity {

    private ProgressBar pbTotalBudget;
    private TextView tvTotalBudgetStats, tvSpentPercent;
    private View itemGroceries, itemUtilities, itemDining;
    private Button btnCreateBudget;
    private BottomNavigationView bottomNavView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager sessionManager;
    private ListenerRegistration budgetListener;

    private double limitTotal = 55000.0;
    private final double LIMIT_GROCERIES = 30000.0;
    private final double LIMIT_UTILITIES = 15000.0;
    private final double LIMIT_DINING    = 10000.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budgets);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupExpandableLogic();
        setupClickListeners();
        setupBottomNav();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadRealBudgetData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (budgetListener != null) budgetListener.remove();
    }

    private void bindViews() {
        pbTotalBudget      = findViewById(R.id.pbTotalBudget);
        tvTotalBudgetStats = findViewById(R.id.tvTotalBudgetStats);
        tvSpentPercent     = findViewById(R.id.tvSpentPercent);
        itemGroceries      = findViewById(R.id.itemGroceries);
        itemUtilities      = findViewById(R.id.itemUtilities);
        itemDining         = findViewById(R.id.itemDining);
        btnCreateBudget    = findViewById(R.id.btnCreateBudget);
        bottomNavView      = findViewById(R.id.bottomNavView);

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());
    }

    private void setupExpandableLogic() {
        setupCategoryToggle(itemGroceries);
        setupCategoryToggle(itemUtilities);
        setupCategoryToggle(itemDining);
    }

    private void setupCategoryToggle(View categoryView) {
        // Corrected: Set listener on the header instead of the container
        View header = categoryView.findViewById(R.id.clCategoryHeader);
        LinearLayout list = categoryView.findViewById(R.id.llTransactionList);
        ImageView chevron = categoryView.findViewById(R.id.ivChevron);

        if (header != null) {
            header.setOnClickListener(v -> {
                if (list.getVisibility() == View.VISIBLE) {
                    list.setVisibility(View.GONE);
                    chevron.animate().rotation(90).start();
                } else {
                    list.setVisibility(View.VISIBLE);
                    chevron.animate().rotation(270).start();
                    if (list.getChildCount() == 0) {
                        addEmptyHint(list);
                    }
                }
            });
        }
    }

    private void setupClickListeners() {
        btnCreateBudget.setOnClickListener(v -> showBudgetGoalDialog());
    }

    private void showBudgetGoalDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_consumer_id, null);

        TextView title    = view.findViewById(R.id.tvDialogTitle);
        TextView subLabel = view.findViewById(R.id.tvConsumerIdLabel); // Now we can find it!
        EditText input    = view.findViewById(R.id.etConsumerId);
        Button btn        = view.findViewById(R.id.btnContinuePayment);
        Button cancel     = view.findViewById(R.id.btnCancelDialog);

        // Change strings for Budget context
        title.setText("Set New Spending Goal");
        if (subLabel != null) {
            subLabel.setText("Enter the maximum amount you want to spend this month.");
        }

        input.setHint("Goal Amount (Rs.)");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        btn.setText("Update Goal");

        btn.setOnClickListener(v -> {
            String val = input.getText().toString();
            if (!val.isEmpty()) {
                limitTotal = Double.parseDouble(val);

                // Force a data refresh to update the Pie Chart
                loadRealBudgetData();

                Toast.makeText(this, "Spending goal updated!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(view);
        dialog.show();
    }

    private void loadRealBudgetData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;

        budgetListener = db.collection("transactions")
                .whereEqualTo("uid", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    double totalSpent = 0;
                    List<Transaction> gTxns = new ArrayList<>();
                    List<Transaction> uTxns = new ArrayList<>();
                    List<Transaction> dTxns = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Transaction t = doc.toObject(Transaction.class);

                        // --- FIX START ---
                        String type = t.getType() != null ? t.getType().toLowerCase() : "";

                        // Broaden the check: Catch "debit", "DEBIT", or "expense"
                        if (t.getAmount() <= 0 || (!type.equals("debit") && !type.equals("expense"))) {
                            continue;
                        }
                        // --- FIX END ---

                        totalSpent += t.getAmount();

                        String cat = t.getCategory() != null ? t.getCategory().toLowerCase() : "";
                        String rec = t.getRecipientName() != null ? t.getRecipientName().toLowerCase() : "";

                        // Category sorting
                        if (cat.contains("grocer") || rec.contains("mart") || rec.contains("imtiaz")) {
                            gTxns.add(t);
                        } else if (cat.contains("util") || cat.contains("bill") || rec.contains("lesco") || rec.contains("ke") || rec.contains("ptcl")) {
                            uTxns.add(t);
                        } else if (cat.contains("dining") || cat.contains("food") || rec.contains("panda") || rec.contains("kfc")) {
                            dTxns.add(t);
                        }
                    }

                    updateUI(totalSpent, gTxns, uTxns, dTxns);
                });
    }

    private void updateUI(double total, List<Transaction> g, List<Transaction> u, List<Transaction> d) {
        // 1. Calculate percentage using double for precision
        double percentage = (total / limitTotal) * 100;

        // 2. Round UP for(visual feedback)
        int progressInt = (int) Math.ceil(percentage);

        // 3. Ensure we don't exceed 100
        int finalProgress = Math.min(progressInt, 100);

        // Update the Circular Bar (Pie Chart)
        pbTotalBudget.setMax(100);
        pbTotalBudget.setProgress(finalProgress);

        if (tvSpentPercent != null) {

            tvSpentPercent.setText(String.format(Locale.getDefault(), "%d%%", finalProgress));
        }

        tvTotalBudgetStats.setText(String.format(Locale.getDefault(), "Rs. %,.0f Spent / Rs. %,.0f Goal", total, limitTotal));

        // Update Category Bars
        updateCategory(itemGroceries, "Groceries", R.drawable.ic_groceries, g, LIMIT_GROCERIES);
        updateCategory(itemUtilities, "Utilities", R.drawable.ic_utilities, u, LIMIT_UTILITIES);
        updateCategory(itemDining, "Dining Out", R.drawable.ic_dining, d, LIMIT_DINING);
    }

    private void updateCategory(View view, String name, int icon, List<Transaction> txns, double limit) {
        double spent = 0;
        LinearLayout listContainer = view.findViewById(R.id.llTransactionList);
        listContainer.removeAllViews();

        for (Transaction t : txns) {
            spent += t.getAmount();
            addTransactionEntry(listContainer, t);
        }

        ((ImageView) view.findViewById(R.id.ivBudgetIcon)).setImageResource(icon);
        ((TextView) view.findViewById(R.id.tvBudgetName)).setText(name);
        
        ProgressBar pb = view.findViewById(R.id.pbBudget);
        int p = (int) ((spent / limit) * 100);
        pb.setProgress(Math.min(p, 100));
        
        ((TextView) view.findViewById(R.id.tvBudgetStats)).setText(String.format(Locale.getDefault(), "Rs. %,.0f of Rs. %,.0f", spent, limit));
    }

    private void addTransactionEntry(LinearLayout container, Transaction t) {
        TextView tv = new TextView(this);
        String name = t.getRecipientName() != null ? t.getRecipientName() : "Payment";
        tv.setText(String.format(Locale.getDefault(), "• %s: Rs. %,.0f", name, t.getAmount()));
        tv.setTextColor(getColor(R.color.black));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setPadding(0, 8, 0, 8);
        container.addView(tv);
    }
    
    private void addEmptyHint(LinearLayout container) {
        TextView tv = new TextView(this);
        tv.setText("No transactions found.");
        tv.setTextColor(getColor(R.color.gray_text));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        container.addView(tv);
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
