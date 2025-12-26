package com.khstay.myapplication.ui.chat.model;

import androidx.annotation.Keep;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;

@Keep
@IgnoreExtraProperties
public class Message {
    private String id;
    private String senderId;
    private String receiverId;
    private String message;
    private Timestamp timestamp;
    private Boolean read;

    public Message() {}

    public Message(String senderId, String receiverId, String message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = Timestamp.now();
        this.read = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
}