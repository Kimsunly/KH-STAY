package com.khstay.myapplication.ui.rental.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khstay.myapplication.R;
import com.khstay.myapplication.ui.rental.model.BookingRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for renters/guests to view their own bookings
 */
public class MyBookingAdapter extends RecyclerView.Adapter<MyBookingAdapter.BookingViewHolder> {

    private final List<BookingRequest> bookings;
    private final OnBookingActionListener listener;
    private final SimpleDateFormat dateFormat;

    public interface OnBookingActionListener {
        void onCancel(BookingRequest booking);
        void onDelete(BookingRequest booking);
        void onViewDetails(BookingRequest booking);
    }

    public MyBookingAdapter(List<BookingRequest> bookings, OnBookingActionListener listener) {
        this.bookings = bookings;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        BookingRequest booking = bookings.get(position);

        // Property title
        holder.tvRentalTitle.setText(booking.getRentalTitle() != null ?
                booking.getRentalTitle() : "Property");

        // Price
        double price = booking.getRentalPrice() != null ? booking.getRentalPrice() : 0.0;
        holder.tvRentalPrice.setText(String.format("$%.0f/month", price));

        // Dates
        String checkIn = booking.getCheckInDate() != null ?
                dateFormat.format(booking.getCheckInDate().toDate()) : "N/A";
        String checkOut = booking.getCheckOutDate() != null ?
                dateFormat.format(booking.getCheckOutDate().toDate()) : "N/A";
        int days = booking.getNumberOfDays() != null ? booking.getNumberOfDays() : 0;

        holder.tvBookingDates.setText("📅 " + checkIn + " → " + checkOut + " (" + days + " days)");

        // Total price
        double total = booking.getTotalPrice() != null ? booking.getTotalPrice() : 0.0;
        holder.tvTotalPrice.setText(String.format("Total: $%.2f", total));

        // Status
        String status = booking.getStatus() != null ? booking.getStatus() : "pending";
        holder.tvStatus.setText(status.substring(0, 1).toUpperCase() + status.substring(1));

        // Status styling and button visibility
        if (status.equals("pending")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
            holder.tvStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.text_secondary));
            holder.tvStatusMessage.setText("⏳ Waiting for owner approval");
            holder.tvStatusMessage.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.text_secondary));

            // Show cancel button for pending bookings
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);

        } else if (status.equals("approved")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
            holder.tvStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.success_green));
            holder.tvStatusMessage.setText("✅ Booking confirmed! Owner approved your request");
            holder.tvStatusMessage.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.success_green));

            // Show cancel button for approved bookings (can still cancel before check-in)
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.GONE);

        } else if (status.equals("rejected")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_archived);
            holder.tvStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.error_red));
            holder.tvStatusMessage.setText("❌ Owner declined this booking request");
            holder.tvStatusMessage.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.error_red));

            // Show delete button for rejected bookings
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);

        } else if (status.equals("cancelled")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_archived);
            holder.tvStatus.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.text_hint));
            holder.tvStatusMessage.setText("🚫 You cancelled this booking");
            holder.tvStatusMessage.setTextColor(holder.itemView.getContext()
                    .getResources().getColor(R.color.text_hint));

            // Show delete button for cancelled bookings
            holder.actionButtons.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        }

        // Notes
        if (!TextUtils.isEmpty(booking.getNotes())) {
            holder.tvNotes.setVisibility(View.VISIBLE);
            holder.tvNotes.setText("💬 " + booking.getNotes());
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }

        // Action buttons
        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel(booking);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(booking);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onViewDetails(booking);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvRentalTitle, tvRentalPrice, tvBookingDates;
        TextView tvTotalPrice, tvStatus, tvStatusMessage, tvNotes;
        LinearLayout actionButtons;
        Button btnCancel, btnDelete;

        BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRentalTitle = itemView.findViewById(R.id.tvRentalTitle);
            tvRentalPrice = itemView.findViewById(R.id.tvRentalPrice);
            tvBookingDates = itemView.findViewById(R.id.tvBookingDates);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvStatusMessage = itemView.findViewById(R.id.tvStatusMessage);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            actionButtons = itemView.findViewById(R.id.actionButtons);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}