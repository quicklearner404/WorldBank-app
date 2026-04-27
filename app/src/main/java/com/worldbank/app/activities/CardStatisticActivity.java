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
import com.worldbank.app.adapters.CardAdapter;
import com.worldbank.app.models.Card;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;

public class CardStatisticActivity extends AppCompatActivity implements CardAdapter.OnCardClickListener {

    private TextView tvTotalSpendingAmount;
    private TextView tvSavingsAmount;
    private TextView tvExpensesAmount;
    private TextView tabDay, tabWeek, tabMonth, tabYear;
    private LineChart lineChart;
    private RecyclerView rvCards;
    private BottomNavigationView bottomNavView;
    private ImageButton ibBack;

    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private TextView activeTab;
    private CardAdapter cardAdapter;
    private List<Card> cardList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_statistic);

        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);
        repo = new TransactionRepository();

        bindViews();
        setupRecyclerView();
        setupBottomNav();
        setupChart(getDayData(), getDayLabels());
        setupTabs();
        loadCards();
        loadSummaryData();
    }

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
        activeTab             = tabDay;

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        // Fixed: Added 'this' as the third argument to match the new CardAdapter constructor
        cardAdapter = new CardAdapter(this, cardList, this);
        rvCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCards.setAdapter(cardAdapter);
    }

    @Override
    public void onCardClick(Card card, int position) {
        // Optional: Filter statistics based on the selected card
    }

    private void setupTabs() {
        tabDay.setOnClickListener(v -> switchTab(tabDay, getDayData(), getDayLabels()));
        tabWeek.setOnClickListener(v -> switchTab(tabWeek, getWeekData(), getWeekLabels()));
        tabMonth.setOnClickListener(v -> switchTab(tabMonth, getMonthData(), getMonthLabels()));
        tabYear.setOnClickListener(v -> switchTab(tabYear, getYearData(), getYearLabels()));
    }

    private void switchTab(TextView selected, List<Entry> data, String[] labels) {
        activeTab.setBackgroundResource(0);
        activeTab.setTextColor(getColor(R.color.gray_text));
        selected.setBackgroundResource(R.drawable.bg_tab_active);
        selected.setTextColor(getColor(R.color.white));
        activeTab = selected;
        setupChart(data, labels);
    }

    private void setupChart(List<Entry> entries, String[] labels) {
        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(getColor(R.color.purple_primary));
        dataSet.setValueTextColor(Color.TRANSPARENT);
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getColor(R.color.purple_light));
        dataSet.setFillAlpha(60);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setTextColor(getColor(R.color.gray_text));

        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setGridColor(getColor(R.color.gray_divider));
        yAxisLeft.setTextColor(getColor(R.color.gray_text));

        lineChart.getAxisRight().setEnabled(false);
        lineChart.setData(new LineData(dataSet));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.animateX(500);
        lineChart.invalidate();
    }

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
        return e;
    }
    private String[] getMonthLabels() { return new String[]{"Jan","Feb","Mar","Apr"}; }
    private List<Entry> getYearData() {
        List<Entry> e = new ArrayList<>();
        e.add(new Entry(0, 60000)); e.add(new Entry(1, 72000));
        e.add(new Entry(2, 90160));
        return e;
    }
    private String[] getYearLabels() { return new String[]{"2022","2023","2024"}; }

    private void loadCards() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) return;

        repo.getCardsQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            cardList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Card card = doc.toObject(Card.class);
                card.setCardId(doc.getId());
                cardList.add(card);
            }
            cardAdapter.notifyDataSetChanged();
        });
    }

    private void loadSummaryData() {
        String uid = getCurrentUserId();
        if (uid == null || uid.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("transactions")
                .whereEqualTo("uid", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalSpending = 0, savings = 0, expenses = 0;
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Double amount = doc.getDouble("amount");
                        String type = doc.getString("type");
                        if (amount == null || type == null) continue;
                        if ("CREDIT".equals(type)) savings += amount;
                        else { expenses += amount; totalSpending += amount; }
                    }
                    tvTotalSpendingAmount.setText(String.format("Rs. %,.2f", totalSpending));
                    tvSavingsAmount.setText(String.format("Rs. %,.2f", savings));
                    tvExpensesAmount.setText(String.format("Rs. %,.2f", expenses));
                });
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.nav_statistic);
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                return true;
            } else if (id == R.id.nav_statistic) {
                return true;
            } else if (id == R.id.nav_budgets) {
                startActivity(new Intent(this, BudgetsActivity.class));
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                return true;
            }
            return false;
        });
    }

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return sessionManager.getUserId();
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }
}
