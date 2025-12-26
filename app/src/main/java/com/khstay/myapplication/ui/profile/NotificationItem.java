package com.khstay.myapplication.ui.profile;

import com.google.firebase.Timestamp;

public class NotificationItem {
    private String id;
    private String title;
    private String message;
    private String type;
    private boolean read;
    private Timestamp timestamp;

    public NotificationItem() {}

    public NotificationItem(String id, String title, String message, String type, boolean read, Timestamp timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.type = type;
        this.read = read;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}