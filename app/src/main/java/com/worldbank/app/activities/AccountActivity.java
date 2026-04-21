package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;

public class AccountActivity extends AppCompatActivity {

    private TextView tvUserName, tvUserLocation;
    private ImageButton ibEditPhoto, ibBack;
    private LinearLayout llYourAccount, llPayment, llHistoryActivities, llPrivacySecurity, llAboutUs;
    private BottomNavigationView bottomNavView;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        db             = FirebaseFirestore.getInstance();
        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        loadUserData();
        setupClickListeners();
        setupBottomNav();
    }

    private void bindViews() {
        tvUserName          = findViewById(R.id.tvUserName);
        tvUserLocation      = findViewById(R.id.tvUserLocation);
        ibEditPhoto         = findViewById(R.id.ibEditPhoto);
        ibBack              = findViewById(R.id.ibBack);
        llYourAccount       = findViewById(R.id.llYourAccount);
        llPayment           = findViewById(R.id.llPayment);
        llHistoryActivities = findViewById(R.id.llHistoryActivities);
        llPrivacySecurity   = findViewById(R.id.llPrivacySecurity);
        llAboutUs           = findViewById(R.id.llAboutUs);
        bottomNavView       = findViewById(R.id.bottomNavView);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }
    }

    private void loadUserData() {
        // Use session data — works with both DEV_BYPASS and real login
        tvUserName.setText(sessionManager.getUserName());

        // Load location from Firestore if not in dev mode
        if (!SessionManager.DEV_BYPASS && auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String location = doc.getString("location");
                            tvUserLocation.setText(location != null ? location : "");
                        }
                    });
        } else {
            tvUserLocation.setText("Lahore, Pakistan");
        }
    }

    private void setupClickListeners() {
        ibEditPhoto.setOnClickListener(v ->
                Toast.makeText(this, "Edit photo — coming soon", Toast.LENGTH_SHORT).show());

        llYourAccount.setOnClickListener(v ->
                Toast.makeText(this, "Your Account settings — coming soon", Toast.LENGTH_SHORT).show());

        llPayment.setOnClickListener(v ->
                Toast.makeText(this, "Payment settings — coming soon", Toast.LENGTH_SHORT).show());

        llHistoryActivities.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class)));

        llPrivacySecurity.setOnClickListener(v ->
                Toast.makeText(this, "Privacy & Security — coming soon", Toast.LENGTH_SHORT).show());

        llAboutUs.setOnClickListener(v ->
                Toast.makeText(this, "About Us — coming soon", Toast.LENGTH_SHORT).show());
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.nav_account);
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
            } else if (id == R.id.nav_budgets) {
                startActivity(new Intent(this, BudgetsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_account) {
                return true;
            }
            return false;
        });
    }
}
