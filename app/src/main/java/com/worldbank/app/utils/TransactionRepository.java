package com.worldbank.app.utils;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.worldbank.app.models.Account;
import com.worldbank.app.models.Card;
import com.worldbank.app.models.Contact;
import com.worldbank.app.models.Transaction;

import java.util.HashMap;
import java.util.Map;

/**
 * TransactionRepository.java
 * ───────────────────────────
 * Central class for ALL Firestore read/write operations.
 * Activities should call this instead of writing Firestore code themselves.
 *
 * KEY PRINCIPLE — Atomic balance changes:
 *   We use db.runTransaction() for any operation that changes balances.
 *   This ensures that if two transfers happen simultaneously, both
 *   complete correctly and no money is lost or duplicated.
 *
 * USAGE in activities:
 *   TransactionRepository repo = new TransactionRepository();
 *   repo.sendMoney(...).addOnSuccessListener(...).addOnFailureListener(...);
 */
public class TransactionRepository {

    private static final String COL_USERS        = "users";
    private static final String COL_ACCOUNTS     = "accounts";
    private static final String COL_CARDS        = "cards";
    private static final String COL_TRANSACTIONS = "transactions";
    private static final String COL_CONTACTS     = "contacts";

    // Admin fee in PKR
    public static final double ADMIN_FEE_INTERNAL = 0;    // free within World Bank
    public static final double ADMIN_FEE_IBFT     = 25;   // PKR 25 for IBFT
    public static final double ADMIN_FEE_WALLET   = 0;    // free for JazzCash/EasyPaisa

    private final FirebaseFirestore db;

    public TransactionRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ══════════════════════════════════════════════════════════════
    //  ACCOUNTS
    // ══════════════════════════════════════════════════════════════

    /** Load primary account for a user (real-time listener version — use in HomeActivity) */
    public Query getAccountQuery(String uid) {
        return db.collection(COL_ACCOUNTS)
                .whereEqualTo("uid", uid)
                .whereEqualTo("isActive", true)
                .limit(1);
    }

    /** Load account by ID */
    public Task<DocumentSnapshot> getAccount(String accountId) {
        return db.collection(COL_ACCOUNTS).document(accountId).get();
    }

    // ══════════════════════════════════════════════════════════════
    //  CARDS
    // ══════════════════════════════════════════════════════════════

    /** Load all active cards for a user */
    public Query getCardsQuery(String uid) {
        return db.collection(COL_CARDS)
                .whereEqualTo("uid", uid)
                .whereEqualTo("isActive", true);
    }

    /** Load single card */
    public Task<DocumentSnapshot> getCard(String cardId) {
        return db.collection(COL_CARDS).document(cardId).get();
    }

    // ══════════════════════════════════════════════════════════════
    //  TRANSACTIONS
    // ══════════════════════════════════════════════════════════════

    /**
     * Returns a real-time query for a user's transactions.
     * Use with addSnapshotListener() in activities for live updates.
     *
     * @param uid   the user's Firebase UID
     * @param limit how many to load (10 for home, 50 for history)
     */
    public Query getTransactionsQuery(String uid, int limit) {
        return db.collection(COL_TRANSACTIONS)
                .whereEqualTo("uid", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit);
    }

    // ══════════════════════════════════════════════════════════════
    //  SEND MONEY — the core banking operation
    // ══════════════════════════════════════════════════════════════

    /**
     * Executes a money transfer atomically using Firestore Transaction.
     *
     * What happens atomically (all or nothing):
     *   1. Read sender's account — check balance is sufficient
     *   2. Deduct amount + fee from sender's balance
     *   3. Update sender's card monthlyUsed
     *   4. If recipient is internal (World Bank user): credit their account
     *   5. Write a DEBIT transaction document for the sender
     *   6. If internal: write a CREDIT transaction document for the recipient
     *   7. Update contact's lastUsed timestamp
     *
     * @param senderUid         Firebase UID of sender
     * @param senderAccountId   Firestore document ID of sender's account
     * @param senderCardId      Firestore document ID of sender's card
     * @param recipientUid      Firebase UID of recipient (null if external)
     * @param recipientAccountId Firestore doc ID of recipient's account (null if external)
     * @param recipientAccount  IBAN or mobile number (for display)
     * @param recipientName     Display name
     * @param recipientBank     Bank name string
     * @param amount            PKR amount to send
     * @param transferType      Transaction.TRANSFER_* constant
     * @param description       Optional note
     * @param contactId         If saving/updating a contact (can be null)
     * @return Task that succeeds with the new transaction's reference number
     */
    public Task<String> sendMoney(
            String senderUid,
            String senderAccountId,
            String senderCardId,
            String recipientUid,
            String recipientAccountId,
            String recipientAccount,
            String recipientName,
            String recipientBank,
            double amount,
            String transferType,
            String description,
            String contactId) {

        double adminFee = getAdminFee(transferType);
        double totalDeducted = amount + adminFee;
        String referenceNumber = Transaction.generateReference();
        boolean isInternal = recipientUid != null && !recipientUid.isEmpty();

        DocumentReference senderAccountRef = db.collection(COL_ACCOUNTS).document(senderAccountId);
        DocumentReference senderCardRef    = db.collection(COL_CARDS).document(senderCardId);
        DocumentReference newTxnRef        = db.collection(COL_TRANSACTIONS).document();

        // Recipient refs (only used if internal)
        DocumentReference recipientAccountRef = isInternal && recipientAccountId != null
                ? db.collection(COL_ACCOUNTS).document(recipientAccountId)
                : null;
        DocumentReference recipientTxnRef = isInternal
                ? db.collection(COL_TRANSACTIONS).document()
                : null;

        return db.runTransaction(firestoreTransaction -> {
            // ── Step 1: Read sender account ──────────────────────
            DocumentSnapshot senderAccountSnap = firestoreTransaction.get(senderAccountRef);
            double currentBalance = senderAccountSnap.getDouble("balance") != null
                    ? senderAccountSnap.getDouble("balance") : 0;

            // ── Step 2: Check sufficient balance ─────────────────
            if (currentBalance < totalDeducted) {
                try {
                    throw new Exception("Insufficient balance. Available: Rs. " +
                            String.format("%,.0f", currentBalance) +
                            ", Required: Rs. " + String.format("%,.0f", totalDeducted));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            // ── Step 3: Deduct from sender balance ────────────────
            firestoreTransaction.update(senderAccountRef, "balance",
                    currentBalance - totalDeducted);

            // ── Step 4: Update sender card's monthlyUsed ──────────
            DocumentSnapshot cardSnap = firestoreTransaction.get(senderCardRef);
            double currentMonthlyUsed = cardSnap.getDouble("monthlyUsed") != null
                    ? cardSnap.getDouble("monthlyUsed") : 0;
            firestoreTransaction.update(senderCardRef, "monthlyUsed",
                    currentMonthlyUsed + amount);

            // ── Step 5: Write sender DEBIT transaction ────────────
            Map<String, Object> senderTxn = new HashMap<>();
            senderTxn.put("txnId",            newTxnRef.getId());
            senderTxn.put("uid",              senderUid);       // legacy compat
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

            // ── Step 6: If internal — credit recipient ────────────
            if (isInternal && recipientAccountRef != null) {
                DocumentSnapshot recipientAccountSnap =
                        firestoreTransaction.get(recipientAccountRef);
                double recipientBalance = recipientAccountSnap.getDouble("balance") != null
                        ? recipientAccountSnap.getDouble("balance") : 0;

                // Add to recipient balance (no fee charged to recipient)
                firestoreTransaction.update(recipientAccountRef, "balance",
                        recipientBalance + amount);

                // Write recipient CREDIT transaction
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

            // ── Step 7: Update contact lastUsed if applicable ─────
            if (contactId != null && !contactId.isEmpty()) {
                DocumentReference contactRef = db.collection(COL_CONTACTS).document(contactId);
                firestoreTransaction.update(contactRef, "lastUsed", Timestamp.now());
            }

            return referenceNumber;
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  CONTACTS
    // ══════════════════════════════════════════════════════════════

    /** Get saved contacts for a user, sorted by most recently used */
    public Query getContactsQuery(String uid) {
        return db.collection(COL_CONTACTS)
                .whereEqualTo("ownerUid", uid)
                .orderBy("lastUsed", Query.Direction.DESCENDING)
                .limit(20);
    }

    /** Save a new contact */
    public Task<DocumentReference> saveContact(Contact contact) {
        Map<String, Object> data = new HashMap<>();
        data.put("ownerUid",      contact.getOwnerUid());
        data.put("name",          contact.getName());
        data.put("accountNumber", contact.getAccountNumber());
        data.put("bankName",      contact.getBankName());
        data.put("isFavorite",    contact.isFavorite());
        data.put("lastUsed",      Timestamp.now());
        return db.collection(COL_CONTACTS).add(data);
    }

    // ══════════════════════════════════════════════════════════════
    //  SETUP — Create data for new user after signup
    // ══════════════════════════════════════════════════════════════

    /**
     * Called by Dev 1's SignUpActivity after successful Firebase Auth registration.
     * Creates the user's account and default card in Firestore.
     *
     * @param uid   Firebase Auth UID
     * @param name  Full name
     * @param email Email address
     * @param phone Pakistan mobile number e.g. "+923001234567"
     */
    public Task<Void> createNewUserData(String uid, String name, String email, String phone) {
        // Generate a fake IBAN for the World Bank account
        String iban = "PK36WBNK" + String.format("%016d",
                Math.abs(uid.hashCode() % 9999999999999999L));

        // Create user profile
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("uid",         uid);
        userMap.put("displayName", name);
        userMap.put("email",       email);
        userMap.put("phone",       phone);
        userMap.put("city",        "Pakistan");
        userMap.put("createdAt",   Timestamp.now());

        // Create savings account with Rs. 0 starting balance
        Map<String, Object> accountMap = new HashMap<>();
        accountMap.put("uid",           uid);
        accountMap.put("accountNumber", iban);
        accountMap.put("accountTitle",  name);
        accountMap.put("bankName",      "World Bank");
        accountMap.put("accountType",   Account.TYPE_SAVINGS);
        accountMap.put("balance",       0.0);
        accountMap.put("currency",      Account.CURRENCY_PKR);
        accountMap.put("isActive",      true);

        DocumentReference userRef    = db.collection(COL_USERS).document(uid);
        DocumentReference accountRef = db.collection(COL_ACCOUNTS).document();

        return db.runBatch(batch -> {
            batch.set(userRef, userMap);
            batch.set(accountRef, accountMap);

            // Create a default debit card linked to this account
            String maskedCard = "4532 **** **** " +
                    String.format("%04d", Math.abs(uid.hashCode() % 10000));

            Map<String, Object> cardMap = new HashMap<>();
            cardMap.put("uid",          uid);
            cardMap.put("accountId",    accountRef.getId());
            cardMap.put("maskedNumber", maskedCard);
            cardMap.put("holderName",   name);
            cardMap.put("expiry",       "12/28");
            cardMap.put("cardType",     "VISA");
            cardMap.put("isActive",     true);
            cardMap.put("monthlyLimit", 200000.0);
            cardMap.put("monthlyUsed",  0.0);

            DocumentReference cardRef = db.collection(COL_CARDS).document();
            batch.set(cardRef, cardMap);
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════

    /** Returns the admin fee for a given transfer type */
    public static double getAdminFee(String transferType) {
        if (transferType == null) return ADMIN_FEE_IBFT;
        switch (transferType) {
            case Transaction.TRANSFER_INTERNAL:  return ADMIN_FEE_INTERNAL;
            case Transaction.TRANSFER_JAZZCASH:
            case Transaction.TRANSFER_EASYPAISA: return ADMIN_FEE_WALLET;
            case Transaction.TRANSFER_IBFT:
            default:                             return ADMIN_FEE_IBFT;
        }
    }
}