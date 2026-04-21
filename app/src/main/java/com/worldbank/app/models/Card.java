package com.worldbank.app.models;

public class Card {
    private String cardId;
    private String uid;
    private String cardNumber;    // full number (store encrypted in real app)
    private String maskedNumber;  // e.g. "**** **** **** 3090"
    private String holderName;
    private String expiry;        // e.g. "09/24"
    private String cardType;      // "VISA" or "MASTERCARD"
    private double balance;
    private double monthlyLimit;
    private double monthlyUsed;

    public Card() {} // Required for Firestore

    public Card(String cardId, String uid, String maskedNumber, String holderName,
                String expiry, String cardType, double balance,
                double monthlyLimit, double monthlyUsed) {
        this.cardId = cardId;
        this.uid = uid;
        this.maskedNumber = maskedNumber;
        this.holderName = holderName;
        this.expiry = expiry;
        this.cardType = cardType;
        this.balance = balance;
        this.monthlyLimit = monthlyLimit;
        this.monthlyUsed = monthlyUsed;
    }

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getMaskedNumber() { return maskedNumber; }
    public void setMaskedNumber(String maskedNumber) { this.maskedNumber = maskedNumber; }

    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }

    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public double getMonthlyUsed() { return monthlyUsed; }
    public void setMonthlyUsed(double monthlyUsed) { this.monthlyUsed = monthlyUsed; }

    /** Returns usage percentage (0–100) for the circular progress bar */
    public int getLimitPercentage() {
        if (monthlyLimit <= 0) return 0;
        return (int) ((monthlyUsed / monthlyLimit) * 100);
    }
}
