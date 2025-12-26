package com.khstay.myapplication.ui.profile;

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

import java.util.ArrayList;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private ImageView backButton;
    private RecyclerView notificationsRecyclerView;
    private TextView emptyStateText;

    private FirebaseAuth auth;
    private UserRepository userRepository;
    private NotificationAdapter adapter;
    private List<NotificationItem> notifications;

    private com.facebook.shimmer.ShimmerFrameLayout shimmerLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadNotifications();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);

        try {
            shimmerLoading = findViewById(R.id.shimmerLoading);
        } catch (Exception e) {
            shimmerLoading = null;
        }
    }

    private void setupRecyclerView() {
        notifications = new ArrayList<>();
        adapter = new NotificationAdapter(notifications, this::onNotificationClick);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadNotifications() {
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
        notificationsRecyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.GONE);

        userRepository.getNotifications()
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    notifications.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        NotificationItem item = doc.toObject(NotificationItem.class);
                        item.setId(doc.getId()); // Set document ID
                        notifications.add(item);
                    }
                    adapter.notifyDataSetChanged();

                    // Hide loading
                    if (shimmerLoading != null) {
                        shimmerLoading.stopShimmer();
                        shimmerLoading.setVisibility(View.GONE);
                    }

                    if (notifications.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        notificationsRecyclerView.setVisibility(View.VISIBLE);
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
            notificationsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            notificationsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void onNotificationClick(NotificationItem item) {
        // Mark as read
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null && item.getId() != null && !item.isRead()) {
            userRepository.markNotificationAsRead(item.getId())
                    .addOnSuccessListener(aVoid -> {
                        // Update item in list
                        item.setRead(true);
                        adapter.notifyDataSetChanged();
                    });
        }
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
        loadNotifications();
    }
}