package com.worldbank.app.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.R;
import com.worldbank.app.utils.SessionManager;

public class UserInfoActivity extends AppCompatActivity {

    TextView tvInfoName, tvInfoEmail, tvInfoPhone, tvProfileInitials;
    TextInputEditText etLocation;
    Button btnSave;
    ImageButton ibBack;

    FirebaseFirestore db;
    FirebaseAuth auth;
    SessionManager sessionManager;

    String currentUid    = "";
    String originalCity  = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);

        db             = FirebaseFirestore.getInstance();
        auth           = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        bindViews();
        loadUserData();
    }

    private void bindViews() {
        tvInfoName        = findViewById(R.id.tvInfoName);
        tvInfoEmail       = findViewById(R.id.tvInfoEmail);
        tvInfoPhone       = findViewById(R.id.tvInfoPhone);
        tvProfileInitials = findViewById(R.id.tvProfileInitials);
        etLocation        = findViewById(R.id.etLocation);
        btnSave           = findViewById(R.id.btnSave);
        ibBack            = findViewById(R.id.ibBack);

        ibBack.setOnClickListener(v -> finish());

        // profile pic circle shows coming soon
        findViewById(R.id.flProfilePic).setOnClickListener(v ->
                Toast.makeText(this, "Profile photo — coming soon!",
                        Toast.LENGTH_SHORT).show());

        btnSave.setOnClickListener(v -> save());
    }

    private void loadUserData() {
        currentUid = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid()
                : sessionManager.getUserId();

        if (currentUid.isEmpty()) return;

        db.collection("users").document(currentUid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name  = doc.getString("displayName");
                    String email = doc.getString("email");
                    String phone = doc.getString("phone");
                    String city  = doc.getString("city");

                    tvInfoName.setText(name != null ? name : "");
                    tvInfoEmail.setText(email != null ? email : "");
                    tvInfoPhone.setText(phone != null ? phone : "");

                    // store original city so we can check if it changed on save
                    originalCity = city != null ? city : "";
                    etLocation.setText(originalCity);

                    tvProfileInitials.setText(getInitials(name));
                });
    }

    private void save() {
        String newLocation = etLocation.getText().toString().trim();

        // nothing changed, no need to hit firestore
        if (newLocation.equals(originalCity)) {
            Toast.makeText(this, "Nothing to save", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newLocation.isEmpty()) {
            etLocation.setError("Please enter your city");
            etLocation.requestFocus();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        db.collection("users").document(currentUid)
                .update("city", newLocation)
                .addOnSuccessListener(unused -> {
                    originalCity = newLocation;
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("Save");
                    Toast.makeText(this, "Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // builds two letter initials from full name
    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split(" ");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1)
                + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}