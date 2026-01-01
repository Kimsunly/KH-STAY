package com.khstay.myapplication.ui.rental.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khstay.myapplication.R;
import com.khstay.myapplication.ui.rental.model.BookingRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class BookingRequestAdapter extends RecyclerView.Adapter<BookingRequestAdapter.BookingViewHolder> {

    private final List<BookingRequest> bookings;
    private final OnBookingActionListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnBookingActionListener {
        void onApprove(BookingRequest booking);
        void onReject(BookingRequest booking);
        void onDelete(BookingRequest booking);
    }

    public BookingRequestAdapter(List<BookingRequest> bookings, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_booking_request, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingRequest booking = bookings.get(position);

        // Property title
        holder.tvRentalTitle.setText(booking.getRentalTitle() != null ?
                booking.getRentalTitle() : "Property");

        // Guest name
        holder.tvGuestName.setText("Guest: " + (booking.getGuestName() != null ?
                booking.getGuestName() : "Unknown"));

        // Contact info
        String contact = "📱 " + (booking.getGuestPhone() != null ? booking.getGuestPhone() : "N/A") +
                " | 📧 " + (booking.getGuestEmail() != null ? booking.getGuestEmail() : "N/A");
        holder.tvGuestContact.setText(contact);

        // Dates
        String checkIn = booking.getCheckInDate() != null ?
                dateFormat.format(booking.getCheckInDate().toDate()) : "N/A";
        String checkOut = booking.getCheckOutDate() != null ?
                dateFormat.format(booking.getCheckOutDate().toDate()) : "N/A";
        int days = booking.getNumberOfDays() != null ? booking.getNumberOfDays() : 0;

        holder.tvBookingDates.setText("📅 " + checkIn + " - " + checkOut +
                " (" + days + " days)");

        // Total price
        double total = booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0;
        holder.tvTotalPrice.setText(String.format("💰 Total: $%.2f", total));

        // Status
        String status = booking.getStatus() != null ? booking.getStatus() : "pending";
        holder.tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

        // Status styling and button visibility
        if (status.equals("pending")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.tvStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.text_secondary));

            // Show approve/reject buttons, hide delete button for pending
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnApprove.setVisibility(View.VISIBLE);
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);

        } else if (status.equals("approved")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
            holder.tvStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.success_green));

            // Hide approve/reject, show delete button
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);

        } else if (status.equals("rejected")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_archived);
            holder.tvStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.error_red));

            // Hide approve/reject, show delete button
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);

        } else if (status.equals("cancelled")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_archived);
            holder.tvStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.text_hint));

            // Show only delete button for cancelled
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnApprove.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        // Notes
        if (!TextUtils.isEmpty(booking.getNotes())) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("Notes: " + booking.getNotes());
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        // Action buttons
        holder.btnApprove.setOnClickListener(v -> {
            if (listener != null) {
                listener.onApprove(booking);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReject(booking);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRentalTitle, tvGuestName, tvGuestContact, tvBookingDates;
        TextView tvTotalPrice, tvStatus, tvNotes;
        LinearLayout actionButtons;
        Button btnApprove, btnReject, btnDelete;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRentalTitle = itemView.findViewById(R.id.tvRentalTitle);
            tvGuestName = itemView.findViewById(R.id.tvGuestName);
            tvGuestContact = itemView.findViewById(R.id.tvGuestContact);
            tvBookingDates = itemView.findViewById(R.id.tvBookingDates);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            actionButtons = itemView.findViewById(R.id.actionButtons);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}