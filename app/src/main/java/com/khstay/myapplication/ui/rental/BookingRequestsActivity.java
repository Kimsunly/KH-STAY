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
import com.khstay.myapplication.ui.rental.adapters.BookingRequestAdapter;
import com.khstay.myapplication.ui.rental.model.BookingRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingRequestsActivity extends AppCompatActivity {

    private static final String TAG = "BookingRequestsActivity";

    private RecyclerView rvBookingRequests;
    private ImageButton btnBack;
    private LinearLayout emptyState;

    private BookingRequestAdapter bookingAdapter;
    private final List<BookingRequest> bookingList = new ArrayList<>();

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private BookingService bookingService;
    private String currentUserId;

    private ListenerRegistration bookingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_requests);

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
        loadBookingRequests();
    }

    private void initializeViews() {
        rvBookingRequests = findViewById(R.id.rvBookingRequests);
        btnBack = findViewById(R.id.btnBack);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupRecyclerView() {
        bookingAdapter = new BookingRequestAdapter(bookingList, new BookingRequestAdapter.OnBookingActionListener() {
            @Override
            public void onApprove(BookingRequest booking) {
                showApproveDialog(booking);
            }

            @Override
            public void onReject(BookingRequest booking) {
                showRejectDialog(booking);
            }

            @Override
            public void onDelete(BookingRequest booking) {
                showDeleteDialog(booking);
            }
        });

        rvBookingRequests.setLayoutManager(new LinearLayoutManager(this));
        rvBookingRequests.setAdapter(bookingAdapter);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBookingRequests() {
        // Load bookings for properties owned by current user
        Query query = db.collection("bookings")
                .whereEqualTo("ownerId", currentUserId)
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
                    rvBookingRequests.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    rvBookingRequests.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showApproveDialog(BookingRequest booking) {
        new AlertDialog.Builder(this)
                .setTitle("Approve Booking")
                .setMessage("Approve this booking request?\n\nThis will change the property status to 'pending' (booked) and notify the guest.")
                .setPositiveButton("Approve", (dialog, which) -> {
                    approveBooking(booking);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRejectDialog(BookingRequest booking) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Booking")
                .setMessage("Are you sure you want to reject this booking request? The guest will be notified.")
                .setPositiveButton("Reject", (dialog, which) -> {
                    rejectBooking(booking);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog(BookingRequest booking) {
        String status = booking.getStatus() != null ? booking.getStatus() : "pending";
        String message;

        if ("pending".equals(status)) {
            message = "Delete this pending booking request?\n\nThe guest will be notified.";
        } else {
            message = "Remove this " + status + " booking from your list?\n\nThis cannot be undone.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteBooking(booking);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void approveBooking(BookingRequest booking) {
        // Use BookingService to update status with notification
        bookingService.updateBookingStatus(
                        booking.getId(),
                        "approved",
                        booking.getUserId(),
                        booking.getRentalTitle()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking approved! Guest has been notified.",
                            Toast.LENGTH_SHORT).show();

                    // Update rental house status to "pending" (booked)
                    updateRentalStatus(booking.getRentalId(), "pending");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to approve booking", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Approve failed", e);
                });
    }

    private void rejectBooking(BookingRequest booking) {
        // Use BookingService to update status with notification
        bookingService.updateBookingStatus(
                        booking.getId(),
                        "rejected",
                        booking.getUserId(),
                        booking.getRentalTitle()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking rejected. Guest has been notified.",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reject booking", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Reject failed", e);
                });
    }

    private void deleteBooking(BookingRequest booking) {
        // Delete booking and notify guest
        bookingService.deleteBookingWithNotification(
                        booking.getId(),
                        booking.getUserId(),
                        "owner",
                        booking.getRentalTitle()
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking removed successfully",
                            Toast.LENGTH_SHORT).show();

                    // If approved booking was deleted, revert rental to active
                    if ("approved".equals(booking.getStatus())) {
                        updateRentalStatus(booking.getRentalId(), "active");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete booking", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Delete failed", e);
                });
    }

    private void updateRentalStatus(String rentalId, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        db.collection("rental_houses")
                .document(rentalId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Rental status updated to: " + status);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update rental status", e);
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