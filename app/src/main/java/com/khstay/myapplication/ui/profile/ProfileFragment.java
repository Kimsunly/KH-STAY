package com.khstay.myapplication.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.UserRepository;
import com.khstay.myapplication.ui.auth.LoginActivity;

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final int IMAGE_TYPE_PROFILE = 1;
    private static final int IMAGE_TYPE_COVER = 2;

    private View rootView;
    private ImageView profileImage, cameraIcon, coverImage, coverCameraIcon;
    private TextView profileName, profileEmail, signOut;
    private View settingsItem, paymentItem, notificationItem, recentViewedItem, favoritesItem, aboutItem, editProfileItem, bookingRequestsItem;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private UserRepository userRepository;

    private ShimmerFrameLayout shimmerProfile;
    private View contentLayout;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;
    private int currentImageType = IMAGE_TYPE_PROFILE; // Track which image we're updating

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        userRepository = new UserRepository();

        initViews(rootView);
        setupImagePicker();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupMenuRows();
        setupClickListeners();
        loadUserProfile();
    }

    private void initViews(View root) {
        profileImage = root.findViewById(R.id.profileImage);
        cameraIcon = root.findViewById(R.id.cameraIcon);
        coverImage = root.findViewById(R.id.coverImage);
        coverCameraIcon = root.findViewById(R.id.coverCameraIcon);
        profileName = root.findViewById(R.id.profileName);
        profileEmail = root.findViewById(R.id.profileEmail);
        signOut = root.findViewById(R.id.signOut);

        editProfileItem = root.findViewById(R.id.editProfileItem);
        bookingRequestsItem = root.findViewById(R.id.bookingRequestsItem);
        settingsItem = root.findViewById(R.id.settingsItem);
        paymentItem = root.findViewById(R.id.paymentItem);
        notificationItem = root.findViewById(R.id.notificationItem);
        recentViewedItem = root.findViewById(R.id.recentViewedItem);
        favoritesItem = root.findViewById(R.id.favoritesItem);
        aboutItem = root.findViewById(R.id.aboutItem);

        try {
            shimmerProfile = root.findViewById(R.id.shimmerProfile);
            contentLayout = root.findViewById(R.id.contentLayout);
        } catch (Exception e) {
            Log.e(TAG, "Shimmer views not found in layout", e);
            shimmerProfile = null;
            contentLayout = null;
        }
    }

    private void setupMenuRows() {
        bindMenuRow(editProfileItem, R.drawable.ic_settings, R.string.menu_edit_profile);
        bindMenuRow(bookingRequestsItem, R.drawable.ic_info, R.string.menu_booking_requests);
        bindMenuRow(settingsItem, R.drawable.ic_settings, R.string.menu_settings);
        bindMenuRow(paymentItem, R.drawable.ic_payment, R.string.menu_payment);
        bindMenuRow(notificationItem, R.drawable.ic_notifications, R.string.menu_notification);
        bindMenuRow(recentViewedItem, R.drawable.ic_list, R.string.menu_recent_viewed);
        bindMenuRow(favoritesItem, R.drawable.ic_bookmark, R.string.menu_favorites);
        bindMenuRow(aboutItem, R.drawable.ic_info, R.string.menu_about);
    }

    private void bindMenuRow(View row, @DrawableRes int iconRes, @StringRes int titleRes) {
        if (row == null) return;

        ImageView iconView = row.findViewById(R.id.menuIcon);
        TextView titleView = row.findViewById(R.id.menuTitle);
        ImageView chevronView = row.findViewById(R.id.menuChevron);

        if (iconView != null) {
            iconView.setImageResource(iconRes);
        }
        if (titleView != null) {
            titleView.setText(getString(titleRes));
        }
        if (chevronView != null) {
            chevronView.setImageResource(R.drawable.ic_chevron_right);
            chevronView.setColorFilter(getResources().getColor(R.color.text_secondary));
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            if (currentImageType == IMAGE_TYPE_PROFILE) {
                                uploadProfilePicture();
                            } else if (currentImageType == IMAGE_TYPE_COVER) {
                                uploadCoverImage();
                            }
                        }
                    }
                }
        );
    }

    private void openImagePicker(int imageType) {
        currentImageType = imageType;
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    // ========== PROFILE PICTURE ==========

    private void uploadProfilePicture() {
        if (selectedImageUri == null) return;

        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Fragment not attached, cannot upload");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            safeShowToast(getString(R.string.login_first));
            return;
        }

        showSkeletonLoading(true);

        userRepository.uploadProfilePicture(selectedImageUri)
                .addOnSuccessListener(downloadUrl -> {
                    if (!isAdded() || getContext() == null) return;

                    Log.d(TAG, "Profile picture uploaded: " + downloadUrl);
                    updateUserProfilePicture(currentUser.getUid(), downloadUrl);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;

                    Log.e(TAG, "Failed to upload profile picture", e);
                    safeShowToast("Failed to upload: " + e.getMessage());
                    showSkeletonLoading(false);
                });
    }

    private void updateUserProfilePicture(String userId, String imageUrl) {
        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Fragment not attached, cannot update profile");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", userId);
        userData.put("photoUrl", imageUrl);
        userData.put("email", currentUser != null ? currentUser.getEmail() : null);
        userData.put("displayName", (currentUser != null && currentUser.getDisplayName() != null)
                ? currentUser.getDisplayName() : "User");
        userData.put("updatedAt", Timestamp.now());

        firestore.collection("users").document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null) return;

                    firestore.collection("users").document(userId).get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.contains("createdAt")) {
                                    firestore.collection("users").document(userId)
                                            .update("createdAt", Timestamp.now());
                                }
                            });

                    Log.d(TAG, "Profile picture updated in Firestore");
                    updateUIWithNewPhoto(imageUrl);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Failed to update profile picture", e);
                    safeShowToast("Failed to update profile: " + e.getMessage());
                    showSkeletonLoading(false);
                });
    }

    private void updateUIWithNewPhoto(String photoUrl) {
        if (!isAdded() || getContext() == null) return;

        if (profileImage != null) {
            Glide.with(this)
                    .load(photoUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .circleCrop()
                    .into(profileImage);
        }

        safeShowToast(getString(R.string.profile_picture_updated));
        showSkeletonLoading(false);
    }

    // ========== COVER IMAGE ==========

    private void uploadCoverImage() {
        if (selectedImageUri == null) return;

        if (!isAdded() || getContext() == null) {
            Log.e(TAG, "Fragment not attached, cannot upload cover");
            return;
        }

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            safeShowToast(getString(R.string.login_first));
            return;
        }

        showSkeletonLoading(true);

        // First, get the old cover URL to delete it later
        firestore.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    String oldCoverUrl = doc.exists() ? doc.getString("coverUrl") : null;

                    // Upload new cover image
                    uploadCoverToStorage(currentUser.getUid(), oldCoverUrl);
                })
                .addOnFailureListener(e -> {
                    // Continue with upload even if we can't get old URL
                    uploadCoverToStorage(currentUser.getUid(), null);
                });
    }

    private void uploadCoverToStorage(String userId, String oldCoverUrl) {
        if (selectedImageUri == null) return;

        String filename = "cover_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference coverRef = storage.getReference()
                .child("cover_images")
                .child(filename);

        coverRef.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return coverRef.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    if (!isAdded() || getContext() == null) return;

                    String downloadUrl = downloadUri.toString();
                    Log.d(TAG, "Cover image uploaded: " + downloadUrl);

                    // Update Firestore with new cover URL
                    updateCoverUrlInFirestore(userId, downloadUrl, oldCoverUrl);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;

                    Log.e(TAG, "Failed to upload cover image", e);
                    safeShowToast("Failed to upload cover: " + e.getMessage());
                    showSkeletonLoading(false);
                });
    }

    private void updateCoverUrlInFirestore(String userId, String newCoverUrl, String oldCoverUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("coverUrl", newCoverUrl);
        updates.put("updatedAt", Timestamp.now());

        firestore.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded() || getContext() == null) return;

                    Log.d(TAG, "Cover image updated in Firestore");

                    // Delete old cover image from storage
                    if (oldCoverUrl != null && !oldCoverUrl.isEmpty()) {
                        deleteOldCoverImage(oldCoverUrl);
                    }

                    updateUIWithNewCover(newCoverUrl);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getContext() == null) return;
                    Log.e(TAG, "Failed to update cover image in Firestore", e);
                    safeShowToast("Failed to update cover: " + e.getMessage());
                    showSkeletonLoading(false);
                });
    }

    private void deleteOldCoverImage(String oldCoverUrl) {
        try {
            StorageReference oldRef = storage.getReferenceFromUrl(oldCoverUrl);
            oldRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Old cover image deleted successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to delete old cover image", e));
        } catch (Exception e) {
            Log.e(TAG, "Error parsing old cover URL", e);
        }
    }

    private void updateUIWithNewCover(String coverUrl) {
        if (!isAdded() || getContext() == null) return;

        if (coverImage != null) {
            Glide.with(this)
                    .load(coverUrl)
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .centerCrop()
                    .into(coverImage);
        }

        safeShowToast("Cover image updated successfully!");
        showSkeletonLoading(false);
    }

    // ========== LOAD USER PROFILE ==========

    private void loadUserProfile() {
        showSkeletonLoading(true);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            showSkeletonLoading(false);
            if (profileName != null) {
                profileName.setText(getString(R.string.profile_name_placeholder));
            }
            if (profileEmail != null) {
                profileEmail.setText(getString(R.string.profile_email_placeholder));
            }
            return;
        }

        userRepository.getCurrentUserData()
                .addOnSuccessListener(doc -> {
                    String name = null;
                    String email = null;
                    String photoUrl = null;
                    String coverUrl = null;

                    if (doc.exists()) {
                        name = doc.getString("displayName");
                        email = doc.getString("email");
                        photoUrl = doc.getString("photoUrl");
                        coverUrl = doc.getString("coverUrl");
                    }

                    if (name == null || name.trim().isEmpty()) {
                        name = currentUser.getDisplayName();
                    }
                    if (email == null || email.trim().isEmpty()) {
                        email = currentUser.getEmail();
                    }
                    if (photoUrl == null || photoUrl.trim().isEmpty()) {
                        if (currentUser.getPhotoUrl() != null) {
                            photoUrl = currentUser.getPhotoUrl().toString();
                        }
                    }

                    updateUI(name, email, photoUrl, coverUrl);
                    showSkeletonLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user profile from Firestore: ", e);
                    String name = currentUser.getDisplayName();
                    String email = currentUser.getEmail();
                    String photoUrl = (currentUser.getPhotoUrl() != null) ?
                            currentUser.getPhotoUrl().toString() : null;
                    updateUI(name, email, photoUrl, null);
                    showSkeletonLoading(false);
                });
    }

    private void showSkeletonLoading(boolean show) {
        if (shimmerProfile != null && contentLayout != null) {
            if (show) {
                shimmerProfile.startShimmer();
                shimmerProfile.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.GONE);
            } else {
                shimmerProfile.stopShimmer();
                shimmerProfile.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateUI(String name, String email, String photoUrl, String coverUrl) {
        if (profileName != null) {
            profileName.setText(
                    (name != null && !name.trim().isEmpty())
                            ? name
                            : getString(R.string.profile_name_placeholder)
            );
        }

        if (profileEmail != null) {
            profileEmail.setText(
                    (email != null && !email.trim().isEmpty())
                            ? email
                            : getString(R.string.profile_email_placeholder)
            );
        }

        if (profileImage != null) {
            if (photoUrl != null && !photoUrl.trim().isEmpty()) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_profile);
            }
        }

        // Load cover image
        if (coverImage != null) {
            if (coverUrl != null && !coverUrl.trim().isEmpty()) {
                Glide.with(this)
                        .load(coverUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .centerCrop()
                        .into(coverImage);
            } else {
                // Use default gradient or placeholder
                coverImage.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    // ========== CLICK LISTENERS ==========

    private void setupClickListeners() {
        if (rootView == null) {
            Log.e(TAG, "rootView is null, cannot setup click listeners.");
            return;
        }

        // Profile picture click
        if (cameraIcon != null) {
            cameraIcon.setOnClickListener(v -> openImagePicker(IMAGE_TYPE_PROFILE));
        }

        // Cover image click - using long click for safety
        if (coverCameraIcon != null) {
            coverCameraIcon.setOnClickListener(v -> openImagePicker(IMAGE_TYPE_COVER));
        }

        // Alternative: Long press on cover image itself
        if (coverImage != null) {
            coverImage.setOnLongClickListener(v -> {
                openImagePicker(IMAGE_TYPE_COVER);
                return true;
            });
        }

        if (signOut != null) {
            signOut.setOnClickListener(v -> handleSignOut());
        }

        bindMenuClick(editProfileItem, EditProfileActivity.class);
        bindMenuClick(bookingRequestsItem, com.khstay.myapplication.ui.rental.BookingRequestsActivity.class);
        bindMenuClick(settingsItem, SettingsActivity.class);
        bindMenuClick(paymentItem, PaymentActivity.class);
        bindMenuClick(notificationItem, NotificationActivity.class);
        bindMenuClick(recentViewedItem, RecentViewedActivity.class);
        bindMenuClick(favoritesItem, FavoritesActivity.class);
        bindMenuClick(aboutItem, AboutActivity.class);
    }

    private void bindMenuClick(View item, Class<?> targetActivity) {
        if (item != null) {
            item.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), targetActivity);
                startActivity(intent);
            });
        }
    }

    private void handleSignOut() {
        auth.signOut();
        safeShowToast(getString(R.string.logout));
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void safeShowToast(String message) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserProfile();
    }
}