package com.khstay.myapplication.ui.chat.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.khstay.myapplication.R;
import com.khstay.myapplication.ui.chat.model.Conversation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    private final List<Conversation> conversations;
    private final OnConversationClickListener listener;
    private final SimpleDateFormat timeFormat;
    private final String currentUserId;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }

    public ConversationAdapter(List<Conversation> conversations, OnConversationClickListener listener) {
        this.conversations = conversations;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        Conversation conversation = conversations.get(position);

        // Set user name
        holder.tvUserName.setText(conversation.getOtherUserName() != null ?
                conversation.getOtherUserName() : "User");

        // Set last message
        holder.tvLastMessage.setText(!TextUtils.isEmpty(conversation.getLastMessage()) ?
                conversation.getLastMessage() : "No messages yet");

        // Set time
        if (conversation.getLastMessageTime() != null) {
            holder.tvTime.setText(formatTime(conversation.getLastMessageTime()));
        }

        // Set avatar
        if (!TextUtils.isEmpty(conversation.getOtherUserPhoto())) {
            Glide.with(holder.itemView.getContext())
                    .load(conversation.getOtherUserPhoto())
                    .placeholder(R.drawable.ic_avatar_placeholder)
                    .error(R.drawable.ic_avatar_placeholder)
                    .circleCrop()
                    .into(holder.ivUserAvatar);
        } else {
            holder.ivUserAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
        }

        // Show unread badge
        int unreadCount = conversation.getUnreadCountForUser(currentUserId);
        if (unreadCount > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(unreadCount));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        // Click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    private String formatTime(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        return timeFormat.format(date);
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivUserAvatar;
        TextView tvUserName, tvLastMessage, tvTime, tvUnreadBadge;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }
}