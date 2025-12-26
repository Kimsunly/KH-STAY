
package com.khstay.myapplication.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.khstay.myapplication.R;
import com.khstay.myapplication.common.IntentKeys;
import com.khstay.myapplication.data.repository.UserRepository;
import com.khstay.myapplication.ui.rental.RentalHouseDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class RecentViewedActivity extends AppCompatActivity {

    private ImageView backButton;
    private RecyclerView recentViewedRecyclerView;
    private TextView emptyStateText;

    private FirebaseAuth auth;
    private UserRepository userRepository;
    private RecentViewedAdapter adapter;
    private List<RecentViewedItem> recentItems;

    private ShimmerFrameLayout shimmerLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recent_viewed);

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadRecentViewed();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        recentViewedRecyclerView = findViewById(R.id.recentViewedRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        try {
            shimmerLoading = findViewById(R.id.shimmerLoading);
        } catch (Exception e) {
            shimmerLoading = null;
        }
    }

    private void setupRecyclerView() {
        recentItems = new ArrayList<>();
        adapter = new RecentViewedAdapter(recentItems, this::onItemClick);
        recentViewedRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recentViewedRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadRecentViewed() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showEmptyState(true);
            return;
        }

        // Show loading
        if (shimmerLoading != null) {
            shimmerLoading.startShimmer();
            shimmerLoading.setVisibility(View.VISIBLE);
        }
        recentViewedRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        userRepository.getRecentViewed()
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    recentItems.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        RecentViewedItem item = doc.toObject(RecentViewedItem.class);
                        // Always keep the document ID for fallback navigation
                        item.setId(doc.getId());
                        recentItems.add(item);
                    }
                    adapter.notifyDataSetChanged();

                    // Hide loading
                    if (shimmerLoading != null) {
                        shimmerLoading.stopShimmer();
                        shimmerLoading.setVisibility(View.GONE);
                    }

                    if (recentItems.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        recentViewedRecyclerView.setVisibility(View.VISIBLE);
                        emptyStateText.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Hide loading
                    if (shimmerLoading != null) {
                        shimmerLoading.stopShimmer();
                        shimmerLoading.setVisibility(View.GONE);
                    }
                    showEmptyState(true);
                });
    }

    private void showEmptyState(boolean show) {
        if (show) {
            emptyStateText.setVisibility(View.VISIBLE);
            recentViewedRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recentViewedRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void onItemClick(RecentViewedItem item) {
        // Prefer the canonical rentalId (new docs), fallback to doc id (legacy docs)
        String rentalId = item.getRentalId();
        if (rentalId == null || rentalId.trim().isEmpty()) {
            rentalId = item.getId(); // legacy: docId equals propertyId
        }

        if (rentalId == null || rentalId.trim().isEmpty()) {
            Toast.makeText(this, "Missing Rental Id", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, RentalHouseDetailActivity.class);
        intent.putExtra(IntentKeys.RENTAL_ID, rentalId); // canonical key for detail screen
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentViewed();
    }
}
