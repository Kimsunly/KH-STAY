package com.khstay.myapplication.ui.chat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khstay.myapplication.R;
import com.khstay.myapplication.ui.chat.model.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat;

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        boolean isSentByCurrentUser = message.getSenderId().equals(currentUserId);

        if (isSentByCurrentUser) {
            // Show sent message (right side)
            holder.sentMessageContainer.setVisibility(View.VISIBLE);
            holder.receivedMessageContainer.setVisibility(View.GONE);

            holder.tvSentMessage.setText(message.getMessage());
            holder.tvSentTime.setText(formatTime(message.getTimestamp()));
        } else {
            // Show received message (left side)
            holder.sentMessageContainer.setVisibility(View.GONE);
            holder.receivedMessageContainer.setVisibility(View.VISIBLE);

            holder.tvReceivedMessage.setText(message.getMessage());
            holder.tvReceivedTime.setText(formatTime(message.getTimestamp()));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    private String formatTime(com.google.firebase.Timestamp timestamp) {
        if (timestamp == null) return "";
        Date date = timestamp.toDate();
        return timeFormat.format(date);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout sentMessageContainer, receivedMessageContainer;
        TextView tvSentMessage, tvSentTime;
        TextView tvReceivedMessage, tvReceivedTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            sentMessageContainer = itemView.findViewById(R.id.sentMessageContainer);
            receivedMessageContainer = itemView.findViewById(R.id.receivedMessageContainer);
            tvSentMessage = itemView.findViewById(R.id.tvSentMessage);
            tvSentTime = itemView.findViewById(R.id.tvSentTime);
            tvReceivedMessage = itemView.findViewById(R.id.tvReceivedMessage);
            tvReceivedTime = itemView.findViewById(R.id.tvReceivedTime);
        }
    }
}