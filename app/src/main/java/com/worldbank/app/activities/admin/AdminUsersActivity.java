package com.worldbank.app.activities.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.worldbank.app.R;
import com.worldbank.app.activities.home.HomeActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminUsersActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private TextView tvEmpty;
    private BottomNavigationView bottomNavView;

    private FirebaseFirestore db;
    private final List<DocumentSnapshot> userList = new ArrayList<>();
    private UsersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        db = FirebaseFirestore.getInstance();

        rvUsers       = findViewById(R.id.rvUsers);
        tvEmpty       = findViewById(R.id.tvEmpty);
        bottomNavView = findViewById(R.id.adminBottomNavView);

        adapter = new UsersAdapter(userList);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        setupBottomNav();
        loadUsers();
    }

    private void loadUsers() {
        db.collection("users")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    userList.clear();
                    userList.addAll(snap.getDocuments());
                    adapter.notifyDataSetChanged();

                    tvEmpty.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvUsers.setVisibility(userList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void setupBottomNav() {
        bottomNavView.setSelectedItemId(R.id.admin_nav_users);
        bottomNavView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.admin_nav_dashboard) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.admin_nav_users) {
                return true;
            } else if (id == R.id.admin_nav_transactions) {
                startActivity(new Intent(this, AdminTransactionsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.admin_nav_exit) {
                showExitDialog();
                return false;
            }
            return false;
        });
    }

    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Exit Admin View")
                .setMessage("Are you sure you want to return to the user view?")
                .setPositiveButton("Exit", (dialog, which) -> switchToUserView())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void switchToUserView() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // back press blocked inside admin view
    }

    // adapter unchanged below this line
    private static class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

        private final List<DocumentSnapshot> list;

        UsersAdapter(List<DocumentSnapshot> list) {
            this.list = list;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_admin_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            DocumentSnapshot doc = list.get(position);

            String name  = doc.getString("displayName");
            String email = doc.getString("email");
            String cnic  = doc.getString("cnic");
            String phone = doc.getString("phone");

            if (name == null)  name  = "Unknown";
            if (email == null) email = "";
            if (cnic == null)  cnic  = "";
            if (phone == null) phone = "";

            holder.tvName.setText(name);
            holder.tvEmail.setText(email);
            holder.tvCnic.setText(cnic);
            holder.tvPhone.setText(phone);
            holder.tvInitials.setText(getInitials(name));

            com.google.firebase.Timestamp ts = doc.getTimestamp("createdAt");
            if (ts != null) {
                String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        .format(ts.toDate());
                holder.tvJoined.setText(formatted);
            } else {
                holder.tvJoined.setText("");
            }
        }

        @Override
        public int getItemCount() { return list.size(); }

        private String getInitials(String name) {
            if (name == null || name.trim().isEmpty()) return "?";
            String[] parts = name.trim().split(" ");
            if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvInitials, tvName, tvEmail, tvCnic, tvPhone, tvJoined;

            VH(View v) {
                super(v);
                tvInitials = v.findViewById(R.id.tvInitials);
                tvName     = v.findViewById(R.id.tvUserName);
                tvEmail    = v.findViewById(R.id.tvUserEmail);
                tvCnic     = v.findViewById(R.id.tvUserCnic);
                tvPhone    = v.findViewById(R.id.tvUserPhone);
                tvJoined   = v.findViewById(R.id.tvUserJoined);
            }
        }
    }
}