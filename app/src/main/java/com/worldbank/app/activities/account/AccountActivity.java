package com.worldbank.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;


public class AccountActivity extends AppCompatActivity {

    private static final String TAG = "AccountActivity";

    private TextView tvUserName, tvUserLocation, tvUserInitials;
    private ImageButton ibBack;
    private View cvEditPhoto;
    private LinearLayout llYourAccount, llPayment, llHistoryActivities, llPrivacySecurity, llAboutUs, llLogout;
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
        loadRealUserData();
        setupClickListeners();
        setupBottomNav();
    }

    private void bindViews() {
        tvUserName          = findViewById(R.id.tvUserName);
        tvUserLocation      = findViewById(R.id.tvUserLocation);
        tvUserInitials      = findViewById(R.id.tvUserInitials);
        cvEditPhoto         = findViewById(R.id.cvEditPhoto);
        ibBack              = findViewById(R.id.ibBack);
        llYourAccount       = findViewById(R.id.llYourAccount);
        llPayment           = findViewById(R.id.llPayment);
        llHistoryActivities = findViewById(R.id.llHistoryActivities);
        llPrivacySecurity   = findViewById(R.id.llPrivacySecurity);
        llAboutUs           = findViewById(R.id.llAboutUs);
        llLogout            = findViewById(R.id.llLogout);
        bottomNavView       = findViewById(R.id.bottomNavView);
    }

    private void loadRealUserData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;

        db.collection("users").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("displayName");
                    String city = doc.getString("city");
                    
                    tvUserName.setText(name != null ? name : sessionManager.getUserName());
                    tvUserLocation.setText(city != null ? city : "Pakistan");
                    
                    if (name != null) {
                        tvUserInitials.setText(getInitials(name));
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error fetching profile", e);
                tvUserName.setText(sessionManager.getUserName());
            });
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }

    private void setupClickListeners() {
        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }

        // 1. Your Account (Profile Info)
        llYourAccount.setOnClickListener(v -> 
            startActivity(new Intent(this, UserInfoActivity.class)));

        // 2. Payment Settings (Cards & Methods)
        llPayment.setOnClickListener(v -> 
            startActivity(new Intent(this, PaymentSettingsActivity.class)));

        // 3. Transaction History (Renamed from History Activities)
        llHistoryActivities.setOnClickListener(v ->
                startActivity(new Intent(this, TransactionHistoryActivity.class)));

        // 4. Privacy & Security
        llPrivacySecurity.setOnClickListener(v -> 
            startActivity(new Intent(this, PrivacySecurityActivity.class)));

        // 5. About Us
        llAboutUs.setOnClickListener(v -> 
            startActivity(new Intent(this, AboutUsActivity.class)));

        // 6. Logout
        llLogout.setOnClickListener(v -> showLogoutDialog());
        
        if (cvEditPhoto != null) {
            cvEditPhoto.setOnClickListener(v ->
                    Toast.makeText(this, "Profile editing coming soon", Toast.LENGTH_SHORT).show());
        }
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to sign out?")
            .setPositiveButton("Logout", (dialog, which) -> {
                auth.signOut();
                sessionManager.clearSession();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Cancel", null)
            .show();
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
            }
            return id == R.id.nav_account;
        });
    }
}
