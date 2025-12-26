
package com.khstay.myapplication.ui.profile;

import com.google.firebase.Timestamp;

public class RecentViewedItem {
    private String id;

    // Accept both legacy and new field names from Firestore
    private String propertyId;   // legacy field name (if older docs still have it)
    private String rentalId;     // canonical field name (use going forward)

    private String propertyName;
    private String propertyImage;
    private String propertyPrice;
    private String propertyLocation;
    private Timestamp viewedAt;

    public RecentViewedItem() {}

    public RecentViewedItem(String id, String rentalId, String propertyName, String propertyImage,
                            String propertyPrice, String propertyLocation, Timestamp viewedAt) {
        this.id = id;
        this.rentalId = rentalId;
        this.propertyName = propertyName;
        this.propertyImage = propertyImage;
        this.propertyPrice = propertyPrice;
        this.propertyLocation = propertyLocation;
        this.viewedAt = viewedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }

    public String getRentalId() { return rentalId; }
    public void setRentalId(String rentalId) { this.rentalId = rentalId; }

    public String getPropertyName() { return propertyName; }
    public void setPropertyName(String propertyName) { this.propertyName = propertyName; }

    public String getPropertyImage() { return propertyImage; }
    public void setPropertyImage(String propertyImage) { this.propertyImage = propertyImage; }

    public String getPropertyPrice() { return propertyPrice; }
    public void setPropertyPrice(String propertyPrice) { this.propertyPrice = propertyPrice; }

    public String getPropertyLocation() { return propertyLocation; }
    public void setPropertyLocation(String propertyLocation) { this.propertyLocation = propertyLocation; }

    public Timestamp getViewedAt() { return viewedAt; }
    public void setViewedAt(Timestamp viewedAt) { this.viewedAt = viewedAt; }

    /** Unified way to get the rental doc ID safely (handles legacy/propertyId) */
    public String getEffectiveRentalId() {
        if (rentalId != null && !rentalId.trim().isEmpty()) return rentalId;
        if (propertyId != null && !propertyId.trim().isEmpty()) return propertyId;
        if (id != null && !id.trim().isEmpty()) return id; // fallback: some legacy used docId
        return null;
    }
}
