package com.worldbank.app.models;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.Timestamp;
import java.util.UUID;
import com.google.firebase.firestore.Exclude;
/**
 * Transaction.java — UPDATED for Pakistani banking
 * ──────────────────────────────────────────────────
 * Represents a money movement in PKR.
 */
public class Transaction {

    // ── Type constants ────────────────────────────────────────────
    public static final String TYPE_CREDIT = "CREDIT";
    public static final String TYPE_DEBIT  = "DEBIT";

    // ── Category constants ────────────────────────────────────────
    public static final String CAT_TRANSFER    = "Transfer";
    public static final String CAT_BILL        = "Bill Payment";
    public static final String CAT_TOPUP       = "Top Up";
    public static final String CAT_SALARY      = "Salary";
    public static final String CAT_SHOPPING    = "Shopping";
    public static final String CAT_WITHDRAW    = "Withdrawal";
    public static final String CAT_MOBILE      = "Mobile Topup";
    public static final String CAT_GAME        = "Game Top Up";
    public static final String CAT_PAYMENT     = "Payment";

    // ── Transfer type constants ───────────────────────────────────
    public static final String TRANSFER_INTERNAL   = "INTERNAL";   // within World Bank app
    public static final String TRANSFER_IBFT       = "IBFT";       // other Pakistani banks
    public static final String TRANSFER_JAZZCASH   = "JAZZ_CASH";
    public static final String TRANSFER_EASYPAISA  = "EASYPAISA";

    // ── Status constants ──────────────────────────────────────────
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED  = "FAILED";

    // ── Fields ───────────────────────────────────────────────────
    private String txnId;
    private String senderUid;
    private String recipientUid;
    private String recipientAccountId; // NEW: To link account doc for internal transfers
    private String senderAccount;
    private String recipientAccount;
    private String recipientName;
    private String recipientBank;
    private double amount;
    private double adminFee;
    private double totalDeducted;
    private String type;
    private String category;
    private String transferType;
    private String referenceNumber;
    private String status;
    private String description;
    private Timestamp timestamp;

    private String uid; // legacy compat

    public Transaction() {} // Required for Firestore

    public static Transaction createTransfer(
            String senderUid, String senderAccount,
            String recipientUid, String recipientAccount,
            String recipientName, String recipientBank,
            double amount, double adminFee,
            String transferType, String description) {

        Transaction t = new Transaction();
        t.senderUid       = senderUid;
        t.uid             = senderUid;
        t.senderAccount   = senderAccount;
        t.recipientUid    = recipientUid;
        t.recipientAccount = recipientAccount;
        t.recipientName   = recipientName;
        t.recipientBank   = recipientBank;
        t.amount          = amount;
        t.adminFee        = adminFee;
        t.totalDeducted   = amount + adminFee;
        t.type            = TYPE_DEBIT;
        t.category        = CAT_TRANSFER;
        t.transferType    = transferType;
        t.referenceNumber = generateReference();
        t.status          = STATUS_PENDING;
        t.description     = description;
        t.timestamp       = Timestamp.now();
        return t;
    }

    public static String generateReference() {
        long epoch = System.currentTimeMillis();
        String tail = String.valueOf(epoch).substring(Math.max(0, String.valueOf(epoch).length() - 6));
        java.util.Calendar cal = java.util.Calendar.getInstance();
        return String.format("WB%04d%02d%02d%s",
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH) + 1,
                cal.get(java.util.Calendar.DAY_OF_MONTH),
                tail);
    }

    @Exclude
    public boolean isCredit() {
        return TYPE_CREDIT.equals(type);
    }
    @Exclude
    public String getFormattedAmount() {
        String prefix = isCredit() ? "+Rs. " : "-Rs. ";
        return String.format("%s%,.0f", prefix, amount);
    }

    // ── Getters ──────────────────────────────────────────────────
    public String getTxnId()            { return txnId; }
    public String getSenderUid()        { return senderUid; }
    public String getUid()              { return uid != null ? uid : senderUid; }
    public String getRecipientUid()     { return recipientUid; }
    public String getRecipientAccountId() { return recipientAccountId; }
    public String getSenderAccount()    { return senderAccount; }
    public String getRecipientAccount() { return recipientAccount; }
    public String getRecipientName()    { return recipientName; }
    public String getRecipientBank()    { return recipientBank; }
    public double getAmount()           { return amount; }
    public double getAdminFee()         { return adminFee; }
    public double getTotalDeducted()    { return totalDeducted; }
    public String getType()             { return type; }
    public String getCategory()         { return category; }
    public String getTransferType()     { return transferType; }
    public String getReferenceNumber()  { return referenceNumber; }
    public String getStatus()           { return status; }
    public String getDescription()      { return description; }
    public Timestamp getTimestamp()     { return timestamp; }

    // ── Setters ──────────────────────────────────────────────────
    public void setTxnId(String txnId)                  { this.txnId = txnId; }
    public void setSenderUid(String uid)                { this.senderUid = uid; this.uid = uid; }
    public void setUid(String uid)                      { this.uid = uid; this.senderUid = uid; }
    public void setRecipientUid(String uid)             { this.recipientUid = uid; }
    public void setRecipientAccountId(String id)        { this.recipientAccountId = id; }
    public void setSenderAccount(String a)              { this.senderAccount = a; }
    public void setRecipientAccount(String a)           { this.recipientAccount = a; }
    public void setRecipientName(String name)           { this.recipientName = name; }
    public void setRecipientBank(String bank)           { this.recipientBank = bank; }
    public void setAmount(double amount)                { this.amount = amount; }
    public void setAdminFee(double fee)                 { this.adminFee = fee; }
    public void setTotalDeducted(double total)          { this.totalDeducted = total; }
    public void setType(String type)                    { this.type = type; }
    public void setCategory(String category)            { this.category = category; }
    public void setTransferType(String t)               { this.transferType = t; }
    public void setReferenceNumber(String ref)          { this.referenceNumber = ref; }
    public void setStatus(String status)                { this.status = status; }
    public void setDescription(String desc)             { this.description = desc; }
    public void setTimestamp(Timestamp timestamp)       { this.timestamp = timestamp; }
}