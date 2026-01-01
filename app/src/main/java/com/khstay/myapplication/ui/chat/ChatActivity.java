package com.khstay.myapplication.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.khstay.myapplication.data.firebase.ChatService;
import com.khstay.myapplication.ui.chat.adapter.MessageAdapter;
import com.khstay.myapplication.ui.chat.model.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnMessageActionListener {

    private static final String TAG = "ChatActivity";

    public static final String EXTRA_OTHER_USER_ID   = "OTHER_USER_ID";
    public static final String EXTRA_OTHER_USER_NAME = "OTHER_USER_NAME";
    public static final String EXTRA_OTHER_USER_PHOTO= "OTHER_USER_PHOTO";
    public static final String EXTRA_RENTAL_ID       = "RENTAL_ID";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend, btnBack, btnMore;
    private TextView tvOtherUserName;
    private ImageView ivOtherUserAvatar;
    private LinearLayout emptyState;

    private MessageAdapter messageAdapter;
    private final List<Message> messageList = new ArrayList<>();

    private ChatService chatService;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String conversationId;
    private String otherUserId;
    private String otherUserName;
    private String otherUserPhoto;
    private String rentalId;
    private String currentUserId;

    private ListenerRegistration messagesListener;

    // Edit mode
    private boolean isEditMode = false;
    private String editingMessageId = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Retrieve intent extras (from deep link notification or navigation)
        otherUserId   = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        otherUserName = getIntent().getStringExtra(EXTRA_OTHER_USER_NAME);
        otherUserPhoto= getIntent().getStringExtra(EXTRA_OTHER_USER_PHOTO);
        rentalId      = getIntent().getStringExtra(EXTRA_RENTAL_ID);

        // Basic validation for essential data
        if (TextUtils.isEmpty(otherUserId)) {
            Toast.makeText(this, "Invalid chat target: missing user id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();
        chatService = new ChatService();

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();

        // UI setup
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadOrCreateConversation();

        // Back-press handling (cancel edit mode first)
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isEditMode) {
                    cancelEditMode();
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, onBackPressedCallback);
    }

    private void initializeViews() {
        rvMessages      = findViewById(R.id.rvMessages);
        etMessage       = findViewById(R.id.etMessage);
        btnSend         = findViewById(R.id.btnSend);
        btnBack         = findViewById(R.id.btnBack);
        btnMore         = findViewById(R.id.btnMore);
        tvOtherUserName = findViewById(R.id.tvOtherUserName);
        ivOtherUserAvatar = findViewById(R.id.ivOtherUserAvatar);
        emptyState      = findViewById(R.id.emptyState);

        tvOtherUserName.setText(!TextUtils.isEmpty(otherUserName) ? otherUserName : "User");

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
        messageAdapter = new MessageAdapter(messageList, currentUserId, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Ensures new messages appear at the bottom
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish()); // Finish the activity when back is clicked

        btnSend.setOnClickListener(v -> {
            if (isEditMode) {
                editMessage();
            } else {
                sendMessage();
            }
        });

        btnMore.setOnClickListener(v -> showConversationOptions());

        // Handle keyboard action (Done/Send)
        etMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                if (isEditMode) {
                    editMessage();
                } else {
                    sendMessage();
                }
                return true;
            }
            return false;
        });
    }

    private void loadOrCreateConversation() {
        // Disable input while loading
        etMessage.setEnabled(false);
        btnSend.setEnabled(false);

        chatService.getOrCreateConversation(otherUserId, rentalId)
                .addOnSuccessListener(convId -> {
                    conversationId = convId;
                    Log.d(TAG, "Conversation ID: " + conversationId);
                    listenToMessages();     // Start listening
                    markMessagesAsRead();   // Mark existing messages as read

                    // Re-enable input
                    etMessage.setEnabled(true);
                    btnSend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create/load conversation", e);
                    Toast.makeText(this, "Failed to load chat: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();

                    // Re-enable input even on failure
                    etMessage.setEnabled(true);
                    btnSend.setEnabled(true);
                });
    }

    private void listenToMessages() {
        if (TextUtils.isEmpty(conversationId)) {
            Log.w(TAG, "Cannot listen to messages: conversationId is empty.");
            return;
        }

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
                            for (int i = 0; i < messageList.size(); i++) {
                                if (TextUtils.equals(messageList.get(i).getId(), message.getId())) {
                                    messageList.set(i, message);
                                    messageAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                            break;

                        case REMOVED:
                            for (int i = 0; i < messageList.size(); i++) {
                                if (TextUtils.equals(messageList.get(i).getId(), message.getId())) {
                                    messageList.remove(i);
                                    messageAdapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                            break;
                    }
                }

                // Empty state visibility
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

        // Disable input to prevent duplicate sends
        btnSend.setEnabled(false);
        etMessage.setEnabled(false);

        chatService.sendMessage(conversationId, messageText, otherUserId)
                .addOnSuccessListener(aVoid -> {
                    etMessage.setText("");   // Clear input
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

    private void editMessage() {
        if (editingMessageId == null) return;

        String newText = etMessage.getText().toString().trim();
        if (TextUtils.isEmpty(newText)) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);
        etMessage.setEnabled(false);

        chatService.editMessage(conversationId, editingMessageId, newText)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Message edited", Toast.LENGTH_SHORT).show();
                    cancelEditMode();
                })
                .addOnFailureListener(e -> {
                    etMessage.setEnabled(true);
                    btnSend.setEnabled(true);
                    Toast.makeText(this, "Failed to edit message: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to edit message", e);
                });
    }

    private void markMessagesAsRead() {
        if (TextUtils.isEmpty(conversationId)) return;

        chatService.markMessagesAsRead(conversationId)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Messages marked as read"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to mark messages as read", e));
    }

    private void showConversationOptions() {
        String[] options = {"Delete Conversation"};

        new AlertDialog.Builder(this)
                .setTitle("Conversation Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        confirmDeleteConversation();
                    }
                })
                .show();
    }

    private void confirmDeleteConversation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Conversation")
                .setMessage("Are you sure you want to delete this conversation? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteConversation())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteConversation() {
        if (TextUtils.isEmpty(conversationId)) {
            Toast.makeText(this, "Cannot delete, conversation not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        chatService.deleteConversation(conversationId)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Conversation deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete conversation: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to delete conversation", e);
                });
    }

    @Override
    public void onEditMessage(Message message) {
        if (!TextUtils.equals(message.getSenderId(), currentUserId)) {
            return;
        }

        isEditMode = true;
        editingMessageId = message.getId();
        etMessage.setText(message.getMessage());
        etMessage.setSelection(etMessage.getText().length());
        etMessage.requestFocus();
        btnSend.setImageResource(R.drawable.ic_check);
        Toast.makeText(this, "Editing message (tap send to save)", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteMessage(Message message) {
        if (!TextUtils.equals(message.getSenderId(), currentUserId)) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    chatService.deleteMessage(conversationId, message.getId())
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to delete message: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cancelEditMode() {
        isEditMode = false;
        editingMessageId = null;
        etMessage.setText("");
        etMessage.setEnabled(true);
        btnSend.setEnabled(true);
        btnSend.setImageResource(R.drawable.ic_send);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }
}
