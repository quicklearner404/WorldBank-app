package com.worldbank.app.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.worldbank.app.models.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * FirestoreSeeder.java — FINAL FIX
 */
public class FirestoreSeeder {

    private static final String TAG = "FirestoreSeeder";

    public static void seedDevData(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String devUid = "dev_user_001";

        // 1. Clear existing dev documents to avoid duplicates or conflicts
        db.collection("accounts").document("dev_account_001").delete();
        db.collection("cards").document("dev_card_001").delete();
        db.collection("cards").document("dev_card_002").delete();

        // ── Create Account ────────────────────────
        Map<String, Object> account = new HashMap<>();
        account.put("uid", devUid);
        account.put("accountNumber", "PK36SCBL0000001123456702");
        account.put("accountTitle", "Emmie Watson");
        account.put("bankName", "World Bank");
        account.put("accountType", "SAVINGS");
        account.put("balance", 125000.0);
        account.put("currency", "PKR");
        account.put("isActive", true);

        db.collection("accounts").document("dev_account_001").set(account);

        // ── Create MULTIPLE Cards ──────────────────────────
        
        // Card 1
        Map<String, Object> card1 = new HashMap<>();
        card1.put("uid", devUid);
        card1.put("accountId", "dev_account_001");
        card1.put("maskedNumber", "**** **** **** 3090");
        card1.put("holderName", "Emmie Watson");
        card1.put("expiry", "09/26");
        card1.put("cardType", "VISA");
        card1.put("isActive", true);
        card1.put("balance", 125000.0);
        card1.put("monthlyLimit", 200000.0);
        card1.put("monthlyUsed", 45000.0);
        db.collection("cards").document("dev_card_001").set(card1);

        // Card 2
        Map<String, Object> card2 = new HashMap<>();
        card2.put("uid", devUid);
        card2.put("accountId", "dev_account_001");
        card2.put("maskedNumber", "**** **** **** 8844");
        card2.put("holderName", "Emmie Watson");
        card2.put("expiry", "12/25");
        card2.put("cardType", "MASTERCARD");
        card2.put("isActive", true);
        card2.put("balance", 0.0);
        card2.put("monthlyLimit", 100000.0);
        card2.put("monthlyUsed", 0.0);
        db.collection("cards").document("dev_card_002").set(card2)
            .addOnSuccessListener(v -> {
                Toast.makeText(context, "Cards Seeded! Scroll to see them.", Toast.LENGTH_SHORT).show();
            });
    }
}
