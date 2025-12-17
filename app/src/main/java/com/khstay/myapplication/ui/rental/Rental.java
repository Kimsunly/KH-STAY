package com.khstay.myapplication.ui.rental;

public class Rental {
    private int id;
    private String title;
    private String location;
    private String price;
    private String status;
    private int imageResId;

    // Additional fields for search functionality
    private String category;        // For category filtering (Toul Kok, Dangkao, etc.)
    private String description;     // For search description
    private int bedrooms;           // Number of bedrooms
    private int bathrooms;          // Number of bathrooms
    private boolean isFavorite;     // Favorite status
    private String imageUrl;        // For online images (optional)

    // Constructor for MyRentFragment (existing usage)
    public Rental(int id, String title, String location, String price, String status, int imageResId) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.price = price;
        this.status = status;
        this.imageResId = imageResId;
        this.category = extractCategoryFromLocation(location);
        this.description = "";
        this.bedrooms = 1;
        this.bathrooms = 1;
        this.isFavorite = false;
        this.imageUrl = "";
    }

    // Full constructor for SearchFragment with all fields
    public Rental(int id, String title, String location, String price, String status,
                  int imageResId, String category, String description,
                  int bedrooms, int bathrooms) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.price = price;
        this.status = status;
        this.imageResId = imageResId;
        this.category = category;
        this.description = description;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.isFavorite = false;
        this.imageUrl = "";
    }

    // Constructor with URL support
    public Rental(int id, String title, String location, String price, String status,
                  String imageUrl, String category, String description,
                  int bedrooms, int bathrooms) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.price = price;
        this.status = status;
        this.imageResId = 0;
        this.imageUrl = imageUrl;
        this.category = category;
        this.description = description;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.isFavorite = false;
    }

    // Helper method to extract category from location string
    private String extractCategoryFromLocation(String location) {
        if (location == null) return "All";

        location = location.toLowerCase();
        if (location.contains("toul kok") || location.contains("tuol kouk")) {
            return "Toul Kok";
        } else if (location.contains("psar derm tkev") || location.contains("psar deum thkov")) {
            return "Psar Derm Tkev";
        } else if (location.contains("dangkao") || location.contains("dang kao")) {
            return "Dangkao";
        } else if (location.contains("chamkar mon") || location.contains("chamkarmorn")) {
            return "Chamkar Mon";
        } else if (location.contains("mean chey") || location.contains("meanchey")) {
            return "Mean Chey";
        } else if (location.contains("sen sok")) {
            return "Sen Sok";
        } else if (location.contains("boeng keng kong")) {
            return "Boeng Keng Kong";
        } else {
            return "All";
        }
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    public String getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public int getBedrooms() {
        return bedrooms;
    }

    public int getBathrooms() {
        return bathrooms;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLocation(String location) {
        this.location = location;
        this.category = extractCategoryFromLocation(location);
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBedrooms(int bedrooms) {
        this.bedrooms = bedrooms;
    }

    public void setBathrooms(int bathrooms) {
        this.bathrooms = bathrooms;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Utility methods
    public String getFormattedPrice() {
        return "$" + price + "/month";
    }

    public String getBedroomBathroomText() {
        return bedrooms + " bed • " + bathrooms + " bath";
    }

    // Check if has online image URL
    public boolean hasImageUrl() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    @Override
    public String toString() {
        return "Rental{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", price='" + price + '\'' +
                ", category='" + category + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}