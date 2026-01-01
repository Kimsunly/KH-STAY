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
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages;
    private String currentUserId;
    private OnMessageActionListener listener;
    private SimpleDateFormat timeFormat;

    public interface OnMessageActionListener {
        void onEditMessage(Message message);
        void onDeleteMessage(Message message);
    }

    public MessageAdapter(List<Message> messages, String currentUserId, OnMessageActionListener listener) {
        this.messages = messages;
        this.currentUserId = currentUserId;
        this.listener = listener;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (message.getSenderId().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // Sent Message ViewHolder
    class SentMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvEdited;
        LinearLayout messageContainer;

        SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvEdited = itemView.findViewById(R.id.tvEdited);
            messageContainer = itemView.findViewById(R.id.messageContainer);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());

            if (message.getTimestamp() != null) {
                tvTime.setText(timeFormat.format(message.getTimestamp().toDate()));
            }

            // Show "edited" indicator
            if (message.isEdited() && tvEdited != null) {
                tvEdited.setVisibility(View.VISIBLE);
            } else if (tvEdited != null) {
                tvEdited.setVisibility(View.GONE);
            }

            // Show deleted message styling
            if (message.isDeleted()) {
                tvMessage.setAlpha(0.5f);
                tvMessage.setTextColor(itemView.getContext().getResources().getColor(R.color.text_hint));
                messageContainer.setOnLongClickListener(null);
            } else {
                tvMessage.setAlpha(1.0f);
                tvMessage.setTextColor(itemView.getContext().getResources().getColor(R.color.white));

                // Long press for edit/delete options
                messageContainer.setOnLongClickListener(v -> {
                    showMessageOptions(message);
                    return true;
                });
            }
        }

        private void showMessageOptions(Message message) {
            String[] options = {"Edit", "Delete"};

            new androidx.appcompat.app.AlertDialog.Builder(itemView.getContext())
                    .setTitle("Message Options")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0 && listener != null) {
                            listener.onEditMessage(message);
                        } else if (which == 1 && listener != null) {
                            listener.onDeleteMessage(message);
                        }
                    })
                    .show();
        }
    }

    // Received Message ViewHolder
    class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvEdited;

        ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvEdited = itemView.findViewById(R.id.tvEdited);
        }

        void bind(Message message) {
            tvMessage.setText(message.getMessage());

            if (message.getTimestamp() != null) {
                tvTime.setText(timeFormat.format(message.getTimestamp().toDate()));
            }

            // Show "edited" indicator
            if (message.isEdited() && tvEdited != null) {
                tvEdited.setVisibility(View.VISIBLE);
            } else if (tvEdited != null) {
                tvEdited.setVisibility(View.GONE);
            }

            // Show deleted message styling
            if (message.isDeleted()) {
                tvMessage.setAlpha(0.5f);
                tvMessage.setTextColor(itemView.getContext().getResources().getColor(R.color.text_hint));
            } else {
                tvMessage.setAlpha(1.0f);
            }
        }
    }
}