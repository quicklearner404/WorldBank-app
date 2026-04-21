package com.worldbank.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.models.Card;
import com.worldbank.app.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

/**
 * CardStatisticActivity
 * ──────────────────────
 * Owner  : Dev 2
 * Mockup : Page 10
 *
 * Shows:
 *  - Horizontal card carousel
 *  - Total spending amount
 *  - Day / Week / Month / Year tab switcher
 *  - MPAndroidChart line chart
 *  - Savings and Expenses summary boxes
 *  - Bottom nav (Statistic tab active)
 */
public class CardStatisticActivity extends AppCompatActivity {

    // ── Views ──────────────────────────────────────────────────────
    private TextView tvTotalSpendingAmount;
    private TextView tvSavingsAmount;
    private TextView tvExpensesAmount;
    private TextView tabDay, tabWeek, tabMonth, tabYear;
    private LineChart lineChart;
    private RecyclerView rvCards;
    private BottomNavigationView bottomNavView;
    private ImageButton ibBack;

    // ── Firebase ───────────────────────────────────────────────────
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    // ── Active tab tracking ────────────────────────────────────────
    private TextView activeTab;

    // ──────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_statistic);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupBottomNav();
        setupChart(getDayData(), getDayLabels());  // default: Day tab
        setupTabs();
        loadCards();
        loadSummaryData();
    }

    // ══════════════════════════════════════════════════════════════
    //  INIT
    // ══════════════════════════════════════════════════════════════

    private void bindViews() {
        tvTotalSpendingAmount = findViewById(R.id.tvTotalSpendingAmount);
        tvSavingsAmount       = findViewById(R.id.tvSavingsAmount);
        tvExpensesAmount      = findViewById(R.id.tvExpensesAmount);
        tabDay                = findViewById(R.id.tabDay);
        tabWeek               = findViewById(R.id.tabWeek);
        tabMonth              = findViewById(R.id.tabMonth);
        tabYear               = findViewById(R.id.tabYear);
        lineChart             = findViewById(R.id.lineChart);
        rvCards               = findViewById(R.id.rvCards);
        bottomNavView         = findViewById(R.id.bottomNavView);
        ibBack                = findViewById(R.id.ibBack);
        activeTab             = tabDay; // Day is selected by default

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  TABS
    // ══════════════════════════════════════════════════════════════

    private void setupTabs() {
        tabDay.setOnClickListener(v -> switchTab(tabDay, getDayData(), getDayLabels()));
        tabWeek.setOnClickListener(v -> switchTab(tabWeek, getWeekData(), getWeekLabels()));
        tabMonth.setOnClickListener(v -> switchTab(tabMonth, getMonthData(), getMonthLabels()));
        tabYear.setOnClickListener(v -> switchTab(tabYear, getYearData(), getYearLabels()));
    }

    private void switchTab(TextView selected, List<Entry> data, String[] labels) {
        // Reset previous active tab style
        activeTab.setBackgroundResource(0);
        activeTab.setTextColor(getColor(R.color.gray_text));

        // Apply active style to newly selected tab
        selected.setBackgroundResource(R.drawable.bg_tab_active);
        selected.setTextColor(getColor(R.color.white));

        activeTab = selected;

        // Refresh chart with new data
        setupChart(data, labels);
    }

    // ══════════════════════════════════════════════════════════════
    //  CHART SETUP
    // ══════════════════════════════════════════════════════════════

    private void setupChart(List<Entry> entries, String[] labels) {
        // Style the dataset
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(getColor(R.color.purple_primary));
        dataSet.setValueTextColor(Color.TRANSPARENT);   // hide value labels on points
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getColor(R.color.purple_light));
        dataSet.setFillAlpha(60);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // smooth curve like mockup
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighLightColor(getColor(R.color.purple_primary));
        dataSet.setHighlightLineWidth(1.5f);

        // X axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getColor(R.color.gray_text));
        xAxis.setGranularity(1f);
        xAxis.setTextSize(12f);

        // Y axis left
        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(getColor(R.color.gray_divider));
        yAxisLeft.setTextColor(getColor(R.color.gray_text));
        yAxisLeft.setTextSize(11f);
        yAxisLeft.setAxisMinimum(0f);

        // Hide right Y axis
        lineChart.getAxisRight().setEnabled(false);

        // Chart general styling
        lineChart.setData(new LineData(dataSet));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setDrawBorders(false);
        lineChart.setExtraBottomOffset(8f);
        lineChart.animateX(500);
        lineChart.invalidate();
    }

    // ══════════════════════════════════════════════════════════════
    //  SAMPLE CHART DATA  (replace with real Firestore aggregation)
    // ══════════════════════════════════════════════════════════════

    private List<Entry> getDayData() {
        List<Entry> e = new ArrayList<>();
        e.add(new Entry(0, 320)); e.add(new Entry(1, 350));
        e.add(new Entry(2, 310)); e.add(new Entry(3, 305));
        e.add(new Entry(4, 390));
        return e;
    }
    private String[] getDayLabels() { return new String[]{"Thu","Fri","Sat","Sun","Mon"}; }

    private List<Entry> getWeekData() {
        List<Entry> e = new ArrayList<>();
        e.add(new Entry(0, 1200)); e.add(new Entry(1, 1800));
        e.add(new Entry(2, 1500)); e.add(new Entry(3, 2100));
        return e;
    }
    private String[] getWeekLabels() { return new String[]{"Wk1","Wk2","Wk3","Wk4"}; }

    private List<Entry> getMonthData() {
        List<Entry> e = new ArrayList<>();
        e.add(new Entry(0, 5000)); e.add(new Entry(1, 7200));
        e.add(new Entry(2, 6100)); e.add(new Entry(3, 8900));
        e.add(new Entry(4, 7500)); e.add(new Entry(5, 9000));
        return e;
    }
    private String[] getMonthLabels() { return new String[]{"Jan","Feb","Mar","Apr","May","Jun"}; }

    private List<Entry> getYearData() {
        List<Entry> e = new ArrayList<>();
        e.add(new Entry(0, 60000)); e.add(new Entry(1, 72000));
        e.add(new Entry(2, 68000)); e.add(new Entry(3, 90160));
        return e;
    }
    private String[] getYearLabels() { return new String[]{"2021","2022","2023","2024"}; }

    // ══════════════════════════════════════════════════════════════
    //  FIRESTORE — Load cards for carousel
    // ══════════════════════════════════════════════════════════════

    private void loadCards() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) return;

        db.collection("cards")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Card> cards = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Card card = doc.toObject(Card.class);
                        card.setCardId(doc.getId());
                        cards.add(card);
                    }
                    // TODO: wire up a CardCarouselAdapter once you have 2+ cards
                    // For now the RecyclerView will be empty — that's fine
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  FIRESTORE — Load savings + expenses summary
    // ══════════════════════════════════════════════════════════════

    private void loadSummaryData() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) {
            // Dev placeholder values
            tvTotalSpendingAmount.setText("$90,160.20");
            tvSavingsAmount.setText("$6,120.10");
            tvExpensesAmount.setText("$2,345.00");
            return;
        }

        db.collection("transactions")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalSpending = 0;
                    double savings = 0;
                    double expenses = 0;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Double amount = doc.getDouble("amount");
                        String type   = doc.getString("type");
                        if (amount == null || type == null) continue;

                        if (type.equals("CREDIT")) {
                            savings += amount;
                        } else {
                            expenses += amount;
                            totalSpending += amount;
                        }
                    }

                    tvTotalSpendingAmount.setText(String.format("$%,.2f", totalSpending));
                    tvSavingsAmount.setText(String.format("$%,.2f", savings));
                    tvExpensesAmount.setText(String.format("$%,.2f", expenses));
                });
    }

    // ══════════════════════════════════════════════════════════════
    //  BOTTOM NAV
    // ══════════════════════════════════════════════════════════════

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.nav_statistic);

        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_statistic) {
                return true; // already here
            } else if (id == R.id.nav_budgets) {
                startActivity(new Intent(this, BudgetsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return sessionManager.getUserId();
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }
}
