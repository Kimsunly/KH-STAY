package com.khstay.myapplication.ui.profile;

public class PaymentMethod {
    private String id;
    private String cardType;
    private String lastFourDigits;
    private String expiryDate;
    private boolean isDefault;

    public PaymentMethod() {}

    public PaymentMethod(String id, String cardType, String lastFourDigits, String expiryDate, boolean isDefault) {
        this.id = id;
        this.cardType = cardType;
        this.lastFourDigits = lastFourDigits;
        this.expiryDate = expiryDate;
        this.isDefault = isDefault;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCardType() { return cardType; }
    public void setCardType(String cardType) { this.cardType = cardType; }

    public String getLastFourDigits() { return lastFourDigits; }
    public void setLastFourDigits(String lastFourDigits) { this.lastFourDigits = lastFourDigits; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }
}