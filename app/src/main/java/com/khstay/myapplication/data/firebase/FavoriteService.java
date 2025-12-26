package com.khstay.myapplication.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for managing user favorites/wishlist
 */
public class FavoriteService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public FavoriteService() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Add a property to favorites
     */
    public Task<Void> addFavorite(String propertyId, String propertyName,
                                  String propertyImage, String propertyPrice,
                                  String propertyLocation) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        Map<String, Object> favorite = new HashMap<>();
        favorite.put("propertyId", propertyId);
        favorite.put("propertyName", propertyName);
        favorite.put("propertyImage", propertyImage);
        favorite.put("propertyPrice", propertyPrice);
        favorite.put("propertyLocation", propertyLocation);
        favorite.put("addedAt", Timestamp.now());

        return db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .document(propertyId) // Use propertyId as document ID to avoid duplicates
                .set(favorite);
    }

    /**
     * Remove a property from favorites
     */
    public Task<Void> removeFavorite(String propertyId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        return db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .document(propertyId)
                .delete();
    }

    /**
     * Check if a property is favorited
     */
    public Task<Boolean> isFavorite(String propertyId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        return db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .document(propertyId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        return task.getResult().exists();
                    }
                    return false;
                });
    }

    /**
     * Get all favorites for current user
     */
    public Query getFavorites() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        return db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .orderBy("addedAt", Query.Direction.DESCENDING);
    }

    /**
     * Clear all favorites
     */
    public Task<Void> clearFavorites() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        return db.collection("users")
                .document(currentUser.getUid())
                .collection("favorites")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Delete all documents
                    task.getResult().getDocuments().forEach(doc ->
                            doc.getReference().delete()
                    );
                    return null;
                });
    }
}