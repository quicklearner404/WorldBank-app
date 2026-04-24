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
 * FirestoreSeeder.java
 * ────────────────────
 * Utility class to quickly seed the database with initial Pakistani banking data
 * for the developer user (dev_user_001).
 *
 * This class now handles seeding for:
 * 1. Users
 * 2. Accounts
 * 3. Cards
 * 4. Transactions
 * 5. Contacts
 */
public class FirestoreSeeder {

    private static final String TAG = "FirestoreSeeder";

    public static void seedDevData(Context context) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String devUid = "dev_user_001";

        // ── 1. Create dev_user_001 Profile ────────────────────────
        Map<String, Object> user = new HashMap<>();
        user.put("uid", devUid);
        user.put("displayName", "Ali Hassan");
        user.put("email", "dev@worldbank.com");
        user.put("phone", "+92 300 1234567");
        user.put("city", "Lahore");
        user.put("cnic", "35202-XXXXXXX-X");
        user.put("createdAt", Timestamp.now());

        db.collection("users").document(devUid).set(user);

        // ── 2. Create dev_user_001 Account ────────────────────────
        Map<String, Object> account = new HashMap<>();
        account.put("uid", devUid);
        account.put("accountNumber", "PK36SCBL0000001123456702");
        account.put("accountTitle", "Ali Hassan");
        account.put("bankName", "World Bank");
        account.put("accountType", "SAVINGS");
        account.put("balance", 125000.0);
        account.put("currency", "PKR");
        account.put("isActive", true);

        db.collection("accounts").document("dev_account_001")
                .set(account)
                .addOnSuccessListener(aVoid -> {
                    // ── 3. Create dev_user_001 Card ───────────────────────
                    Map<String, Object> card = new HashMap<>();
                    card.put("uid", devUid);
                    card.put("accountId", "dev_account_001");
                    card.put("maskedNumber", "4532 **** **** 3090");
                    card.put("holderName", "Ali Hassan");
                    card.put("expiry", "09/26");
                    card.put("cardType", "VISA");
                    card.put("isActive", true);
                    card.put("monthlyLimit", 200000.0);
                    card.put("monthlyUsed", 45000.0);

                    db.collection("cards").document("dev_card_001").set(card);

                    // ── 4. Add Some Test Transactions ──────────────────────
                    addTestTransaction(db, devUid, "Fatima Khan", 15000, Transaction.TYPE_DEBIT, Transaction.CAT_TRANSFER, "PK36HBLN0000001234567802", "HBL");
                    addTestTransaction(db, devUid, "Lesco Bill", 4500, Transaction.TYPE_DEBIT, Transaction.CAT_BILL, "LESCO-123", "LESCO");
                    addTestTransaction(db, devUid, "Salary", 125000, Transaction.TYPE_CREDIT, Transaction.CAT_SALARY, "PK36SCBL0000001123456702", "World Bank");

                    // ── 5. Add Some Contacts ──────────────────────────────
                    addContact(db, devUid, "Fatima Khan", "PK36HBLN0000001234567802", "HBL");
                    addContact(db, devUid, "Ali Hassan", "PK36MEZN0000009876543210", "Meezan");

                    Toast.makeText(context, "Full Banking Data Seeded!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error seeding data", e));
    }

    private static void addTestTransaction(FirebaseFirestore db, String uid, String name, double amt, String type, String cat, String acc, String bank) {
        Map<String, Object> txn = new HashMap<>();
        txn.put("uid", uid);
        txn.put("senderUid", uid);
        txn.put("recipientName", name);
        txn.put("recipientAccount", acc);
        txn.put("recipientBank", bank);
        txn.put("amount", amt);
        txn.put("type", type);
        txn.put("category", cat);
        txn.put("status", "SUCCESS");
        txn.put("timestamp", Timestamp.now());
        txn.put("referenceNumber", Transaction.generateReference());
        db.collection("transactions").add(txn);
    }

    private static void addContact(FirebaseFirestore db, String ownerUid, String name, String acc, String bank) {
        Map<String, Object> contact = new HashMap<>();
        contact.put("ownerUid", ownerUid);
        contact.put("name", name);
        contact.put("accountNumber", acc);
        contact.put("bankName", bank);
        contact.put("lastUsed", Timestamp.now());
        db.collection("contacts").add(contact);
    }
}
