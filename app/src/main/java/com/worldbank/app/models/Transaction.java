package com.worldbank.app.models;

import com.google.firebase.Timestamp;

public class Transaction {

    public static final String TYPE_CREDIT = "CREDIT";
    public static final String TYPE_DEBIT  = "DEBIT";

    public static final String CAT_TRANSFER = "Transfer";
    public static final String CAT_PAYMENT  = "Payment";
    public static final String CAT_TOPUP    = "Top Up";
    public static final String CAT_SHOPPING = "Shopping";
    public static final String CAT_GAME     = "Game";
    public static final String CAT_WITHDRAW = "Withdraw";

    private String txnId;
    private String uid;
    private String cardId;
    private String type;           // TYPE_CREDIT or TYPE_DEBIT
    private String category;       // CAT_* constants above
    private String recipientName;
    private String recipientAccount;
    private double amount;
    private double adminFee;
    private Timestamp timestamp;
    private String status;         // "SUCCESS" / "PENDING" / "FAILED"

    public Transaction() {} // Required for Firestore

    // Getters
    public String getTxnId() { return txnId; }
    public String getUid() { return uid; }
    public String getCardId() { return cardId; }
    public String getType() { return type; }
    public String getCategory() { return category; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientAccount() { return recipientAccount; }
    public double getAmount() { return amount; }
    public double getAdminFee() { return adminFee; }
    public Timestamp getTimestamp() { return timestamp; }
    public String getStatus() { return status; }

    // Setters
    public void setTxnId(String txnId) { this.txnId = txnId; }
    public void setUid(String uid) { this.uid = uid; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public void setType(String type) { this.type = type; }
    public void setCategory(String category) { this.category = category; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public void setRecipientAccount(String recipientAccount) { this.recipientAccount = recipientAccount; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setAdminFee(double adminFee) { this.adminFee = adminFee; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setStatus(String status) { this.status = status; }

    /** Returns true if this is an incoming transaction */
    public boolean isCredit() {
        return TYPE_CREDIT.equals(type);
    }

    /** Returns formatted amount string e.g. "+$21.21" or "-$163.98" */
    public String getFormattedAmount() {
        String prefix = isCredit() ? "+$" : "-$";
        return String.format("%s%.2f", prefix, amount);
    }
}
