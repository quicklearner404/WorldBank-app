package com.worldbank.app.models;

/**
 * Account.java
 * ─────────────
 * Represents a bank account in PKR.
 * One user can have multiple accounts (savings, current).
 * Each account links to one or more cards.
 *
 * Firestore collection: accounts/{accountId}
 */
public class Account {

    public static final String TYPE_SAVINGS = "SAVINGS";
    public static final String TYPE_CURRENT = "CURRENT";
    public static final String CURRENCY_PKR = "PKR";

    private String accountId;
    private String uid;              // Firebase Auth UID of owner
    private String accountNumber;   // Full IBAN e.g. "PK36WBNK0000001123456702"
    private String accountTitle;    // "Ali Hassan"
    private String bankName;        // "World Bank"
    private String accountType;     // TYPE_SAVINGS / TYPE_CURRENT
    private double balance;         // in PKR
    private String currency;        // always "PKR"
    private boolean isActive;

    public Account() {} // Required for Firestore

    public Account(String uid, String accountNumber, String accountTitle,
                   String bankName, String accountType, double balance) {
        this.uid           = uid;
        this.accountNumber = accountNumber;
        this.accountTitle  = accountTitle;
        this.bankName      = bankName;
        this.accountType   = accountType;
        this.balance       = balance;
        this.currency      = CURRENCY_PKR;
        this.isActive      = true;
    }

    // ── Getters ──────────────────────────────────────────────────
    public String getAccountId()     { return accountId; }
    public String getUid()           { return uid; }
    public String getAccountNumber() { return accountNumber; }
    public String getAccountTitle()  { return accountTitle; }
    public String getBankName()      { return bankName; }
    public String getAccountType()   { return accountType; }
    public double getBalance()       { return balance; }
    public String getCurrency()      { return currency; }
    //public boolean isActive()        { return isActive; }

    // ── Setters ──────────────────────────────────────────────────
    public void setAccountId(String accountId)     { this.accountId = accountId; }
    public void setUid(String uid)                 { this.uid = uid; }
    public void setAccountNumber(String n)         { this.accountNumber = n; }
    public void setAccountTitle(String t)          { this.accountTitle = t; }
    public void setBankName(String b)              { this.bankName = b; }
    public void setAccountType(String t)           { this.accountType = t; }
    public void setBalance(double balance)         { this.balance = balance; }
    public void setCurrency(String currency)       { this.currency = currency; }
    //public void setActive(boolean active)          { isActive = active; }
// Replace your old getter with this:
// Notice the names are back to isActive and setActive,
    // but we kept the @PropertyName tags to force Firestore to behave!

    @com.google.firebase.firestore.PropertyName("isActive")
    public boolean isActive() {
        return isActive;
    }

    @com.google.firebase.firestore.PropertyName("isActive")
    public void setActive(boolean active) {
        isActive = active;
    }
    /**
     * Returns a masked version of the account number for display.
     * "PK36WBNK0000001123456702" → "PK36 **** **** 6702"
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 8) return accountNumber;
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return accountNumber.substring(0, 4) + " **** **** " + last4;
    }

    /**
     * Returns formatted balance in PKR.
     * 125000.0 → "Rs. 1,25,000"
     */
    public String getFormattedBalance() {
        return String.format("Rs. %,.0f", balance);
    }
}