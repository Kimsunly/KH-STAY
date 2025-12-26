package com.khstay.myapplication.ui.profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.UserRepository;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView backButton;
    private EditText etDisplayName, etPhone, etBirthDate, etEmail;
    private Button btnSaveChanges;

    private FirebaseAuth auth;
    private UserRepository userRepository;

    private String currentEmail;

    private com.facebook.shimmer.ShimmerFrameLayout shimmerLoading;
    private View contentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        initViews();
        setupClickListeners();
        loadUserData();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        etDisplayName = findViewById(R.id.etDisplayName);
        etPhone = findViewById(R.id.etPhone);
        etBirthDate = findViewById(R.id.etBirthDate);
        etEmail = findViewById(R.id.etEmail);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        try {
            shimmerLoading = findViewById(R.id.shimmerLoading);
            contentLayout = findViewById(R.id.contentLayout);
        } catch (Exception e) {
            shimmerLoading = null;
            contentLayout = null;
        }
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showLoading(true);

        userRepository.getCurrentUserData()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String displayName = doc.getString("displayName");
                        String phone = doc.getString("phone");
                        String birthDate = doc.getString("birthDate");
                        String email = doc.getString("email");

                        if (displayName != null) etDisplayName.setText(displayName);
                        if (phone != null) etPhone.setText(phone);
                        if (birthDate != null) etBirthDate.setText(birthDate);
                        if (email != null) {
                            etEmail.setText(email);
                            currentEmail = email;
                        }
                    } else {
                        // Use Firebase Auth data
                        if (currentUser.getDisplayName() != null) {
                            etDisplayName.setText(currentUser.getDisplayName());
                        }
                        if (currentUser.getEmail() != null) {
                            etEmail.setText(currentUser.getEmail());
                            currentEmail = currentUser.getEmail();
                        }
                    }
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    showLoading(false);
                });
    }

    private void saveChanges() {
        String displayName = etDisplayName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(displayName)) {
            etDisplayName.setError("Name is required");
            etDisplayName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        // Update profile
        userRepository.updateUserProfile(displayName, phone, birthDate)
                .addOnSuccessListener(aVoid -> {
                    // Check if email changed
                    if (!email.equals(currentEmail)) {
                        updateEmail(email);
                    } else {
                        Toast.makeText(this, "Profile updated successfully",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                });
    }

    private void updateEmail(String newEmail) {
        userRepository.updateEmail(newEmail)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile and email updated successfully",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Profile updated but email change failed. " +
                            "Please re-authenticate and try again.", Toast.LENGTH_LONG).show();
                    btnSaveChanges.setEnabled(true);
                    btnSaveChanges.setText("Save Changes");
                });
    }

    private void showLoading(boolean show) {
        if (shimmerLoading != null && contentLayout != null) {
            if (show) {
                shimmerLoading.startShimmer();
                shimmerLoading.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.GONE);
            } else {
                shimmerLoading.stopShimmer();
                shimmerLoading.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }
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
}