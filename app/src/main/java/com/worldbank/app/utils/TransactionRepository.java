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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class TransactionRepository {

    private static final String TAG          = "TransactionRepo";
    private static final String COL_USERS        = "users";
    private static final String COL_ACCOUNTS     = "accounts";
    private static final String COL_CARDS        = "cards";
    private static final String COL_TRANSACTIONS = "transactions";
    private static final String COL_CONTACTS     = "contacts";

    private final FirebaseFirestore db;

    public TransactionRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ── Account queries ─────────────────────────────────────────────

    public Query getAccountQuery(String uid) {
        return db.collection(COL_ACCOUNTS).whereEqualTo("uid", uid).limit(1);
    }

    public Task<DocumentSnapshot> getAccount(String accountId) {
        if (accountId == null || accountId.isEmpty())
            return Tasks.forException(new Exception("Invalid Account ID"));
        return db.collection(COL_ACCOUNTS).document(accountId).get();
    }

    public Task<QuerySnapshot> findAccountByIban(String iban) {
        // Strip all whitespace before querying
        String normalized = iban.replaceAll("\\s+", "").toUpperCase();
        return db.collection(COL_ACCOUNTS)
                .whereEqualTo("accountNumber", normalized)
                .limit(1)
                .get();
    }


    public Task<QuerySnapshot> findUserByPhone(String phone) {
        String clean = phone.replaceAll("[^0-9]", "");
        List<String> variations = new ArrayList<>();

        variations.add(phone.trim());
        variations.add(clean);

        if (clean.startsWith("0") && clean.length() == 11) {
            // 03001234567  → +923001234567, 923001234567, and +92 3001234567
            variations.add("+92" + clean.substring(1));
            variations.add("92"  + clean.substring(1));
            variations.add("+92 " + clean.substring(1)); // <-- Added the space version!
        } else if (clean.startsWith("92") && clean.length() == 12) {
            // 923001234567 → +923001234567, 03001234567, and +92 3001234567
            variations.add("+"  + clean);
            variations.add("0"  + clean.substring(2));
            variations.add("+92 " + clean.substring(2)); // <-- Added the space version!
        } else if (clean.startsWith("923") && clean.length() == 12) {
            variations.add("+" + clean);
            variations.add("0" + clean.substring(2));
            variations.add("+92 " + clean.substring(2)); // <-- Added the space version!
        }

        return db.collection(COL_USERS)
                .whereIn("phone", variations)
                .limit(1)
                .get();
    }

    // ── Card queries ────────────────────────────────────────────────

    public Query getCardsQuery(String uid) {
        return db.collection(COL_CARDS).whereEqualTo("uid", uid);
    }

    public Task<DocumentSnapshot> getCard(String cardId) {
        if (cardId == null || cardId.isEmpty())
            return Tasks.forException(new Exception("Invalid Card ID"));
        return db.collection(COL_CARDS).document(cardId).get();
    }

    public Task<Void> addCard(Map<String, Object> cardMap) {
        return db.collection(COL_CARDS).document().set(cardMap);
    }

    // ── Top-up ──────────────────────────────────────────────────────

// ── Top-up ──────────────────────────────────────────────────────

    public Task<String> topUp(String uid, String accountId, String cardId, double amount) {
        String referenceNumber = Transaction.generateReference();
        DocumentReference accRef = db.collection(COL_ACCOUNTS).document(accountId);
        DocumentReference cardRef = db.collection(COL_CARDS).document(cardId);
        DocumentReference txnRef = db.collection(COL_TRANSACTIONS).document();

        return db.runTransaction(tx -> {
            // 1. ALL READS MUST HAPPEN FIRST
            DocumentSnapshot accSnap = tx.get(accRef);
            DocumentSnapshot cardSnap = tx.get(cardRef);

            // 2. THE MOCK GATEWAY (VALIDATION)
            // Fetch limits, default to 50,000 limit and 0 used if missing
            double monthlyLimit = cardSnap.contains("monthlyLimit") ? cardSnap.getDouble("monthlyLimit") : 50000.0;
            double monthlyUsed = cardSnap.contains("monthlyUsed") ? cardSnap.getDouble("monthlyUsed") : 0.0;

            if (monthlyUsed + amount > monthlyLimit) {
                double available = monthlyLimit - monthlyUsed;
                throw new RuntimeException("Transaction declined by bank. Limit Exceeded. Available to use: Rs. "
                        + String.format("%,.0f", available));
            }

            double currentBalance = accSnap.contains("balance") ? accSnap.getDouble("balance") : 0.0;

            // 3. ALL WRITES HAPPEN LAST (Atomic Operation)

            // A. Add the amount to the card's 'monthlyUsed' tracker
            tx.update(cardRef, "monthlyUsed", monthlyUsed + amount);

            // B. Add the amount to the World Bank Account
            tx.update(accRef, "balance", currentBalance + amount);

            // C. Create the Success Receipt
            Transaction t = new Transaction();
            t.setUid(uid);
            t.setSenderUid(uid);
            t.setAmount(amount);
            t.setType(Transaction.TYPE_CREDIT);
            t.setCategory(Transaction.CAT_TOPUP);
            t.setReferenceNumber(referenceNumber);
            t.setTimestamp(Timestamp.now());
            t.setRecipientName("Card Top-Up"); // Looks better on receipt
            t.setStatus(Transaction.STATUS_SUCCESS);
            tx.set(txnRef, t);

            return referenceNumber;
        });
    }

    // ── Send money (2-way transfer) ─────────────────────────────────


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

        double adminFee      = getAdminFee(transferType);
        double totalDeducted = amount + adminFee;
        String referenceNumber = Transaction.generateReference();

        boolean isInternal = recipientUid       != null && !recipientUid.isEmpty()
                && recipientAccountId != null && !recipientAccountId.isEmpty();

        DocumentReference senderAccRef = db.collection(COL_ACCOUNTS).document(senderAccountId);
        DocumentReference senderTxnRef = db.collection(COL_TRANSACTIONS).document();

        // Prepare recipient references if internal
        DocumentReference recAccRef = isInternal ? db.collection(COL_ACCOUNTS).document(recipientAccountId) : null;
        DocumentReference recTxnRef = isInternal ? db.collection(COL_TRANSACTIONS).document() : null;
// Force a local final copy that CANNOT be changed
        return db.runTransaction(tx -> {
            Log.d("TRANSFER_UID_CHECK", "Sender UID: " + senderUid);
            Log.d("TRANSFER_UID_CHECK", "Recipient UID: " + recipientUid);
            // ── 1. ALL READS MUST HAPPEN FIRST ─────────────────────
            DocumentSnapshot senderSnap = tx.get(senderAccRef);
            DocumentSnapshot recSnap = null;
            if (isInternal) {
                recSnap = tx.get(recAccRef); // Read recipient BEFORE any writes!
            }

            // ── 2. VALIDATION ──────────────────────────────────────
            double senderBalance = senderSnap.contains("balance") ? senderSnap.getDouble("balance") : 0;
            if (senderBalance < totalDeducted) {
                throw new RuntimeException("Insufficient balance. Available: Rs. "
                        + String.format("%,.0f", senderBalance)
                        + ", Required: Rs. " + String.format("%,.0f", totalDeducted));
            }

            double recBalance = 0;
            if (isInternal && recSnap != null) {
                recBalance = recSnap.contains("balance") ? recSnap.getDouble("balance") : 0;
            }

            // ── 3. ALL WRITES HAPPEN LAST ──────────────────────────

            // A. Deduct from sender
            tx.update(senderAccRef, "balance", senderBalance - totalDeducted);

            // B. Write sender DEBIT record
            Transaction debit = new Transaction();
            debit.setUid(senderUid);
            debit.setSenderUid(senderUid);
            debit.setSenderAccount(senderSnap.getString("accountNumber"));
            debit.setRecipientName(recipientName);
            debit.setRecipientAccount(recipientAccount);
            debit.setRecipientBank(recipientBank);
            debit.setRecipientUid(isInternal ? recipientUid : "");
            debit.setAmount(amount);
            debit.setAdminFee(adminFee);
            debit.setTotalDeducted(totalDeducted);
            debit.setType(Transaction.TYPE_DEBIT);
            debit.setCategory(Transaction.CAT_TRANSFER);
            debit.setTransferType(transferType);
            debit.setDescription(description);
            debit.setStatus(Transaction.STATUS_SUCCESS);
            debit.setReferenceNumber(referenceNumber);
            debit.setTimestamp(Timestamp.now());
            tx.set(senderTxnRef, debit);

            // C. Credit recipient (internal transfers only)
            if (isInternal) {
                tx.update(recAccRef, "balance", recBalance + amount);

                Transaction credit = new Transaction();

                credit.setSenderUid(senderUid);
                credit.setUid(recipientUid);
                credit.setSenderUid(senderUid);
                credit.setRecipientUid(recipientUid);
                credit.setSenderAccount(senderSnap.getString("accountNumber"));
                credit.setRecipientName(
                        senderSnap.getString("accountTitle") != null
                                ? senderSnap.getString("accountTitle")
                                : "World Bank Sender");
                credit.setRecipientAccount(senderSnap.getString("accountNumber"));
                credit.setRecipientBank("World Bank");
                credit.setAmount(amount);
                credit.setAdminFee(0);
                credit.setTotalDeducted(amount);
                credit.setType(Transaction.TYPE_CREDIT);
                credit.setCategory(Transaction.CAT_TRANSFER);
                credit.setTransferType(Transaction.TRANSFER_INTERNAL);
                credit.setDescription(description);
                credit.setReferenceNumber(referenceNumber);
                credit.setStatus(Transaction.STATUS_SUCCESS);
                credit.setTimestamp(Timestamp.now());
                tx.set(recTxnRef, credit);
            }

            return referenceNumber;
        });
    }

    // ── Other queries ───────────────────────────────────────────────

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

    // ── Fee table ───────────────────────────────────────────────────

    public static double getAdminFee(String transferType) {
        if (transferType == null) return 25.0;
        switch (transferType) {
            case Transaction.TRANSFER_INTERNAL:   return 0.0;
            case Transaction.TRANSFER_JAZZCASH:
            case Transaction.TRANSFER_EASYPAISA:  return 0.0;
            default:                              return 25.0;  // IBFT
        }
    }
}