package com.worldbank.app.activities;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;

public class UserInfoActivity extends AppCompatActivity {

    private TextView tvInfoName, tvInfoEmail, tvInfoPhone, tvInfoLocation;
    private ImageButton ibBack;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        loadUserData();
    }

    private void bindViews() {
        tvInfoName = findViewById(R.id.tvInfoName);
        tvInfoEmail = findViewById(R.id.tvInfoEmail);
        tvInfoPhone = findViewById(R.id.tvInfoPhone);
        tvInfoLocation = findViewById(R.id.tvInfoLocation);
        ibBack = findViewById(R.id.ibBack);

        if (ibBack != null) {
            ibBack.setOnClickListener(v -> finish());
        }
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : sessionManager.getUserId();
        if (uid == null || uid.isEmpty()) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvInfoName.setText(doc.getString("displayName"));
                tvInfoEmail.setText(doc.getString("email"));
                tvInfoPhone.setText(doc.getString("phone"));
                tvInfoLocation.setText(doc.getString("city"));
            }
        });
    }
}
