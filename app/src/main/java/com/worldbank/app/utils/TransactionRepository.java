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

    // tops up the sender account balance from an external source
    public Task<String> topUp(String uid, String accountId, double amount) {
        String referenceNumber = Transaction.generateReference();
        DocumentReference accountRef = db.collection(COL_ACCOUNTS).document(accountId);
        DocumentReference txnRef = db.collection(COL_TRANSACTIONS).document();

        return db.runTransaction(firestoreTransaction -> {
            // all reads first
            DocumentSnapshot accountSnap = firestoreTransaction.get(accountRef);

            double currentBalance = accountSnap.getDouble("balance") != null
                    ? accountSnap.getDouble("balance") : 0;

            // writes after all reads
            firestoreTransaction.update(accountRef, "balance", currentBalance + amount);

            Transaction txn = Transaction.createCredit(
                    uid,
                    accountSnap.getString("accountNumber"),
                    uid,
                    accountSnap.getString("accountNumber"),
                    "Self Top-Up",
                    "External Source",
                    amount,
                    Transaction.TRANSFER_INTERNAL,
                    "Top-Up"
            );
            txn.setTxnId(txnRef.getId());
            txn.setStatus(Transaction.STATUS_SUCCESS);
            txn.setReferenceNumber(referenceNumber);
            firestoreTransaction.set(txnRef, txn);

            return referenceNumber;
        });
    }

    public Task<String> sendMoney(
            String senderUid, String senderAccountId, String senderCardId,
            String recipientUid, String recipientAccountId, String recipientAccount,
            String recipientName, String recipientBank, double amount,
            String transferType, String description, String contactId) {

        double adminFee      = getAdminFee(transferType);
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
            // all reads first before any writes
            DocumentSnapshot senderAccountSnap = firestoreTransaction.get(senderAccountRef);
            DocumentSnapshot cardSnap          = firestoreTransaction.get(senderCardRef);

            DocumentSnapshot recipientAccountSnap = null;
            if (isInternal && recipientAccountRef != null) {
                recipientAccountSnap = firestoreTransaction.get(recipientAccountRef);
            }

            // validate balance before touching anything
            double currentBalance = senderAccountSnap.getDouble("balance") != null
                    ? senderAccountSnap.getDouble("balance") : 0;

            if (currentBalance < totalDeducted) {
                throw new RuntimeException("Insufficient balance");
            }

            double currentMonthlyUsed = cardSnap.getDouble("monthlyUsed") != null
                    ? cardSnap.getDouble("monthlyUsed") : 0;

            // all writes after all reads
            firestoreTransaction.update(senderAccountRef, "balance", currentBalance - totalDeducted);
            firestoreTransaction.update(senderCardRef, "monthlyUsed", currentMonthlyUsed + amount);

            Transaction senderTxn = Transaction.createTransfer(
                    senderUid,
                    senderAccountSnap.getString("accountNumber"),
                    recipientUid != null ? recipientUid : "",
                    recipientAccount,
                    recipientName,
                    recipientBank,
                    amount,
                    adminFee,
                    transferType,
                    description != null ? description : ""
            );
            senderTxn.setTxnId(newTxnRef.getId());
            senderTxn.setStatus(Transaction.STATUS_SUCCESS);
            senderTxn.setReferenceNumber(referenceNumber);
            firestoreTransaction.set(newTxnRef, senderTxn);

            // credit the recipient account for internal world bank transfers
            if (isInternal && recipientAccountRef != null && recipientAccountSnap != null) {
                double recipientBalance = recipientAccountSnap.getDouble("balance") != null
                        ? recipientAccountSnap.getDouble("balance") : 0;

                firestoreTransaction.update(recipientAccountRef, "balance", recipientBalance + amount);

                Transaction recipientTxn = Transaction.createCredit(
                        recipientUid,
                        recipientAccount,
                        senderUid,
                        senderAccountSnap.getString("accountNumber"),
                        senderAccountSnap.getString("accountTitle"),
                        "World Bank",
                        amount,
                        Transaction.TRANSFER_INTERNAL,
                        "Received from " + senderAccountSnap.getString("accountTitle")
                );
                recipientTxn.setTxnId(recipientTxnRef.getId());
                recipientTxn.setStatus(Transaction.STATUS_SUCCESS);
                recipientTxn.setReferenceNumber(referenceNumber);
                firestoreTransaction.set(recipientTxnRef, recipientTxn);
            }

            // update the last used timestamp on the contact if one was selected
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

        // account and card use maps here because they are batch writes to new documents
        // and the Account and Card models do not have a toMap method
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