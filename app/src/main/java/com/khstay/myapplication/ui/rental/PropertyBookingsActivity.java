package com.khstay.myapplication.ui.rental;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.khstay.myapplication.ui.rental.adapters.BookingRequestAdapter;
import com.khstay.myapplication.ui.rental.model.BookingRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyBookingsActivity extends AppCompatActivity {

    private static final String TAG = "PropertyBookingsActivity";

    private RecyclerView rvBookingRequests;
    private ImageButton btnBack;
    private LinearLayout emptyState;
    private TextView tvTitle;

    private BookingRequestAdapter bookingAdapter;
    private final List<BookingRequest> bookingList = new ArrayList<>();

    private FirebaseFirestore db;
    private String rentalId;
    private String rentalTitle;
    private ListenerRegistration bookingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_bookings);

        db = FirebaseFirestore.getInstance();

        rentalId = getIntent().getStringExtra("RENTAL_ID");
        rentalTitle = getIntent().getStringExtra("RENTAL_TITLE");

        if (rentalId == null) {
            Toast.makeText(this, "Property ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        setupClickListeners();
        loadBookingRequests();
    }

    private void initializeViews() {
        rvBookingRequests = findViewById(R.id.rvBookingRequests);
        btnBack = findViewById(R.id.btnBack);
        emptyState = findViewById(R.id.emptyState);
        tvTitle = findViewById(R.id.tvTitle);

        if (tvTitle != null && rentalTitle != null) {
            tvTitle.setText("Bookings: " + rentalTitle);
        }
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

    private void showDeleteDialog(BookingRequest booking) {
        String status = booking.getStatus() != null ? booking.getStatus() : "pending";
        String message;

        if ("pending".equals(status)) {
            message = "Delete this pending booking request?\n\nThe guest will be notified.";
        } else {
            message = "Remove this " + status + " booking from the list?\n\nThis cannot be undone.";
        }

        new AlertDialog.Builder(this)
                .setTitle("Delete Booking")
                .setMessage(message)
                .setPositiveButton("Delete", (dialog, which) -> deleteBooking(booking))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBooking(BookingRequest booking) {
        db.collection("bookings")
                .document(booking.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking removed successfully", Toast.LENGTH_SHORT).show();

                    // If approved booking was deleted, revert rental to active
                    if ("approved".equals(booking.getStatus())) {
                        updateRentalStatus(rentalId, "active");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete booking", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Delete failed", e);
                });
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadBookingRequests() {
        Query query = db.collection("bookings")
                .whereEqualTo("rentalId", rentalId)
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
                            for (int i = 0; i < bookingList.size(); i++) {
                                if (bookingList.get(i).getId().equals(booking.getId())) {
                                    bookingList.set(i, booking);
                                    bookingAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                            break;
                        case REMOVED:
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
                .setMessage("Approve this booking request?\n\nThis will change the property status to 'pending'.")
                .setPositiveButton("Approve", (dialog, which) -> approveBooking(booking))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRejectDialog(BookingRequest booking) {
        new AlertDialog.Builder(this)
                .setTitle("Reject Booking")
                .setMessage("Reject this booking request?")
                .setPositiveButton("Reject", (dialog, which) -> rejectBooking(booking))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void approveBooking(BookingRequest booking) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");

        db.collection("bookings")
                .document(booking.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Booking approved!", Toast.LENGTH_SHORT).show();
                    updateRentalStatus(rentalId, "pending");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to approve", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Approve failed", e);
                });
    }

    private void rejectBooking(BookingRequest booking) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");

        db.collection("bookings")
                .document(booking.getId())
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Booking rejected", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reject", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Reject failed", e);
                });
    }

    private void updateRentalStatus(String rentalId, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        db.collection("rental_houses")
                .document(rentalId)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Log.d(TAG, "Rental status updated"))
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to update rental status", e));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingsListener != null) {
            bookingsListener.remove();
        }
    }
}