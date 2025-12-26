package com.khstay.myapplication.ui.rental;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.RentalRepository;
import com.khstay.myapplication.ui.rental.adapters.RentalAdapter;
import com.khstay.myapplication.ui.rental.model.Rental;

import java.util.ArrayList;
import java.util.List;

public class MyRentFragment extends Fragment {

    private static final String TAG = "MyRentFragment";

    private RecyclerView rvRentals;
    private RentalAdapter rentalAdapter;
    private Chip chipActive, chipPending, chipArchived;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerLoading;
    private TextView tvEmptyState;
    private FloatingActionButton fabAddProperty;

    private final List<Rental> visibleRentals = new ArrayList<>();
    private String currentTab = "active";

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private RentalRepository rentalRepository;

    public MyRentFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_rent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        rvRentals = view.findViewById(R.id.rv_rentals);
        chipActive = view.findViewById(R.id.chip_active);
        chipPending = view.findViewById(R.id.chip_pending);
        chipArchived = view.findViewById(R.id.chip_archived);
        shimmerLoading = view.findViewById(R.id.shimmerLoading);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        fabAddProperty = view.findViewById(R.id.fab_add_property);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        rentalRepository = new RentalRepository();

        setupRecyclerView();
        setupChipListeners();
        setupFAB();

        // Load initial data
        updateChipStates(chipActive);
        fetchRentalsForTab(currentTab);
    }

    private void setupRecyclerView() {
        rvRentals.setLayoutManager(new LinearLayoutManager(getContext()));
        rentalAdapter = new RentalAdapter(getContext(), new ArrayList<>());

        // Enable status badge for MyRent screen
        rentalAdapter.setShowStatusBadge(true);
        rentalAdapter.setShowFavoriteButton(false);

        // Set click listeners
        rentalAdapter.setOnRentalClickListener(new RentalAdapter.OnRentalClickListener() {
            @Override
            public void onRentalClick(Rental rental) {
                openRentalDetail(rental);
            }

            @Override
            public void onMoreClick(Rental rental) {
                showRentalOptionsMenu(rental);
            }

            @Override
            public void onFavoriteClick(Rental rental) {
                // Not used in MyRent screen
            }
        });

        rvRentals.setAdapter(rentalAdapter);
    }

    private void setupChipListeners() {
        chipActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTab = "active";
                updateChipStates(chipActive);
                fetchRentalsForTab(currentTab);
            }
        });

        chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTab = "pending";
                updateChipStates(chipPending);
                fetchRentalsForTab(currentTab);
            }
        });

        chipArchived.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTab = "archived";
                updateChipStates(chipArchived);
                fetchRentalsForTab(currentTab);
            }
        });
    }

    private void setupFAB() {
        fabAddProperty.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PostRentalActivity.class);
            startActivity(intent);
        });
    }

    private void updateChipStates(Chip selectedChip) {
        chipActive.setChecked(false);
        chipPending.setChecked(false);
        chipArchived.setChecked(false);
        selectedChip.setChecked(true);
    }

    private void fetchRentalsForTab(String statusKey) {
        Log.d(TAG, "Fetching rentals for status: " + statusKey);

        // Show skeleton loading
        showSkeletonLoading();

        // Get current user ID
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (uid == null) {
            Log.e(TAG, "User not authenticated");
            showEmptyState("Please log in to view your properties");
            hideSkeletonLoading();
            return;
        }

        // 🔑 CRITICAL: Query with composite index
        // Index required: ownerId (ASC), status (ASC), createdAt (DESC)
        Query query = db.collection("rental_houses")
                .whereEqualTo("ownerId", uid)
                .whereEqualTo("status", statusKey)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        query.get()
                .addOnSuccessListener(qs -> {
                    visibleRentals.clear();

                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Rental r = d.toObject(Rental.class);
                        if (r != null) {
                            r.setId(d.getId());
                            visibleRentals.add(r);
                        }
                    }

                    // Update adapter
                    rentalAdapter.updateList(new ArrayList<>(visibleRentals));

                    // Show/hide empty state
                    if (visibleRentals.isEmpty()) {
                        String message = getEmptyStateMessage(statusKey);
                        showEmptyState(message);
                    } else {
                        hideEmptyState();
                    }

                    // Hide skeleton
                    hideSkeletonLoading();

                    Log.d(TAG, "Loaded " + visibleRentals.size() + " rentals for status=" + statusKey);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fetch failed: " + e.getMessage(), e);

                    // Check if it's an index error
                    if (e.getMessage() != null && e.getMessage().contains("FAILED_PRECONDITION")) {
                        showEmptyState("⚠️ Database index required.\nPlease wait a few minutes and try again.");
                        Toast.makeText(getContext(),
                                "Setting up database. This is a one-time setup.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        showEmptyState("Failed to load properties.\nPlease check your connection and try again.");
                        Toast.makeText(getContext(), "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                    visibleRentals.clear();
                    rentalAdapter.updateList(new ArrayList<>(visibleRentals));
                    hideSkeletonLoading();
                });
    }

    private String getEmptyStateMessage(String status) {
        switch (status) {
            case "active":
                return "No active properties.\n\nTap + to add your first property!";
            case "pending":
                return "No pending bookings.\n\nProperties with booking requests will appear here.";
            case "archived":
                return "No archived properties.\n\nArchived properties will appear here.";
            default:
                return "No properties found.";
        }
    }

    private void showRentalOptionsMenu(Rental rental) {
        if (getContext() == null || getView() == null) return;

        // Find the view holder for the clicked item
        View itemView = null;
        for (int i = 0; i < rvRentals.getChildCount(); i++) {
            View child = rvRentals.getChildAt(i);
            int position = rvRentals.getChildAdapterPosition(child);
            if (position >= 0 && position < visibleRentals.size()) {
                if (visibleRentals.get(position).getId().equals(rental.getId())) {
                    itemView = child;
                    break;
                }
            }
        }

        if (itemView == null) return;

        PopupMenu popup = new PopupMenu(getContext(), itemView.findViewById(R.id.btn_more));
        popup.getMenuInflater().inflate(R.menu.menu_rental_options, popup.getMenu());

        // Show/hide menu items based on status
        boolean isArchived = "archived".equals(rental.getStatus());
        popup.getMenu().findItem(R.id.action_archive).setVisible(!isArchived);
        popup.getMenu().findItem(R.id.action_activate).setVisible(isArchived);

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_view_property) {
                openRentalDetail(rental);
                return true;
            } else if (id == R.id.action_view_bookings) {
                openBookingRequests(rental);
                return true;
            } else if (id == R.id.action_archive) {
                showArchiveDialog(rental);
                return true;
            } else if (id == R.id.action_activate) {
                showReactivateDialog(rental);
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteDialog(rental);
                return true;
            }

            return false;
        });

        popup.show();
    }

    private void openRentalDetail(Rental rental) {
        if (rental.getId() == null || rental.getId().isEmpty()) {
            Toast.makeText(getContext(), "Invalid property ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), RentalHouseDetailActivity.class);
        intent.putExtra(RentalHouseDetailActivity.EXTRA_RENTAL_ID, rental.getId());
        startActivity(intent);
    }

    private void openBookingRequests(Rental rental) {
        Intent intent = new Intent(getActivity(), PropertyBookingsActivity.class);
        intent.putExtra("RENTAL_ID", rental.getId());
        intent.putExtra("RENTAL_TITLE", rental.getTitle());
        startActivity(intent);
    }

    private void showArchiveDialog(Rental rental) {
        new AlertDialog.Builder(getContext())
                .setTitle("Archive Property")
                .setMessage("Archive \"" + rental.getTitle() + "\"?\n\n" +
                        "This will hide it from search results. You can reactivate it later.")
                .setPositiveButton("Archive", (dialog, which) -> archiveProperty(rental))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReactivateDialog(Rental rental) {
        new AlertDialog.Builder(getContext())
                .setTitle("Reactivate Property")
                .setMessage("Reactivate \"" + rental.getTitle() + "\"?\n\n" +
                        "This will make it visible in search results again.")
                .setPositiveButton("Reactivate", (dialog, which) -> reactivateProperty(rental))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteDialog(Rental rental) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Property")
                .setMessage("Permanently delete \"" + rental.getTitle() + "\"?\n\n" +
                        "⚠️ This action cannot be undone!")
                .setPositiveButton("Delete", (dialog, which) -> deleteProperty(rental))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void archiveProperty(Rental rental) {
        rentalRepository.updateRentalStatus(rental.getId(), "archived")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Property archived", Toast.LENGTH_SHORT).show();
                    fetchRentalsForTab(currentTab);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to archive: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Archive failed", e);
                });
    }

    private void reactivateProperty(Rental rental) {
        rentalRepository.updateRentalStatus(rental.getId(), "active")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Property reactivated", Toast.LENGTH_SHORT).show();
                    fetchRentalsForTab(currentTab);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to reactivate: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Reactivate failed", e);
                });
    }

    private void deleteProperty(Rental rental) {
        rentalRepository.deleteRental(rental.getId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Property deleted", Toast.LENGTH_SHORT).show();
                    fetchRentalsForTab(currentTab);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Delete failed", e);
                });
    }

    // ✅ SKELETON LOADING HELPERS
    private void showSkeletonLoading() {
        if (shimmerLoading != null) {
            shimmerLoading.startShimmer();
            shimmerLoading.setVisibility(View.VISIBLE);
        }
        rvRentals.setVisibility(View.GONE);
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void hideSkeletonLoading() {
        if (shimmerLoading != null) {
            shimmerLoading.stopShimmer();
            shimmerLoading.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(String message) {
        if (tvEmptyState != null) {
            tvEmptyState.setText(message);
            tvEmptyState.setVisibility(View.VISIBLE);
        }
        rvRentals.setVisibility(View.GONE);
    }

    private void hideEmptyState() {
        if (tvEmptyState != null) {
            tvEmptyState.setVisibility(View.GONE);
        }
        rvRentals.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Refreshing rentals for tab: " + currentTab);
        fetchRentalsForTab(currentTab);
    }
}