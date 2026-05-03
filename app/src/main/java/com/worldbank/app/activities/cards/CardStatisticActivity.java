package com.worldbank.app.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
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
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class CardStatisticActivity extends AppCompatActivity implements CardAdapter.OnCardClickListener {

    private static final String TAG = "CardStatisticActivity";

    private TextView tvTotalSpendingAmount, tvSavingsAmount, tvExpensesAmount;
    private TextView tabDay, tabWeek, tabMonth, tabYear;
    private LineChart lineChart;
    private RecyclerView rvCards;
    private BottomNavigationView bottomNavView;

    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private TextView activeTab;
    private CardAdapter cardAdapter;
    private final List<Card> cardList = new ArrayList<>();

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
        setupTabs();
        loadCards();
        loadRealChartData("Day"); 
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
        activeTab             = tabDay;

        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        cardAdapter = new CardAdapter(this, cardList, this);
        rvCards.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvCards.setAdapter(cardAdapter);
    }

    @Override
    public void onCardClick(Card card, int position) {
        Toast.makeText(this, "Stats for: " + card.getMaskedNumber(), Toast.LENGTH_SHORT).show();
    }

    private void setupTabs() {
        tabDay.setOnClickListener(v -> { switchTabStyle(tabDay); loadRealChartData("Day"); });
        tabWeek.setOnClickListener(v -> { switchTabStyle(tabWeek); loadRealChartData("Week"); });
        tabMonth.setOnClickListener(v -> { switchTabStyle(tabMonth); loadRealChartData("Month"); });
        tabYear.setOnClickListener(v -> { switchTabStyle(tabYear); loadRealChartData("Year"); });
    }

    private void switchTabStyle(TextView selected) {
        activeTab.setBackgroundResource(0);
        activeTab.setTextColor(getColor(R.color.gray_text));
        selected.setBackgroundResource(R.drawable.bg_tab_active);
        selected.setTextColor(getColor(R.color.white));
        activeTab = selected;
    }

    private void loadRealChartData(String period) {
        String uid = getCurrentUserId();
        if (uid.isEmpty()) return;

        repo.getTransactionsQuery(uid, 50).get().addOnSuccessListener(snapshots -> {
            TreeMap<Long, Double> dataPoints = new TreeMap<>();
            
            for (QueryDocumentSnapshot doc : snapshots) {
                Transaction t = doc.toObject(Transaction.class);
                if (t.getTimestamp() != null && Transaction.TYPE_DEBIT.equals(t.getType())) {
                    long time = t.getTimestamp().toDate().getTime();
                    long key = normalizeTime(time, period);
                    dataPoints.put(key, dataPoints.getOrDefault(key, 0.0) + t.getAmount());
                }
            }

            List<Entry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            int i = 0;
            SimpleDateFormat sdf = getDateFormatForPeriod(period);

            for (Map.Entry<Long, Double> entry : dataPoints.entrySet()) {
                entries.add(new Entry(i++, entry.getValue().floatValue()));
                labels.add(sdf.format(new Date(entry.getKey())));
            }

            updateChartUI(entries, labels);
        });
    }

    private long normalizeTime(long time, String period) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        
        if (period.equals("Day")) {
            // Keep hour precision
        } else if (period.equals("Week") || period.equals("Month")) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
        } else {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
        }
        return cal.getTimeInMillis();
    }

    private SimpleDateFormat getDateFormatForPeriod(String period) {
        if (period.equals("Day")) return new SimpleDateFormat("EEE", Locale.getDefault());
        if (period.equals("Month")) return new SimpleDateFormat("MMM", Locale.getDefault());
        return new SimpleDateFormat("dd/MM", Locale.getDefault());
    }

    private void updateChartUI(List<Entry> entries, List<String> labels) {
        if (entries.isEmpty()) {
            lineChart.clear();
            lineChart.setNoDataText("No spending data for this period.");
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Spending");
        dataSet.setColor(getColor(R.color.purple_primary));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(getColor(R.color.purple_primary));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(getColor(R.color.purple_light));
        dataSet.setFillAlpha(50);
        dataSet.setDrawValues(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisRight().setEnabled(false);
        lineChart.getAxisLeft().setGridColor(getColor(R.color.gray_divider));
        lineChart.setData(new LineData(dataSet));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.animateX(800);
        lineChart.invalidate();
    }

    private void loadCards() {
        String uid = getCurrentUserId();
        if (uid.isEmpty()) return;
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
        if (uid.isEmpty()) return;

        FirebaseFirestore.getInstance().collection("transactions")
                .whereEqualTo("uid", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    double totalSpent = 0, savings = 0, expenses = 0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Double amount = doc.getDouble("amount");
                        String type = doc.getString("type");
                        if (amount == null || type == null) continue;
                        if (Transaction.TYPE_CREDIT.equals(type)) savings += amount;
                        else { expenses += amount; totalSpent += amount; }
                    }
                    tvTotalSpendingAmount.setText(String.format("Rs. %,.0f", totalSpent));
                    tvSavingsAmount.setText(String.format("Rs. %,.0f", savings));
                    tvExpensesAmount.setText(String.format("Rs. %,.0f", expenses));
                });
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.nav_statistic);
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, HomeActivity.class)); finish(); return true; }
            if (id == R.id.nav_budgets) { startActivity(new Intent(this, BudgetsActivity.class)); finish(); return true; }
            if (id == R.id.nav_account) { startActivity(new Intent(this, AccountActivity.class)); finish(); return true; }
            return id == R.id.nav_statistic;
        });
    }

    private String getCurrentUserId() {
        if (SessionManager.DEV_BYPASS) return "dev_user_001";
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        return "";
    }
}
