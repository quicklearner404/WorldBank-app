package com.worldbank.app.utils;

import com.google.android.gms.tasks.Task;
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
 * Central class for ALL Firestore banking operations.
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
        return db.collection(COL_ACCOUNTS).whereEqualTo("uid", uid).limit(1);
    }

    /** Load account by ID for balance checks */
    public Task<DocumentSnapshot> getAccount(String accountId) {
        return db.collection(COL_ACCOUNTS).document(accountId).get();
    }

    public Query getCardsQuery(String uid) {
        return db.collection(COL_CARDS).whereEqualTo("uid", uid);
    }

    public Task<DocumentSnapshot> getCard(String cardId) {
        return db.collection(COL_CARDS).document(cardId).get();
    }

    public Task<Void> addCard(Map<String, Object> cardMap) {
        return db.collection(COL_CARDS).document().set(cardMap);
    }

    /** Find a World Bank account by IBAN for Auto-Lookup */
    public Task<QuerySnapshot> findAccountByIban(String iban) {
        return db.collection(COL_ACCOUNTS)
                .whereEqualTo("accountNumber", iban)
                .limit(1)
                .get();
    }

    /**
     * TOP-UP: Safe Credit Transaction
     */
    public Task<String> topUp(String uid, String accountId, double amount) {
        String referenceNumber = Transaction.generateReference();
        DocumentReference accountRef = db.collection(COL_ACCOUNTS).document(accountId);
        DocumentReference txnRef = db.collection(COL_TRANSACTIONS).document();

        return db.runTransaction(transaction -> {
            DocumentSnapshot accountSnap = transaction.get(accountRef);
            double currentBalance = accountSnap.getDouble("balance") != null ? accountSnap.getDouble("balance") : 0;
            transaction.update(accountRef, "balance", currentBalance + amount);

            Transaction t = new Transaction();
            t.setUid(uid);
            t.setSenderUid(uid);
            t.setAmount(amount);
            t.setType(Transaction.TYPE_CREDIT);
            t.setCategory(Transaction.CAT_TOPUP);
            t.setReferenceNumber(referenceNumber);
            t.setTimestamp(Timestamp.now());
            t.setRecipientName("Self Top-Up");
            t.setStatus(Transaction.STATUS_SUCCESS);

            transaction.set(txnRef, t);
            return referenceNumber;
        });
    }

    /**
     * SEND MONEY: Atomic Transfer using a Transaction object.
     * Deducts from sender and credits recipient if internal.
     */
    public Task<String> sendMoney(Transaction txn, String senderAccountId) {
        DocumentReference senderAccountRef = db.collection(COL_ACCOUNTS).document(senderAccountId);
        DocumentReference senderTxnRef     = db.collection(COL_TRANSACTIONS).document();

        boolean isInternal = txn.getRecipientUid() != null && !txn.getRecipientUid().isEmpty();
        
        // Recipient refs (only used if internal)
        DocumentReference recipientAccountRef = isInternal
                ? db.collection(COL_ACCOUNTS).document(txn.getRecipientAccountId())
                : null;
        DocumentReference recipientTxnRef = isInternal
                ? db.collection(COL_TRANSACTIONS).document()
                : null;

        return db.runTransaction(transaction -> {
            // 1. Check Sender Balance
            DocumentSnapshot senderAccountSnap = transaction.get(senderAccountRef);
            double senderBalance = senderAccountSnap.getDouble("balance") != null ? senderAccountSnap.getDouble("balance") : 0;

            if (senderBalance < txn.getTotalDeducted()) {
                throw new RuntimeException("Insufficient balance");
            }

            // 2. Deduct from Sender
            transaction.update(senderAccountRef, "balance", senderBalance - txn.getTotalDeducted());

            // 3. Save Record
            String referenceNumber = Transaction.generateReference();
            txn.setTxnId(senderTxnRef.getId());
            txn.setTimestamp(Timestamp.now());
            txn.setStatus(Transaction.STATUS_SUCCESS);
            txn.setReferenceNumber(referenceNumber);
            transaction.set(senderTxnRef, txn);

            // 4. If Internal — Credit Recipient
            if (isInternal && recipientAccountRef != null) {
                DocumentSnapshot recipientSnap = transaction.get(recipientAccountRef);
                double recipientBalance = recipientSnap.getDouble("balance") != null ? recipientSnap.getDouble("balance") : 0;
                transaction.update(recipientAccountRef, "balance", recipientBalance + txn.getAmount());

                Transaction credit = new Transaction();
                credit.setUid(txn.getRecipientUid());
                credit.setSenderUid(txn.getSenderUid());
                credit.setRecipientUid(txn.getRecipientUid());
                credit.setRecipientAccount(txn.getRecipientAccount());
                credit.setRecipientName(senderAccountSnap.getString("accountTitle"));
                credit.setRecipientBank("World Bank");
                credit.setAmount(txn.getAmount());
                credit.setType(Transaction.TYPE_CREDIT);
                credit.setCategory(Transaction.CAT_TRANSFER);
                credit.setTransferType(Transaction.TRANSFER_INTERNAL);
                credit.setReferenceNumber(referenceNumber);
                credit.setStatus(Transaction.STATUS_SUCCESS);
                credit.setTimestamp(txn.getTimestamp());
                transaction.set(recipientTxnRef, credit);
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
