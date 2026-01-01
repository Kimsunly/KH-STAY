package com.khstay.myapplication.ui.rental;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.firebase.BookingService;
import com.khstay.myapplication.ui.rental.adapters.MyBookingAdapter;
import com.khstay.myapplication.ui.rental.model.BookingRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for renters/guests to view and manage their bookings
 */
public class MyBookingsActivity extends AppCompatActivity {

    private static final String TAG = "MyBookingsActivity";

    private RecyclerView rvMyBookings;
    private ImageButton btnBack;
    private LinearLayout emptyState;

    private MyBookingAdapter bookingAdapter;
    private final List<BookingRequest> bookingList = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private BookingService bookingService;
    private String currentUserId;

    private ListenerRegistration bookingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_booking);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        bookingService = new BookingService();

        if (auth.getCurrentUser() == null) {
            finish();
            return;
        }

        currentUserId = auth.getCurrentUser().getUid();

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadMyBookings();
    }

    private void initializeViews() {
        rvMyBookings = findViewById(R.id.rvMyBookings);
        btnBack = findViewById(R.id.btnBack);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        bookingAdapter = new MyBookingAdapter(bookingList, new MyBookingAdapter.OnBookingActionListener() {
            @Override
            public void onCancel(BookingRequest booking) {
                showCancelDialog(booking);
            }

            @Override
            public void onDelete(BookingRequest booking) {
                showDeleteDialog(booking);
            }

            @Override
            public void onViewDetails(BookingRequest booking) {
                // Optional: Open detail view
                Toast.makeText(MyBookingsActivity.this,
                        "View details for: " + booking.getRentalTitle(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        rvMyBookings.setLayoutManager(new LinearLayoutManager(this));
        rvMyBookings.setAdapter(bookingAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadMyBookings() {
        // Load bookings made by current user
        Query query = db.collection("bookings")
                .whereEqualTo("userId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        bookingsListener = query.addSnapshotListener((snapshots, error) -> {
            if (error != null) {
                Log.e(TAG, "Listen failed", error);
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    BookingRequest booking = dc.getDocument().toObject(BookingRequest.class);
                    booking.setId(dc.getDocument().getId());

                    switch (dc.getType()) {
                        case ADDED:
                            bookingList.add(0, booking);
                            bookingAdapter.notifyItemInserted(0);
                            break;
                        case MODIFIED:
                            // Find and update
                            for (int i = 0; i < bookingList.size(); i++) {
                                if (bookingList.get(i).getId().equals(booking.getId())) {
                                    bookingList.set(i, booking);
                                    bookingAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                            break;
                        case REMOVED:
                            // Find and remove
                            for (int i = 0; i < bookingList.size(); i++) {
                                if (bookingList.get(i).getId().equals(booking.getId())) {
                                    bookingList.remove(i);
                                    bookingAdapter.notifyItemRemoved(i);
                                    break;
                                }
                            }
                            break;
                    }
                }

                // Show/hide empty state
                if (bookingList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    rvMyBookings.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvMyBookings.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showCancelDialog(BookingRequest booking) {
        String message;
        if ("pending".equals(booking.getStatus())) {
            message = "Cancel this booking request?\n\nThe owner will be notified.";
        } else if ("approved".equals(booking.getStatus())) {
            message = "Cancel this confirmed booking?\n\nThe property will become available again and the owner will be notified.";
        } else {
            message = "Cancel this booking?";
        }

        new AlertDialog.Builder(this)
                .setTitle("Cancel Booking")
                .setMessage(message)
                .setPositiveButton("Cancel Booking", (dialog, which) -> {
                    cancelBooking(booking);
                })
                .setNegativeButton("Keep Booking", null)
                .show();
    }

    private void showDeleteDialog(BookingRequest booking) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage("Remove this booking from your list?\n\nThis cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteBooking(booking);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cancelBooking(BookingRequest booking) {
        bookingService.cancelBooking(booking.getId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking cancelled. Owner has been notified.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to cancel booking", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Cancel failed", e);
                });
    }

    private void deleteBooking(BookingRequest booking) {
        bookingService.deleteBooking(booking.getId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking removed from your list",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete booking", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Delete failed", e);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingsListener != null) {
            bookingsListener.remove();
        }
    }
}