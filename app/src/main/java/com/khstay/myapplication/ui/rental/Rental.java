package com.khstay.myapplication.ui.rental;

public class Rental {
    private int id;
    private String title;
    private String location;
    private String price;
    private String status;
    private int imageResId;

    public Rental(int id, String title, String location, String price, String status, int imageResId) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.price = price;
        this.status = status;
        this.imageResId = imageResId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getImageResId() {
        return imageResId;
    }

    public void setImageResId(int imageResId) {
        this.imageResId = imageResId;
    }
}