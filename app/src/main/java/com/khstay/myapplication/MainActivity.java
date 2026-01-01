
// app/src/main/java/com/khstay/myapplication/MainActivity.java
package com.khstay.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.messaging.FirebaseMessaging;
import com.khstay.myapplication.data.firebase.NotificationService;
import com.khstay.myapplication.ui.auth.LoginActivity;
import com.khstay.myapplication.ui.chat.ConversationsActivity;
import com.khstay.myapplication.ui.home.HomeFragment;
import com.khstay.myapplication.ui.profile.ProfileFragment;
import com.khstay.myapplication.ui.rental.MyRentFragment;
import com.khstay.myapplication.ui.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
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

        // Ensure token is saved; do NOT delete here
        ensureFcmTokenSaved();

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment;
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

        handleRefreshIntent(getIntent());
    }

    private void ensureFcmTokenSaved() {
        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "FCM Token (ensure saved): " + token);
                    var user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null || token == null) return;
                    String uid = user.getUid();

                    FirebaseFirestore.getInstance().collection("users").document(uid)
                            .get().addOnSuccessListener(doc -> {
                                String current = doc.getString("fcmToken");
                                if (current == null || !current.equals(token)) {
                                    FirebaseFirestore.getInstance().collection("users").document(uid)
                                            .update("fcmToken", token)
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token saved/updated"))
                                            .addOnFailureListener(e -> Log.e(TAG, "Failed to save FCM token", e));

                                    // Optional: tokens/{token}.uid index
                                    FirebaseFirestore.getInstance().collection("tokens").document(token)
                                            .set(java.util.Collections.singletonMap("uid", uid));
                                } else {
                                    Log.d(TAG, "FCM token unchanged; skip write");
                                }
                            });
                })
                .addOnFailureListener(e -> Log.e(TAG, "getToken failed", e));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission granted");
            } else {
                Log.d(TAG, "Notification permission denied");
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleRefreshIntent(intent);
    }

    private void handleRefreshIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("REFRESH_HOME", false)) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            loadFragment(new HomeFragment());
            Toast.makeText(this, "Data refreshed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);

        MenuItem messagesItem = menu.findItem(R.id.action_messages);
        if (messagesItem != null) {
            View actionView = messagesItem.getActionView();
            if (actionView != null) {
                tvMessageBadge = actionView.findViewById(R.id.tvMessageBadge);
                actionView.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, ConversationsActivity.class);
                    startActivity(intent);
                });
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
            // 1) Clear token in Firestore so pushes stop targeting this UID
            com.khstay.myapplication.utils.FcmTokenManager.clearTokenForCurrentUser();

            // 2) Delete the local FCM token (forces new token next login)
            FirebaseMessaging.getInstance().deleteToken()
                    .addOnSuccessListener(aVoid -> Log.d("MainActivity", "FCM token deleted locally"))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to delete FCM token", e));

            // 3) Sign out
            auth.signOut();
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();

            // 4) To Login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void listenToUnreadCount() { updateUnreadBadge(); }

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
                    // Silent
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
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
