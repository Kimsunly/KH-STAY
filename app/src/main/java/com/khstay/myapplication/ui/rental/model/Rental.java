package com.khstay.myapplication.ui.rental.model;

import androidx.annotation.Keep;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.List;
import java.util.ArrayList;

/**
 * Unified model used by Firestore and UI.
 * Now supports multiple images (max 3) and view tracking
 */
@Keep
@IgnoreExtraProperties
public class Rental {

    // ===== Firestore fields =====
    private String id;
    private String title;
    private String location;
    private Double price;
    private String status;
    private Boolean isPopular;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Images support
    private String imageUrl;
    private List<String> imageUrls;

    // Location
    private Double latitude;
    private Double longitude;

    // Details
    private String ownerId;
    private String category;
    private String description;
    private Integer bedrooms;
    private Integer bathrooms;

    // NEW: Popularity tracking (Hybrid Score System)
    private Integer viewCount;           // Track how many times viewed
    private Integer favoriteCount;       // Track how many favorites
    private Integer bookingCount;        // Track how many bookings
    private Double popularityScore;      // Calculated score for ranking

    // ===== UI-only helpers =====
    private Integer imageResId;
    private Boolean favorite;

    /** REQUIRED by Firestore */
    public Rental() { }

    /** UI sample constructor */
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
        this.viewCount = 0;
        this.favoriteCount = 0;
        this.bookingCount = 0;
        this.popularityScore = 0.0;
    }

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
        this.viewCount = 0;
        this.favoriteCount = 0;
        this.bookingCount = 0;
        this.popularityScore = 0.0;
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

    // Multiple images support
    public List<String> getImageUrls() {
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                imageUrls.add(imageUrl);
            }
        }
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        if (imageUrls != null && !imageUrls.isEmpty()) {
            this.imageUrl = imageUrls.get(0);
        }
    }

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

    // NEW: View tracking getters/setters
    public Integer getViewCount() {
        return viewCount != null ? viewCount : 0;
    }
    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount != null ? favoriteCount : 0;
    }
    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Integer getBookingCount() {
        return bookingCount != null ? bookingCount : 0;
    }
    public void setBookingCount(Integer bookingCount) {
        this.bookingCount = bookingCount;
    }

    // FIXED: Add missing popularityScore getter/setter
    public Double getPopularityScore() {
        return popularityScore != null ? popularityScore : 0.0;
    }
    public void setPopularityScore(Double popularityScore) {
        this.popularityScore = popularityScore;
    }

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

    public boolean hasMultipleImages() {
        return imageUrls != null && imageUrls.size() > 1;
    }
}