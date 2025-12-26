package com.khstay.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;
import com.khstay.myapplication.data.firebase.NotificationService;
import com.khstay.myapplication.ui.auth.LoginActivity;
import com.khstay.myapplication.ui.chat.ConversationsActivity;
import com.khstay.myapplication.ui.home.HomeFragment;
import com.khstay.myapplication.ui.profile.ProfileFragment;
import com.khstay.myapplication.ui.rental.MyRentFragment;
import com.khstay.myapplication.ui.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FirebaseAuth auth;
    private NotificationService.ChatService chatService;
    private TextView tvMessageBadge;
    private ListenerRegistration unreadCountListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();
        chatService = new NotificationService.ChatService();

        // Redirect to Login if not logged in
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // TOP APP BAR
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // BOTTOM NAVIGATION
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            int id = item.getItemId();
            if (id == R.id.nav_search) {
                fragment = new SearchFragment();
            } else if (id == R.id.nav_my_rent) {
                fragment = new MyRentFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else {
                fragment = new HomeFragment();
            }

            return loadFragment(fragment);
        });

        // Handle refresh intent from PostRentalActivity
        handleRefreshIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRefreshIntent(intent);
    }

    private void handleRefreshIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("REFRESH_HOME", false)) {
            // Navigate to home and refresh
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            loadFragment(new HomeFragment());

            Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);

        // Setup message badge
        MenuItem messagesItem = menu.findItem(R.id.action_messages);
        if (messagesItem != null) {
            View actionView = messagesItem.getActionView();
            if (actionView != null) {
                tvMessageBadge = actionView.findViewById(R.id.tvMessageBadge);

                // Set click listener on the action view
                actionView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, ConversationsActivity.class);
                    startActivity(intent);
                });

                // Start listening for unread count
                listenToUnreadCount();
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_messages) {
            Intent intent = new Intent(MainActivity.this, ConversationsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_sign_out) {
            auth.signOut();
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void listenToUnreadCount() {
        // Poll for unread count every few seconds
        // Or use Firestore listener for real-time updates
        updateUnreadBadge();
    }

    private void updateUnreadBadge() {
        chatService.getTotalUnreadCount()
                .addOnSuccessListener(count -> {
                    if (tvMessageBadge != null) {
                        if (count > 0) {
                            tvMessageBadge.setVisibility(View.VISIBLE);
                            tvMessageBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                        } else {
                            tvMessageBadge.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error silently
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh unread count when returning to MainActivity
        updateUnreadBadge();
    }

    private boolean loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (unreadCountListener != null) {
            unreadCountListener.remove();
        }
    }
}