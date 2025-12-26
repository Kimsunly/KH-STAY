package com.khstay.myapplication.ui.rental.model;

import androidx.annotation.Keep;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

/**
 * Unified model used by Firestore and UI.
 * Includes all getters/setters your Activity and adapters call.
 */
@Keep
@IgnoreExtraProperties
public class Rental {

    // ===== Firestore fields =====
    private String id;               // set from snapshot.getId()
    private String title;
    private String location;
    private Double price;            // Firestore Number -> Double
    private String status;           // "active" | "pending" | "archived"
    private Boolean isPopular;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private String ownerId;
    private String category;
    private String description;
    private Integer bedrooms;
    private Integer bathrooms;

    // ===== UI-only helpers =====
    private Integer imageResId;      // local drawable fallback
    private Boolean favorite;        // heart toggle in UI

    /** REQUIRED by Firestore */
    public Rental() { }

    /** UI sample constructor (used in local dummy data) */
    public Rental(int idInt, String title, String location,
                  String priceText, String statusText, int imageResId) {
        this.id = String.valueOf(idInt);
        this.title = title;
        this.location = location;
        try { this.price = Double.valueOf(priceText); } catch (Exception e) { this.price = 0.0; }
        this.status = statusText;
        this.imageResId = imageResId;
        this.bedrooms = 0;
        this.bathrooms = 0;
        this.favorite = false;
    }

    /** UI constructor used by SearchFragment */
    public Rental(int idInt, String title, String location, String priceText, String statusText,
                  int imageResId, String imageUrl, String description, int bedrooms, int bathrooms) {
        this.id = String.valueOf(idInt);
        this.title = title;
        this.location = location;
        try { this.price = Double.valueOf(priceText); } catch (Exception e) { this.price = 0.0; }
        this.status = statusText;
        this.imageResId = imageResId;
        this.imageUrl = imageUrl;
        this.description = description;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.favorite = false;
    }

    // ===== Getters & Setters =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Boolean getIsPopular() { return isPopular; }
    public void setIsPopular(Boolean isPopular) { this.isPopular = isPopular; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getBedrooms() { return bedrooms; }
    public void setBedrooms(Integer bedrooms) { this.bedrooms = bedrooms; }

    public Integer getBathrooms() { return bathrooms; }
    public void setBathrooms(Integer bathrooms) { this.bathrooms = bathrooms; }

    public int getImageResId() { return imageResId != null ? imageResId : 0; }
    public void setImageResId(Integer imageResId) { this.imageResId = imageResId; }

    public boolean isFavorite() { return favorite != null && favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    // ===== Helper Methods =====
    public String getBedroomBathroomText() {
        int b = getBedrooms() != null ? getBedrooms() : 0;
        int ba = getBathrooms() != null ? getBathrooms() : 0;
        if (b <= 0 && ba <= 0) return "";
        String bedText = b > 0 ? (b + (b == 1 ? " Bed" : " Beds")) : "";
        String bathText = ba > 0 ? (ba + (ba == 1 ? " Bath" : " Baths")) : "";
        return (!bedText.isEmpty() && !bathText.isEmpty()) ? (bedText + " · " + bathText) : (bedText + bathText);
    }

    public boolean hasImageUrl() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }
}