package com.khstay.myapplication.ui.chat.model;

import androidx.annotation.Keep;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.List;
import java.util.Map;

@Keep
@IgnoreExtraProperties
public class Conversation {
    private String id;
    private List<String> participantIds;
    private String lastMessage;
    private Timestamp lastMessageTime;
    private String lastMessageSenderId;
    private Timestamp createdAt;
    private String rentalId;
    private Map<String, Integer> unreadCounts;

    // UI helpers
    private String otherUserName;
    private String otherUserPhoto;
    private String otherUserId;

    public Conversation() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public List<String> getParticipantIds() { return participantIds; }
    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Timestamp getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(Timestamp lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastMessageSenderId() { return lastMessageSenderId; }
    public void setLastMessageSenderId(String lastMessageSenderId) {
        this.lastMessageSenderId = lastMessageSenderId;
    }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getRentalId() { return rentalId; }
    public void setRentalId(String rentalId) { this.rentalId = rentalId; }

    public Map<String, Integer> getUnreadCounts() { return unreadCounts; }
    public void setUnreadCounts(Map<String, Integer> unreadCounts) {
        this.unreadCounts = unreadCounts;
    }

    public String getOtherUserName() { return otherUserName; }
    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserPhoto() { return otherUserPhoto; }
    public void setOtherUserPhoto(String otherUserPhoto) {
        this.otherUserPhoto = otherUserPhoto;
    }

    public String getOtherUserId() { return otherUserId; }
    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public int getUnreadCountForUser(String userId) {
        if (unreadCounts != null && unreadCounts.containsKey(userId)) {
            Integer count = unreadCounts.get(userId);
            return count != null ? count : 0;
        }
        return 0;
    }
}