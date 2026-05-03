package com.worldbank.app.models;

import com.google.firebase.Timestamp;

public class Contact {

    public static final String BANK_HBL        = "HBL";
    public static final String BANK_MEEZAN     = "Meezan Bank";
    public static final String BANK_UBL        = "UBL";
    public static final String BANK_MCB        = "MCB";
    public static final String BANK_ALLIED     = "Allied Bank";
    public static final String BANK_ASKARI     = "Askari Bank";
    public static final String BANK_JAZZCASH   = "JazzCash";
    public static final String BANK_EASYPAISA  = "EasyPaisa";
    public static final String BANK_WORLDBANK  = "World Bank";

    private String contactId;
    private String ownerUid;        // whose contact list this belongs to
    private String name;            // "Fatima Khan"
    private String accountNumber;   // IBAN or mobile number
    private String bankName;        // BANK_* constants above
    private boolean isFavorite;
    private Timestamp lastUsed;
    private String recipientUid;

    public Contact() {} // Required for Firestore

    public Contact(String ownerUid, String recipientUid, String name, String accountNumber, String bankName) {
        this.ownerUid      = ownerUid;
        this.recipientUid  = recipientUid; // Set it here
        this.name          = name;
        this.accountNumber = accountNumber;
        this.bankName      = bankName;
        this.isFavorite    = false;
    }

    // ── Getters ──────────────────────────────────────────────────
    public String getContactId()     { return contactId; }
    public String getOwnerUid()      { return ownerUid; }
    public String getName()          { return name; }
    public String getAccountNumber() { return accountNumber; }
    public String getBankName()      { return bankName; }
    public boolean isFavorite()      { return isFavorite; }
    public Timestamp getLastUsed()   { return lastUsed; }
    public String getRecipientUid() { return recipientUid; }
    public void setRecipientUid(String uid) { this.recipientUid = uid; }
    // ── Setters ──────────────────────────────────────────────────
    public void setContactId(String id)        { this.contactId = id; }
    public void setOwnerUid(String uid)        { this.ownerUid = uid; }
    public void setName(String name)           { this.name = name; }
    public void setAccountNumber(String n)     { this.accountNumber = n; }
    public void setBankName(String b)          { this.bankName = b; }
    public void setFavorite(boolean f)         { this.isFavorite = f; }
    public void setLastUsed(Timestamp t)       { this.lastUsed = t; }

    /** Returns initials from name for avatar display. "Fatima Khan" → "FK" */
    public String getInitials() {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)).toUpperCase();
        }
        return String.valueOf(parts[0].charAt(0)).toUpperCase();
    }
}