package com.worldbank.app.activities.payments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.worldbank.app.R;
import com.worldbank.app.adapters.BillerAdapter;
import com.worldbank.app.adapters.QuickPayAdapter;
import com.worldbank.app.adapters.TransactionAdapter;
import com.worldbank.app.models.Biller;
import com.worldbank.app.models.Contact;
import com.worldbank.app.models.Transaction;
import com.worldbank.app.utils.SessionManager;
import com.worldbank.app.utils.TransactionRepository;

import java.util.ArrayList;
import java.util.List;
import com.worldbank.app.activities.account.AccountActivity;
import com.worldbank.app.activities.budget.BudgetsActivity;
import com.worldbank.app.activities.cards.CardDetailActivity;
import com.worldbank.app.activities.cards.CardStatisticActivity;
import com.worldbank.app.activities.payments.AddPayeeActivity;
import com.worldbank.app.activities.transaction.SendMoneyActivity;
import com.worldbank.app.activities.transaction.ReviewPaymentActivity;
public class PaymentsActivity extends AppCompatActivity implements QuickPayAdapter.OnContactClickListener, BillerAdapter.OnBillerClickListener {

    private RecyclerView rvQuickPay, rvScheduled;
    private TextView tvScheduledLabel, tvQuickPayLabel;
    private EditText etSearchPayees;
    private View quickPayContainer;
    
    private QuickPayAdapter quickPayAdapter;
    private TransactionAdapter scheduledAdapter;
    private BillerAdapter billerAdapter;

    private List<Contact> contactList = new ArrayList<>();
    private List<Transaction> scheduledList = new ArrayList<>();
    private List<Biller> billerList = new ArrayList<>();
    private List<Biller> filteredBillers = new ArrayList<>();

    private TransactionRepository repo;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    private String accountId, cardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payments);

        accountId = getIntent().getStringExtra("accountId");
        cardId    = getIntent().getStringExtra("cardId");

        repo = new TransactionRepository();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        setupBillers();
        setupRecyclers();
        loadRealData();
        setupSearch();
        setupBottomNav();
    }

    private void bindViews() {
        rvQuickPay = findViewById(R.id.rvQuickPay);
        rvScheduled = findViewById(R.id.rvScheduled);
        tvScheduledLabel = findViewById(R.id.tvScheduledLabel);
        tvQuickPayLabel = findViewById(R.id.tvQuickPayLabel);
        etSearchPayees = findViewById(R.id.etSearchPayees);
        quickPayContainer = findViewById(R.id.rvQuickPay);
        
        ImageButton ibBack = findViewById(R.id.ibBack);
        if (ibBack != null) ibBack.setOnClickListener(v -> finish());

        Button btnAddNewPayee = findViewById(R.id.btnAddNewPayee);
        if (btnAddNewPayee != null) {
            btnAddNewPayee.setOnClickListener(v -> startActivity(new Intent(this, AddPayeeActivity.class)));
        }
    }

    private void setupBillers() {
        // Common Pakistani Billers for immediate selection
        billerList.add(new Biller("L01", "LESCO", "Electricity Bill", android.R.drawable.ic_menu_agenda));
        billerList.add(new Biller("K01", "K-Electric", "Electricity Bill", android.R.drawable.ic_menu_agenda));
        billerList.add(new Biller("S01", "SNGPL", "Gas Bill", android.R.drawable.ic_menu_agenda));
        billerList.add(new Biller("P01", "PTCL", "Internet & Phone", android.R.drawable.ic_menu_agenda));
        billerList.add(new Biller("ST01", "StormFiber", "Internet Bill", android.R.drawable.ic_menu_agenda));
        billerList.add(new Biller("N01", "Netflix", "Subscription", android.R.drawable.ic_menu_agenda));
        filteredBillers.addAll(billerList);
    }

    private void setupRecyclers() {
        quickPayAdapter = new QuickPayAdapter(contactList, this);
        rvQuickPay.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvQuickPay.setAdapter(quickPayAdapter);

        scheduledAdapter = new TransactionAdapter(this, scheduledList);
        billerAdapter = new BillerAdapter(filteredBillers, this);
        
        rvScheduled.setLayoutManager(new LinearLayoutManager(this));
        rvScheduled.setAdapter(scheduledAdapter); // Default shows history
    }

    private void setupSearch() {
        // Enter search mode when user taps the bar
        etSearchPayees.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                setSearchMode(true);
                showBillerList(etSearchPayees.getText().toString());
            }
        });

        etSearchPayees.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                showBillerList(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setSearchMode(boolean isSearching) {
        if (isSearching) {
            tvQuickPayLabel.setVisibility(View.GONE);
            rvQuickPay.setVisibility(View.GONE);
            tvScheduledLabel.setText("Select a Biller");
        } else {
            tvQuickPayLabel.setVisibility(View.VISIBLE);
            rvQuickPay.setVisibility(View.VISIBLE);
            tvScheduledLabel.setText("Scheduled Payments");
            rvScheduled.setAdapter(scheduledAdapter);
        }
    }

    private void showBillerList(String query) {
        filteredBillers.clear();
        if (query.isEmpty()) {
            filteredBillers.addAll(billerList);
        } else {
            for (Biller b : billerList) {
                if (b.getName().toLowerCase().contains(query.toLowerCase())) filteredBillers.add(b);
            }
        }
        rvScheduled.setAdapter(billerAdapter);
        tvScheduledLabel.setText(query.isEmpty() ? "Select a Biller" : "Search Results (" + filteredBillers.size() + ")");
        billerAdapter.notifyDataSetChanged();
    }

    private void loadRealData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;

        repo.getContactsQuery(uid).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            contactList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Contact c = doc.toObject(Contact.class);
                c.setContactId(doc.getId());
                contactList.add(c);
            }
            quickPayAdapter.notifyDataSetChanged();
        });

        repo.getTransactionsQuery(uid, 20).addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            scheduledList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Transaction t = doc.toObject(Transaction.class);
                if (Transaction.CAT_BILL.equals(t.getCategory())) scheduledList.add(t);
            }
            if (rvScheduled.getAdapter() == scheduledAdapter) scheduledAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onContactClick(Contact contact) {
        Intent intent = new Intent(this, SendMoneyActivity.class);
        intent.putExtra("recipientName", contact.getName());
        intent.putExtra("recipientAccount", contact.getAccountNumber());
        intent.putExtra("recipientBank", contact.getBankName());
        intent.putExtra("recipientUid", contact.getRecipientUid());
        intent.putExtra("accountId", accountId);
        intent.putExtra("cardId", cardId);
        startActivity(intent);
    }

    @Override
    public void onBillerClick(Biller biller) {
        showConsumerIdDialog(biller);
    }

    private void showConsumerIdDialog(Biller biller) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_consumer_id);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvTitle = dialog.findViewById(R.id.tvDialogTitle);
        EditText etConsumerId = dialog.findViewById(R.id.etConsumerId);
        Button btnContinue = dialog.findViewById(R.id.btnContinuePayment);
        Button btnCancel = dialog.findViewById(R.id.btnCancelDialog);

        tvTitle.setText("Pay " + biller.getName());
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnContinue.setOnClickListener(v -> {
            String cid = etConsumerId.getText().toString().trim();
            if (cid.isEmpty()) { Toast.makeText(this, "Enter Consumer ID", Toast.LENGTH_SHORT).show(); return; }
            
            // Simulating bill fetch from server
            double simulatedBillAmount = 500 + (Math.random() * 4500);
            
            Intent intent = new Intent(this, ReviewPaymentActivity.class);
            intent.putExtra("recipientName", biller.getName());
            intent.putExtra("recipientAccount", "Ref: " + cid);
            intent.putExtra("recipientBank", biller.getCategory());
            intent.putExtra("amount", simulatedBillAmount);
            intent.putExtra("fee", 0.0);
            intent.putExtra("transferType", Transaction.CAT_BILL);
            intent.putExtra("accountId", accountId);
            intent.putExtra("cardId", cardId);
            dialog.dismiss();
            startActivity(intent);
        });
        dialog.show();
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottomNavView);
        nav.setSelectedItemId(R.id.nav_home);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                finish();
                return true;
            } else if (id == R.id.nav_statistic) {
                startActivity(new Intent(this, CardStatisticActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_budgets) {
                startActivity(new Intent(this, BudgetsActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(this, AccountActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        if (rvScheduled.getAdapter() == billerAdapter) {
            setSearchMode(false);
            etSearchPayees.clearFocus();
        } else {
            super.onBackPressed();
        }
    }
}
