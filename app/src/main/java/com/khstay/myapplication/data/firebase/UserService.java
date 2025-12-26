package com.khstay.myapplication.data.firebase;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.tasks.Tasks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing user data in Firebase
 */
public class UserService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public UserService() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.storage = FirebaseStorage.getInstance();
    }

    /**
     * Get current user document from Firestore
     */
    public Task<DocumentSnapshot> getCurrentUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        return db.collection("users").document(currentUser.getUid()).get();
    }

    /**
     * Update user profile information
     */
    public Task<Void> updateUserProfile(String displayName, String phone, String birthDate) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        Map<String, Object> updates = new HashMap<>();
        if (displayName != null) updates.put("displayName", displayName);
        if (phone != null) updates.put("phone", phone);
        if (birthDate != null) updates.put("birthDate", birthDate);
        updates.put("updatedAt", Timestamp.now());

        // Also update Firebase Auth display name
        if (displayName != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build();
            currentUser.updateProfile(profileUpdates);
        }

        return db.collection("users")
                .document(currentUser.getUid())
                .update(updates);
    }

    /**
     * Update user email
     */
    public Task<Void> updateEmail(String newEmail) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        // Update in Firebase Auth first
        return currentUser.updateEmail(newEmail)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Then update in Firestore
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("email", newEmail);
                    updates.put("updatedAt", Timestamp.now());

                    return db.collection("users")
                            .document(currentUser.getUid())
                            .update(updates);
                });
    }

    /**
     * Update user password
     */
    public Task<Void> updatePassword(String newPassword) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }
        return currentUser.updatePassword(newPassword);
    }

    /**
     * Upload profile picture to Firebase Storage and update user profile
     */
    public Task<String> uploadProfilePicture(Uri imageUri) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || imageUri == null) {
            return Tasks.forException(new Exception("User not authenticated or image is null"));
        }

        String filename = "profile_" + currentUser.getUid() + "_" + UUID.randomUUID() + ".jpg";
        StorageReference imageRef = storage.getReference()
                .child("profile_images")
                .child(filename);

        return imageRef.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return imageRef.getDownloadUrl();
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    String downloadUrl = task.getResult().toString();

                    // Update Firebase Auth profile
                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest.Builder()
                                    .setPhotoUri(Uri.parse(downloadUrl))
                                    .build();
                    currentUser.updateProfile(profileUpdates);

                    return downloadUrl;
                });
    }

    /**
     * Update user settings (dark mode, notifications, etc.)
     */
    public Task<Void> updateSettings(String key, Object value) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(key, value);
        updates.put("updatedAt", Timestamp.now());

        return db.collection("users")
                .document(currentUser.getUid())
                .update(updates);
    }

    /**
     * Get user document by ID
     */
    public Task<DocumentSnapshot> getUserById(String userId) {
        return db.collection("users").document(userId).get();
    }
}