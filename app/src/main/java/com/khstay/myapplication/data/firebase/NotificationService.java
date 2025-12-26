package com.khstay.myapplication.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

public class NotificationService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public NotificationService() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Create a new notification
     */
    public Task<Void> createNotification(String userId, String title, String message, String type) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("type", type);
        notification.put("read", false);
        notification.put("createdAt", Timestamp.now());

        return db.collection("users")
                .document(userId)
                .collection("notifications")
                .document()
                .set(notification);
    }

    /**
     * Get notifications for current user
     */
    public Query getNotifications() {
        String currentUserId = auth.getCurrentUser().getUid();
        return db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    /**
     * Mark a notification as read
     */
    public Task<Void> markAsRead(String notificationId) {
        String currentUserId = auth.getCurrentUser().getUid();
        return db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(notificationId)
                .update("read", true);
    }

    /**
     * Mark all notifications as read
     */
    public Task<Void> markAllAsRead() {
        String currentUserId = auth.getCurrentUser().getUid();

        return db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        task.getResult().getDocuments().forEach(doc ->
                                doc.getReference().update("read", true)
                        );
                    }
                    return task.continueWith(t -> null);
                });
    }

    /**
     * Delete a notification
     */
    public Task<Void> deleteNotification(String notificationId) {
        String currentUserId = auth.getCurrentUser().getUid();
        return db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .document(notificationId)
                .delete();
    }

    /**
     * Get unread notification count
     */
    public Task<Integer> getUnreadCount() {
        String currentUserId = auth.getCurrentUser().getUid();

        return db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .whereEqualTo("read", false)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return task.getResult().size();
                    }
                    return 0;
                });
    }

    /**
     * Chat Service - Inner class for better organization
     */
    public static class ChatService extends com.khstay.myapplication.data.firebase.ChatService {
        // This extends the standalone ChatService class
        // Keeping it here for backward compatibility with existing code
    }
}