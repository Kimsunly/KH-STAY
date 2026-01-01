package com.khstay.myapplication.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.khstay.myapplication.utils.PopularityScoreHelper;

import java.util.HashMap;
import java.util.Map;

public class FavoriteService {

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public FavoriteService() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Add a property to favorites and increment rental's favoriteCount
     */
    public Task<Void> addFavorite(String propertyId, String propertyName,
                                  String propertyImage, String propertyPrice,
                                  String propertyLocation) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new Exception("User not authenticated"));
        }

        String uid = currentUser.getUid();

        Map<String, Object> favorite = new HashMap<>();
        favorite.put("propertyId", propertyId);
        favorite.put("propertyName", propertyName);
        favorite.put("propertyImage", propertyImage);
        favorite.put("propertyPrice", propertyPrice);
        favorite.put("propertyLocation", propertyLocation);
        favorite.put("timestamp", Timestamp.now());

        return db.collection("users")
                .document(uid)
                .collection("favorites")
                .document(propertyId)
                .set(favorite)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // NEW: Increment favorite count in rental document (for popularity score)
                    PopularityScoreHelper.incrementFavoriteCount(propertyId);

                    return Tasks.forResult(null);
                });
    }

    /**
     * Remove a property from favorites and decrement rental's favoriteCount
     */
    public Task<Void> removeFavorite(String propertyId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new Exception("User not authenticated"));
        }

        String uid = currentUser.getUid();

        return db.collection("users")
                .document(uid)
                .collection("favorites")
                .document(propertyId)
                .delete()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // NEW: Decrement favorite count in rental document (for popularity score)
                    PopularityScoreHelper.decrementFavoriteCount(propertyId);

                    return Tasks.forResult(null);
                });
    }

    /**
     * Check if a property is in favorites
     */
    public Task<Boolean> isFavorite(String propertyId) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forResult(false);
        }

        String uid = currentUser.getUid();

        return db.collection("users")
                .document(uid)
                .collection("favorites")
                .document(propertyId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        return document != null && document.exists();
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

        String uid = currentUser.getUid();

        return db.collection("users")
                .document(uid)
                .collection("favorites")
                .orderBy("timestamp", Query.Direction.DESCENDING);
    }

    /**
     * Clear all favorites
     */
    public Task<Void> clearFavorites() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new Exception("User not authenticated"));
        }

        String uid = currentUser.getUid();

        return db.collection("users")
                .document(uid)
                .collection("favorites")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    var batch = db.batch();
                    for (DocumentSnapshot doc : task.getResult()) {
                        batch.delete(doc.getReference());

                        // Decrement favorite count for each removed favorite
                        String propertyId = doc.getString("propertyId");
                        if (propertyId != null) {
                            PopularityScoreHelper.decrementFavoriteCount(propertyId);
                        }
                    }

                    return batch.commit();
                });
    }
}