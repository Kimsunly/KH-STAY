package com.khstay.myapplication.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.firebase.NotificationService;
import com.khstay.myapplication.ui.chat.adapter.MessageAdapter;
import com.khstay.myapplication.ui.chat.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";
    public static final String EXTRA_OTHER_USER_ID = "OTHER_USER_ID";
    public static final String EXTRA_OTHER_USER_NAME = "OTHER_USER_NAME";
    public static final String EXTRA_OTHER_USER_PHOTO = "OTHER_USER_PHOTO";
    public static final String EXTRA_RENTAL_ID = "RENTAL_ID";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack;
    private TextView tvOtherUserName;
    private ImageView ivOtherUserAvatar;
    private LinearLayout emptyState;

    private MessageAdapter messageAdapter;
    private List<Message> messageList = new ArrayList<>();

    private NotificationService.ChatService chatService;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String conversationId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhoto;
    private String rentalId;
    private String currentUserId;

    private ListenerRegistration messagesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        otherUserName = getIntent().getStringExtra(EXTRA_OTHER_USER_NAME);
        otherUserPhoto = getIntent().getStringExtra(EXTRA_OTHER_USER_PHOTO);
        rentalId = getIntent().getStringExtra(EXTRA_RENTAL_ID);

        if (TextUtils.isEmpty(otherUserId)) {
            Toast.makeText(this, "Invalid user data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        chatService = new NotificationService.ChatService();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadOrCreateConversation();
    }

    private void initializeViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);
        tvOtherUserName = findViewById(R.id.tvOtherUserName);
        ivOtherUserAvatar = findViewById(R.id.ivOtherUserAvatar);
        emptyState = findViewById(R.id.emptyState);

        tvOtherUserName.setText(otherUserName != null ? otherUserName : "User");

        if (!TextUtils.isEmpty(otherUserPhoto)) {
            Glide.with(this)
                    .load(otherUserPhoto)
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(ivOtherUserAvatar);
        }
    }

    private void setupRecyclerView() {
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnSend.setOnClickListener(v -> sendMessage());

        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });
    }

    private void loadOrCreateConversation() {
        etMessage.setEnabled(false);
        btnSend.setEnabled(false);

        chatService.getOrCreateConversation(otherUserId, rentalId)
                .addOnSuccessListener(convId -> {
                    conversationId = convId;
                    Log.d(TAG, "Conversation ID: " + conversationId);
                    listenToMessages();
                    markMessagesAsRead();

                    etMessage.setEnabled(true);
                    btnSend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create/load conversation", e);
                    Toast.makeText(this, "Failed to load chat: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    etMessage.setEnabled(true);
                    btnSend.setEnabled(true);
                });
    }

    private void listenToMessages() {
        if (TextUtils.isEmpty(conversationId)) return;

        Query query = chatService.getMessages(conversationId);

        messagesListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed", error);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    Message message = dc.getDocument().toObject(Message.class);
                    message.setId(dc.getDocument().getId());

                    switch (dc.getType()) {
                        case ADDED:
                            messageList.add(message);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            rvMessages.scrollToPosition(messageList.size() - 1);
                            break;
                        case MODIFIED:
                            break;
                        case REMOVED:
                            break;
                    }
                }

                if (messageList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvMessages.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvMessages.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void sendMessage() {
        String messageText = etMessage.getText().toString().trim();

        if (TextUtils.isEmpty(messageText)) {
            return;
        }

        if (TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, "Please wait, loading conversation...", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);
        etMessage.setEnabled(false);

        chatService.sendMessage(conversationId, messageText, otherUserId)
                .addOnSuccessListener(aVoid -> {
                    etMessage.setText("");
                    etMessage.setEnabled(true);
                    btnSend.setEnabled(true);
                    etMessage.requestFocus();
                    Log.d(TAG, "Message sent successfully");

                    rvMessages.post(() -> {
                        if (messageAdapter.getItemCount() > 0) {
                            rvMessages.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    etMessage.setEnabled(true);
                    btnSend.setEnabled(true);
                    Toast.makeText(this, "Failed to send message: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to send message", e);
                });
    }

    private void markMessagesAsRead() {
        if (TextUtils.isEmpty(conversationId)) return;

        chatService.markMessagesAsRead(conversationId)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Messages marked as read");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to mark messages as read", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesListener.remove();
        }
    }
}