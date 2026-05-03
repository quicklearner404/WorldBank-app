package com.worldbank.app.models;

public class Card {

    String cardId;
    String uid;
    String accountId;
    String cardNumber;
    String maskedNumber;
    String holderName;
    String expiry;
    String cardType;
    boolean isActive;
    double balance;
    double monthlyLimit;
    double monthlyUsed;

    public Card() {}

    public Card(String uid, String accountId, String maskedNumber,
                String holderName, String expiry, String cardType,
                double monthlyLimit) {
        this.uid = uid;
        this.accountId = accountId;
        this.maskedNumber = maskedNumber;
        this.holderName = holderName;
        this.expiry = expiry;
        this.cardType = cardType;
        this.isActive = true;
        this.balance = 0;
        this.monthlyLimit = monthlyLimit;
        this.monthlyUsed = 0;
    }

    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

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

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getMonthlyLimit() { return monthlyLimit; }
    public void setMonthlyLimit(double monthlyLimit) { this.monthlyLimit = monthlyLimit; }

    public double getMonthlyUsed() { return monthlyUsed; }
    public void setMonthlyUsed(double monthlyUsed) { this.monthlyUsed = monthlyUsed; }

    public int getLimitPercentage() {
        if (monthlyLimit <= 0) return 0;
        return (int) ((monthlyUsed / monthlyLimit) * 100);
    }
}