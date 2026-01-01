package com.khstay.myapplication.ui.chat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.firebase.NotificationService;
import com.khstay.myapplication.ui.chat.adapter.ConversationAdapter;
import com.khstay.myapplication.ui.chat.model.Conversation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConversationsActivity extends AppCompatActivity {

    private static final String TAG = "ConversationsActivity";

    private RecyclerView rvConversations;
    private ImageButton btnBack;
    private LinearLayout emptyState;

    private ConversationAdapter conversationAdapter;
    private List<Conversation> conversationList = new ArrayList<>();

    private NotificationService.ChatService chatService;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentUserId;

    private ListenerRegistration conversationsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        chatService = new NotificationService.ChatService();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadConversations();
    }

    private void initializeViews() {
        rvConversations = findViewById(R.id.rvConversations);
        btnBack = findViewById(R.id.btnBack);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        conversationAdapter = new ConversationAdapter(conversationList, conversation -> {
            // Open chat with this conversation
            Intent intent = new Intent(this, ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, conversation.getOtherUserId());
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, conversation.getOtherUserName());
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_PHOTO, conversation.getOtherUserPhoto());
            startActivity(intent);
        });

        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(conversationAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadConversations() {
        Query query = chatService.getUserConversations();
        if (query == null) return;

        conversationsListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed", error);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    Conversation conversation = dc.getDocument().toObject(Conversation.class);
                    conversation.setId(dc.getDocument().getId());

                    // ✅ CHECK IF DELETED FOR CURRENT USER
                    Map<String, Object> deletedFor = (Map<String, Object>)
                            dc.getDocument().get("deletedFor");

                    if (deletedFor != null && deletedFor.containsKey(currentUserId)) {
                        Boolean isDeleted = (Boolean) deletedFor.get(currentUserId);
                        if (isDeleted != null && isDeleted) {
                            // Skip this conversation - it's deleted for this user
                            // But remove it from list if it was already there
                            for (int i = 0; i < conversationList.size(); i++) {
                                if (conversationList.get(i).getId().equals(conversation.getId())) {
                                    conversationList.remove(i);
                                    conversationAdapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                            continue;
                        }
                    }

                    // Determine the other user ID
                    String otherUserId = null;
                    for (String participantId : conversation.getParticipantIds()) {
                        if (!participantId.equals(currentUserId)) {
                            otherUserId = participantId;
                            break;
                        }
                    }

                    if (otherUserId != null) {
                        conversation.setOtherUserId(otherUserId);
                        loadOtherUserInfo(conversation, otherUserId);
                    }

                    switch (dc.getType()) {
                        case ADDED:
                            conversationList.add(0, conversation);
                            conversationAdapter.notifyItemInserted(0);
                            break;
                        case MODIFIED:
                            // Find and update
                            for (int i = 0; i < conversationList.size(); i++) {
                                if (conversationList.get(i).getId().equals(conversation.getId())) {
                                    conversationList.set(i, conversation);
                                    conversationAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                            break;
                        case REMOVED:
                            // Find and remove
                            for (int i = 0; i < conversationList.size(); i++) {
                                if (conversationList.get(i).getId().equals(conversation.getId())) {
                                    conversationList.remove(i);
                                    conversationAdapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                            break;
                    }
                }

                // Show/hide empty state
                if (conversationList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvConversations.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvConversations.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadOtherUserInfo(Conversation conversation, String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        String photo = doc.getString("photoUrl");

                        conversation.setOtherUserName(name != null ? name : "User");
                        conversation.setOtherUserPhoto(photo);

                        // Update adapter
                        conversationAdapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conversationsListener != null) {
            conversationsListener.remove();
        }
    }
}