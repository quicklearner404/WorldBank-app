package com.worldbank.app.utils;

import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.worldbank.app.models.Account;
import com.worldbank.app.models.Contact;
import com.worldbank.app.models.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * TransactionRepository.java
 * ───────────────────────────
 * Robust Firestore banking operations.
 * Reverted to stable multi-argument logic to prevent Channel Shutdowns.
 */
public class TransactionRepository {

    private static final String TAG = "TransactionRepo";
    private static final String COL_USERS        = "users";
    private static final String COL_ACCOUNTS     = "accounts";
    private static final String COL_CARDS        = "cards";
    private static final String COL_TRANSACTIONS = "transactions";
    private static final String COL_CONTACTS     = "contacts";

    private final FirebaseFirestore db;

    public TransactionRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public Query getAccountQuery(String uid) {
        return db.collection(COL_ACCOUNTS).whereEqualTo("uid", uid).limit(1);
    }

    public Task<DocumentSnapshot> getAccount(String accountId) {
        if (accountId == null || accountId.isEmpty()) {
            return Tasks.forException(new Exception("Invalid Account ID"));
        }
        return db.collection(COL_ACCOUNTS).document(accountId).get();
    }

    public Query getCardsQuery(String uid) {
        return db.collection(COL_CARDS).whereEqualTo("uid", uid);
    }

    public Task<DocumentSnapshot> getCard(String cardId) {
        if (cardId == null || cardId.isEmpty()) {
            return Tasks.forException(new Exception("Invalid Card ID"));
        }
        return db.collection(COL_CARDS).document(cardId).get();
    }

    public Task<Void> addCard(Map<String, Object> cardMap) {
        return db.collection(COL_CARDS).document().set(cardMap);
    }

    public Task<QuerySnapshot> findAccountByIban(String iban) {
        return db.collection(COL_ACCOUNTS).whereEqualTo("accountNumber", iban).limit(1).get();
    }

    public Task<String> topUp(String uid, String accountId, double amount) {
        String referenceNumber = Transaction.generateReference();
        DocumentReference accountRef = db.collection(COL_ACCOUNTS).document(accountId);
        DocumentReference txnRef = db.collection(COL_TRANSACTIONS).document();

        return db.runTransaction(transaction -> {
            DocumentSnapshot accountSnap = transaction.get(accountRef);
            double currentBalance = accountSnap.getDouble("balance") != null ? accountSnap.getDouble("balance") : 0;
            transaction.update(accountRef, "balance", currentBalance + amount);

            // FIX: Use your Transaction Model Class instead of a HashMap!
            Transaction t = new Transaction();
            t.setUid(uid);
            t.setSenderUid(uid); // uid and senderUid are the same for top-up
            t.setAmount(amount);
            t.setType(Transaction.TYPE_CREDIT);
            t.setCategory(Transaction.CAT_TOPUP);
            t.setReferenceNumber(referenceNumber);
            t.setTimestamp(Timestamp.now());
            t.setRecipientName("Self Top-Up");
            t.setStatus(Transaction.STATUS_SUCCESS);

            // Firebase will automatically save the Transaction object!
            transaction.set(txnRef, t);
            return referenceNumber;
        });
    }

    /**
     * SEND MONEY: Reliable multi-argument atomic transfer.
     */

    public Task<String> sendMoney(
            String senderUid, String senderAccountId, String senderCardId,
            String recipientUid, String recipientAccountId, String recipientAccount,
            String recipientName, String recipientBank, double amount,
            String transferType, String description, String contactId) {

        Log.d(TAG, "sendMoney() invoked. SenderAcc: " + senderAccountId + ", Recipient: " + recipientName);

        if (senderAccountId == null || senderAccountId.isEmpty()) {
            return Tasks.forException(new Exception("Sender account ID missing"));
        }

        double adminFee = getAdminFee(transferType);
        double totalDeducted = amount + adminFee;
        String referenceNumber = Transaction.generateReference();

        boolean isInternal = recipientUid != null && !recipientUid.isEmpty() &&
                recipientAccountId != null && !recipientAccountId.isEmpty();

        DocumentReference senderAccountRef = db.collection(COL_ACCOUNTS).document(senderAccountId);
        DocumentReference senderTxnRef     = db.collection(COL_TRANSACTIONS).document();

        // Note: Using 'dbTransaction' instead of 'transaction' to avoid confusing
        // Java with your 'Transaction' model class!
        return db.runTransaction(dbTransaction -> {

            // ==========================================
            // 1. DO ALL READS FIRST (Firestore Rule)
            // ==========================================
            DocumentSnapshot senderAccountSnap = dbTransaction.get(senderAccountRef);
            if (!senderAccountSnap.exists()) {
                throw new RuntimeException("Sender account record not found");
            }

            DocumentSnapshot recAccSnap = null;
            DocumentReference recAccRef = null;

            // Read the recipient's account BEFORE making any writes
            if (isInternal) {
                recAccRef = db.collection(COL_ACCOUNTS).document(recipientAccountId);
                recAccSnap = dbTransaction.get(recAccRef);
            }

            // ==========================================
            // 2. VALIDATE CALCULATIONS
            // ==========================================
            double senderBalance = senderAccountSnap.getDouble("balance") != null ? senderAccountSnap.getDouble("balance") : 0;

            if (senderBalance < totalDeducted) {
                throw new RuntimeException("Insufficient balance");
            }

            // ==========================================
            // 3. DO ALL WRITES NOW
            // ==========================================

            // A. Deduct balance from Sender
            dbTransaction.update(senderAccountRef, "balance", senderBalance - totalDeducted);

            // B. Record Sender Debit (Using the clean Model Class!)
            Transaction debit = new Transaction();
            debit.setUid(senderUid);
            debit.setSenderUid(senderUid);
            debit.setRecipientName(recipientName);
            debit.setRecipientAccount(recipientAccount);
            debit.setAmount(amount);
            debit.setAdminFee(adminFee);
            debit.setTotalDeducted(totalDeducted);
            debit.setType(Transaction.TYPE_DEBIT);
            debit.setCategory(Transaction.CAT_TRANSFER);
            debit.setStatus(Transaction.STATUS_SUCCESS);
            debit.setReferenceNumber(referenceNumber);
            debit.setTimestamp(Timestamp.now());

            dbTransaction.set(senderTxnRef, debit);

            // C. If internal transfer, credit the recipient
            if (isInternal && recAccSnap != null && recAccSnap.exists()) {
                double recBalance = recAccSnap.getDouble("balance") != null ? recAccSnap.getDouble("balance") : 0;

                // Add balance to recipient
                dbTransaction.update(recAccRef, "balance", recBalance + amount);

                DocumentReference recTxnRef = db.collection(COL_TRANSACTIONS).document();

                // Record Recipient Credit (Using the clean Model Class!)
                Transaction credit = new Transaction();
                credit.setUid(recipientUid);
                credit.setSenderUid(senderUid);
                credit.setType(Transaction.TYPE_CREDIT);
                credit.setAmount(amount);
                credit.setCategory(Transaction.CAT_TRANSFER);
                credit.setTimestamp(Timestamp.now());
                credit.setRecipientName(senderAccountSnap.getString("accountTitle"));

                dbTransaction.set(recTxnRef, credit);
            }

            return referenceNumber;
        });
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

    public Task<DocumentReference> saveContact(Contact contact) {
        contact.setLastUsed(Timestamp.now());
        return db.collection(COL_CONTACTS).add(contact);
    }

    public static double getAdminFee(String transferType) {
        if (transferType == null) return 25.0;
        switch (transferType) {
            case Transaction.TRANSFER_INTERNAL: return 0.0;
            case Transaction.TRANSFER_JAZZCASH:
            case Transaction.TRANSFER_EASYPAISA: return 0.0;
            default: return 25.0;
        }
    }
}
