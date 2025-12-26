package com.khstay.myapplication.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.UserRepository;
import com.khstay.myapplication.ui.rental.RentalHouseDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private ImageView backButton;
    private RecyclerView favoritesRecyclerView;
    private TextView emptyStateText;

    private FirebaseAuth auth;
    private UserRepository userRepository;
    private RecentViewedAdapter adapter;
    private List<RecentViewedItem> favoriteItems;

    private com.facebook.shimmer.ShimmerFrameLayout shimmerLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadFavorites();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        try {
            shimmerLoading = findViewById(R.id.shimmerLoading);
        } catch (Exception e) {
            shimmerLoading = null;
        }
    }

    private void setupRecyclerView() {
        favoriteItems = new ArrayList<>();
        adapter = new RecentViewedAdapter(favoriteItems, this::onItemClick);
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoritesRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadFavorites() {
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
        favoritesRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        userRepository.getFavorites()
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    favoriteItems.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        RecentViewedItem item = new RecentViewedItem();
                        item.setId(doc.getId());
                        item.setPropertyId(doc.getString("propertyId"));
                        item.setPropertyName(doc.getString("propertyName"));
                        item.setPropertyImage(doc.getString("propertyImage"));
                        item.setPropertyPrice(doc.getString("propertyPrice"));
                        item.setPropertyLocation(doc.getString("propertyLocation"));
                        item.setViewedAt(doc.getTimestamp("addedAt"));
                        favoriteItems.add(item);
                    }
                    adapter.notifyDataSetChanged();

                    // Hide loading
                    if (shimmerLoading != null) {
                        shimmerLoading.stopShimmer();
                        shimmerLoading.setVisibility(View.GONE);
                    }

                    if (favoriteItems.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        favoritesRecyclerView.setVisibility(View.VISIBLE);
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
            favoritesRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            favoritesRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void onItemClick(RecentViewedItem item) {
        // Open property detail
        Intent intent = new Intent(this, RentalHouseDetailActivity.class);
        intent.putExtra(RentalHouseDetailActivity.EXTRA_RENTAL_ID, item.getPropertyId());
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
        loadFavorites();
    }
}