package com.khstay.myapplication.ui.profile;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.UserRepository;

public class SettingsActivity extends AppCompatActivity {

    private ImageView backButton;
    private Switch darkModeSwitch, notificationSwitch;
    private TextView languageOption, privacyOption, termsOption, clearCacheOption;

    private FirebaseAuth auth;
    private UserRepository userRepository;

    private boolean isLoadingSettings = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        initViews();
        setupClickListeners();
        loadUserSettings();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        notificationSwitch = findViewById(R.id.notificationSwitch);
        languageOption = findViewById(R.id.languageOption);
        privacyOption = findViewById(R.id.privacyOption);
        termsOption = findViewById(R.id.termsOption);
        clearCacheOption = findViewById(R.id.clearCacheOption);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isLoadingSettings) return; // Prevent triggering during load

            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            saveSetting("darkMode", isChecked);
        });

        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isLoadingSettings) return; // Prevent triggering during load

            saveSetting("notifications", isChecked);
            Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT).show();
        });

        languageOption.setOnClickListener(v -> {
            Toast.makeText(this, "Language selection coming soon", Toast.LENGTH_SHORT).show();
        });

        privacyOption.setOnClickListener(v -> {
            Toast.makeText(this, "Privacy policy coming soon", Toast.LENGTH_SHORT).show();
        });

        termsOption.setOnClickListener(v -> {
            Toast.makeText(this, "Terms & Conditions coming soon", Toast.LENGTH_SHORT).show();
        });

        clearCacheOption.setOnClickListener(v -> {
            // Clear Glide cache
            new Thread(() -> {
                try {
                    com.bumptech.glide.Glide.get(this).clearDiskCache();
                    runOnUiThread(() ->
                            Toast.makeText(this, "Cache cleared successfully",
                                    Toast.LENGTH_SHORT).show()
                    );
                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Failed to clear cache",
                                    Toast.LENGTH_SHORT).show()
                    );
                }
            }).start();
        });
    }

    private void loadUserSettings() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        isLoadingSettings = true;

        userRepository.getCurrentUserData()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean darkMode = doc.getBoolean("darkMode");
                        Boolean notifications = doc.getBoolean("notifications");

                        if (darkMode != null) {
                            darkModeSwitch.setChecked(darkMode);
                            // Apply dark mode setting
                            if (darkMode) {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                            } else {
                                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                            }
                        }

                        if (notifications != null) {
                            notificationSwitch.setChecked(notifications);
                        }
                    }
                    isLoadingSettings = false;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load settings", Toast.LENGTH_SHORT).show();
                    isLoadingSettings = false;
                });
    }

    private void saveSetting(String key, boolean value) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        userRepository.updateSettings(key, value)
                .addOnSuccessListener(aVoid -> {
                    // Setting saved successfully
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save setting", Toast.LENGTH_SHORT).show();
                    // Revert the switch
                    if (key.equals("darkMode")) {
                        darkModeSwitch.setChecked(!value);
                    } else if (key.equals("notifications")) {
                        notificationSwitch.setChecked(!value);
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}