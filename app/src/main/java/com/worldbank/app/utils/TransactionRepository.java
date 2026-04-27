package com.worldbank.app.utils;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.worldbank.app.models.Account;
import com.worldbank.app.models.Contact;
import com.worldbank.app.models.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * TransactionRepository.java
 * ───────────────────────────
 * Central class for ALL Firestore read/write operations.
 */
public class TransactionRepository {

    private static final String COL_USERS        = "users";
    private static final String COL_ACCOUNTS     = "accounts";
    private static final String COL_CARDS        = "cards";
    private static final String COL_TRANSACTIONS = "transactions";
    private static final String COL_CONTACTS     = "contacts";

    public static final double ADMIN_FEE_INTERNAL = 0;
    public static final double ADMIN_FEE_IBFT     = 25;
    public static final double ADMIN_FEE_WALLET   = 0;

    private final FirebaseFirestore db;

    public TransactionRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Query getAccountQuery(String uid) {
        return db.collection(COL_ACCOUNTS)
                .whereEqualTo("uid", uid)
                .limit(1);
    }

    public Query getCardsQuery(String uid) {
        return db.collection(COL_CARDS)
                .whereEqualTo("uid", uid);
    }

    public Task<DocumentSnapshot> getCard(String cardId) {
        return db.collection(COL_CARDS).document(cardId).get();
    }

    public Task<Void> addCard(Map<String, Object> cardMap) {
        return db.collection(COL_CARDS).document().set(cardMap);
    }

    public Query getTransactionsQuery(String uid, int limit) {
        return db.collection(COL_TRANSACTIONS)
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit);
    }

    public Query getContactsQuery(String uid) {
        return db.collection(COL_CONTACTS)
                .whereEqualTo("ownerUid", uid)
                .orderBy("lastUsed", Query.Direction.DESCENDING)
                .limit(20);
    }

    /**
     * TOP-UP: Safe Credit Transaction
     * Adds money from an external source to the internal account.
     */
    public Task<String> topUp(String uid, String accountId, double amount) {
        String referenceNumber = Transaction.generateReference();
        DocumentReference accountRef = db.collection(COL_ACCOUNTS).document(accountId);
        DocumentReference txnRef = db.collection(COL_TRANSACTIONS).document();

        return db.runTransaction(transaction -> {
            DocumentSnapshot accountSnap = transaction.get(accountRef);
            double currentBalance = accountSnap.getDouble("balance") != null 
                ? accountSnap.getDouble("balance") : 0;
            
            // 1. Update Balance
            transaction.update(accountRef, "balance", currentBalance + amount);

            // 2. Log Transaction
            Map<String, Object> txn = new HashMap<>();
            txn.put("txnId",            txnRef.getId());
            txn.put("uid",              uid);
            txn.put("senderUid",        uid);
            txn.put("amount",           amount);
            txn.put("adminFee",         0.0);
            txn.put("totalDeducted",    0.0);
            txn.put("type",             Transaction.TYPE_CREDIT);
            txn.put("category",         Transaction.CAT_TOPUP);
            txn.put("status",           Transaction.STATUS_SUCCESS);
            txn.put("referenceNumber",  referenceNumber);
            txn.put("timestamp",        Timestamp.now());
            txn.put("recipientName",    "Self Top-Up");
            txn.put("recipientBank",    "External Source");
            
            transaction.set(txnRef, txn);
            
            return referenceNumber;
        });
    }

    public Task<String> sendMoney(
            String senderUid, String senderAccountId, String senderCardId,
            String recipientUid, String recipientAccountId, String recipientAccount,
            String recipientName, String recipientBank, double amount,
            String transferType, String description, String contactId) {

        double adminFee = getAdminFee(transferType);
        double totalDeducted = amount + adminFee;
        String referenceNumber = Transaction.generateReference();
        boolean isInternal = recipientUid != null && !recipientUid.isEmpty();

        DocumentReference senderAccountRef = db.collection(COL_ACCOUNTS).document(senderAccountId);
        DocumentReference senderCardRef    = db.collection(COL_CARDS).document(senderCardId);
        DocumentReference newTxnRef        = db.collection(COL_TRANSACTIONS).document();

        DocumentReference recipientAccountRef = isInternal && recipientAccountId != null
                ? db.collection(COL_ACCOUNTS).document(recipientAccountId)
                : null;
        DocumentReference recipientTxnRef = isInternal
                ? db.collection(COL_TRANSACTIONS).document()
                : null;

        return db.runTransaction(firestoreTransaction -> {
            DocumentSnapshot senderAccountSnap = firestoreTransaction.get(senderAccountRef);
            double currentBalance = senderAccountSnap.getDouble("balance") != null
                    ? senderAccountSnap.getDouble("balance") : 0;

            if (currentBalance < totalDeducted) {
                throw new RuntimeException("Insufficient balance");
            }

            firestoreTransaction.update(senderAccountRef, "balance", currentBalance - totalDeducted);

            DocumentSnapshot cardSnap = firestoreTransaction.get(senderCardRef);
            double currentMonthlyUsed = cardSnap.getDouble("monthlyUsed") != null
                    ? cardSnap.getDouble("monthlyUsed") : 0;
            firestoreTransaction.update(senderCardRef, "monthlyUsed", currentMonthlyUsed + amount);

            Map<String, Object> senderTxn = new HashMap<>();
            senderTxn.put("txnId",            newTxnRef.getId());
            senderTxn.put("uid",              senderUid);
            senderTxn.put("senderUid",        senderUid);
            senderTxn.put("senderAccount",    senderAccountSnap.getString("accountNumber"));
            senderTxn.put("recipientUid",     recipientUid != null ? recipientUid : "");
            senderTxn.put("recipientAccount", recipientAccount);
            senderTxn.put("recipientName",    recipientName);
            senderTxn.put("recipientBank",    recipientBank);
            senderTxn.put("amount",           amount);
            senderTxn.put("adminFee",         adminFee);
            senderTxn.put("totalDeducted",    totalDeducted);
            senderTxn.put("type",             Transaction.TYPE_DEBIT);
            senderTxn.put("category",         Transaction.CAT_TRANSFER);
            senderTxn.put("transferType",     transferType);
            senderTxn.put("referenceNumber",  referenceNumber);
            senderTxn.put("status",           Transaction.STATUS_SUCCESS);
            senderTxn.put("description",      description != null ? description : "");
            senderTxn.put("timestamp",        Timestamp.now());
            firestoreTransaction.set(newTxnRef, senderTxn);

            if (isInternal && recipientAccountRef != null) {
                DocumentSnapshot recipientAccountSnap = firestoreTransaction.get(recipientAccountRef);
                double recipientBalance = recipientAccountSnap.getDouble("balance") != null
                        ? recipientAccountSnap.getDouble("balance") : 0;

                firestoreTransaction.update(recipientAccountRef, "balance", recipientBalance + amount);

                Map<String, Object> recipientTxn = new HashMap<>();
                recipientTxn.put("uid",              recipientUid);
                recipientTxn.put("senderUid",        senderUid);
                recipientTxn.put("recipientUid",     recipientUid);
                recipientTxn.put("recipientAccount", recipientAccount);
                recipientTxn.put("recipientName",    senderAccountSnap.getString("accountTitle"));
                recipientTxn.put("recipientBank",    "World Bank");
                recipientTxn.put("amount",           amount);
                recipientTxn.put("adminFee",         0.0);
                recipientTxn.put("totalDeducted",    0.0);
                recipientTxn.put("type",             Transaction.TYPE_CREDIT);
                recipientTxn.put("category",         Transaction.CAT_TRANSFER);
                recipientTxn.put("transferType",     Transaction.TRANSFER_INTERNAL);
                recipientTxn.put("referenceNumber",  referenceNumber);
                recipientTxn.put("status",           Transaction.STATUS_SUCCESS);
                recipientTxn.put("description",      "Received from " + senderAccountSnap.getString("accountTitle"));
                recipientTxn.put("timestamp",        Timestamp.now());
                firestoreTransaction.set(recipientTxnRef, recipientTxn);
            }

            if (contactId != null && !contactId.isEmpty()) {
                DocumentReference contactRef = db.collection(COL_CONTACTS).document(contactId);
                firestoreTransaction.update(contactRef, "lastUsed", Timestamp.now());
            }

            return referenceNumber;
        });
    }

    public static double getAdminFee(String transferType) {
        if (transferType == null) return ADMIN_FEE_IBFT;
        switch (transferType) {
            case Transaction.TRANSFER_INTERNAL: return ADMIN_FEE_INTERNAL;
            case Transaction.TRANSFER_JAZZCASH:
            case Transaction.TRANSFER_EASYPAISA: return ADMIN_FEE_WALLET;
            default: return ADMIN_FEE_IBFT;
        }
    }

    public Task<Void> createNewUserData(String uid, String name, String email, String phone) {
        String iban = "PK36WBNK" + String.format("%016d", Math.abs(uid.hashCode() % 9999999999999999L));
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid", uid);
        userMap.put("displayName", name);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("city", "Pakistan");
        userMap.put("createdAt", Timestamp.now());

        Map<String, Object> accountMap = new HashMap<>();
        accountMap.put("uid", uid);
        accountMap.put("accountNumber", iban);
        accountMap.put("accountTitle", name);
        accountMap.put("bankName", "World Bank");
        accountMap.put("accountType", Account.TYPE_SAVINGS);
        accountMap.put("balance", 0.0);
        accountMap.put("currency", Account.CURRENCY_PKR);
        accountMap.put("isActive", true);

        DocumentReference userRef    = db.collection(COL_USERS).document(uid);
        DocumentReference accountRef = db.collection(COL_ACCOUNTS).document();

        return db.runBatch(batch -> {
            batch.set(userRef, userMap);
            batch.set(accountRef, accountMap);

            String maskedCard = "**** **** **** " + String.format("%04d", Math.abs(uid.hashCode() % 10000));
            Map<String, Object> cardMap = new HashMap<>();
            cardMap.put("uid", uid);
            cardMap.put("accountId", accountRef.getId());
            cardMap.put("maskedNumber", maskedCard);
            cardMap.put("holderName", name);
            cardMap.put("expiry", "12/28");
            cardMap.put("cardType", "VISA");
            cardMap.put("isActive", true);
            cardMap.put("monthlyLimit", 200000.0);
            cardMap.put("monthlyUsed", 0.0);

            DocumentReference cardRef = db.collection(COL_CARDS).document();
            batch.set(cardRef, cardMap);
        });
    }
}
