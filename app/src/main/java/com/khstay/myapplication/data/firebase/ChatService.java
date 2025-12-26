package com.khstay.myapplication.data.firebase;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatService {

    private static final String TAG = "ChatService";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ChatService() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Get or create a conversation between current user and another user
     */
    public Task<String> getOrCreateConversation(String otherUserId, String rentalId) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Generate consistent conversation ID (sorted user IDs)
        String conversationId = generateConversationId(currentUserId, otherUserId);

        DocumentReference conversationRef = db.collection("conversations").document(conversationId);

        return conversationRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            DocumentSnapshot document = task.getResult();

            if (document.exists()) {
                // Conversation already exists
                Log.d(TAG, "Conversation already exists: " + conversationId);
                return Tasks.forResult(conversationId);
            } else {
                // Create new conversation
                Map<String, Object> conversation = new HashMap<>();
                conversation.put("participantIds", Arrays.asList(currentUserId, otherUserId));
                conversation.put("createdAt", Timestamp.now());
                conversation.put("lastMessage", "");
                conversation.put("lastMessageTime", Timestamp.now());
                conversation.put("lastMessageSenderId", "");

                if (rentalId != null) {
                    conversation.put("rentalId", rentalId);
                }

                // Initialize unread counts
                Map<String, Integer> unreadCounts = new HashMap<>();
                unreadCounts.put(currentUserId, 0);
                unreadCounts.put(otherUserId, 0);
                conversation.put("unreadCounts", unreadCounts);

                return conversationRef.set(conversation)
                        .continueWith(setTask -> {
                            if (!setTask.isSuccessful()) {
                                throw setTask.getException();
                            }
                            Log.d(TAG, "New conversation created: " + conversationId);
                            return conversationId;
                        });
            }
        });
    }

    /**
     * Generate consistent conversation ID from two user IDs
     */
    private String generateConversationId(String userId1, String userId2) {
        List<String> ids = Arrays.asList(userId1, userId2);
        ids.sort(String::compareTo);
        return ids.get(0) + "_" + ids.get(1);
    }

    /**
     * Send a message in a conversation
     */
    public Task<Void> sendMessage(String conversationId, String messageText, String receiverId) {
        String senderId = auth.getCurrentUser().getUid();

        // Create message data
        Map<String, Object> message = new HashMap<>();
        message.put("senderId", senderId);
        message.put("receiverId", receiverId);
        message.put("message", messageText);
        message.put("timestamp", Timestamp.now());
        message.put("read", false);

        // Use batch write to update conversation and add message
        WriteBatch batch = db.batch();

        // Add message to subcollection
        DocumentReference messageRef = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document();
        batch.set(messageRef, message);

        // Update conversation metadata
        DocumentReference conversationRef = db.collection("conversations").document(conversationId);
        Map<String, Object> conversationUpdate = new HashMap<>();
        conversationUpdate.put("lastMessage", messageText);
        conversationUpdate.put("lastMessageTime", Timestamp.now());
        conversationUpdate.put("lastMessageSenderId", senderId);
        conversationUpdate.put("unreadCounts." + receiverId, FieldValue.increment(1));

        batch.update(conversationRef, conversationUpdate);

        return batch.commit();
    }

    /**
     * Get messages for a conversation (ordered by timestamp)
     */
    public Query getMessages(String conversationId) {
        return db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING);
    }

    /**
     * Mark messages as read in a conversation
     */
    public Task<Void> markMessagesAsRead(String conversationId) {
        String currentUserId = auth.getCurrentUser().getUid();

        // Reset unread count for current user
        return db.collection("conversations")
                .document(conversationId)
                .update("unreadCounts." + currentUserId, 0);
    }

    /**
     * Get all conversations for current user
     */
    public Query getUserConversations() {
        String currentUserId = auth.getCurrentUser().getUid();

        return db.collection("conversations")
                .whereArrayContains("participantIds", currentUserId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING);
    }

    /**
     * Get total unread message count for current user
     */
    public Task<Integer> getTotalUnreadCount() {
        String currentUserId = auth.getCurrentUser().getUid();

        return db.collection("conversations")
                .whereArrayContains("participantIds", currentUserId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        return 0;
                    }

                    int totalUnread = 0;
                    QuerySnapshot snapshots = task.getResult();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Map<String, Object> unreadCounts = (Map<String, Object>) doc.get("unreadCounts");
                        if (unreadCounts != null && unreadCounts.containsKey(currentUserId)) {
                            Object count = unreadCounts.get(currentUserId);
                            if (count instanceof Long) {
                                totalUnread += ((Long) count).intValue();
                            } else if (count instanceof Integer) {
                                totalUnread += (Integer) count;
                            }
                        }
                    }

                    return totalUnread;
                });
    }

    /**
     * Delete a conversation
     */
    public Task<Void> deleteConversation(String conversationId) {
        return db.collection("conversations")
                .document(conversationId)
                .delete();
    }
}