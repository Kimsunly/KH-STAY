
package com.khstay.myapplication.data.firebase;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class RecentViewedService {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public RecentViewedService() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Add a rental to recent views (use rentalId consistently).
     */
    public Task<Void> addRecentView(String rentalId, String propertyName,
                                    String propertyImage, String propertyPrice,
                                    String propertyLocation) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        Map<String, Object> recentView = new HashMap<>();
        recentView.put("rentalId", rentalId);      // canonical field going forward
        recentView.put("propertyName", propertyName);
        recentView.put("propertyImage", propertyImage);
        recentView.put("propertyPrice", propertyPrice);
        recentView.put("propertyLocation", propertyLocation);
        recentView.put("viewedAt", Timestamp.now());

        return db.collection("users")
                .document(currentUser.getUid())
                .collection("recent_viewed")
                .document(rentalId)                // use rentalId as doc ID to avoid duplicates
                .set(recentView, SetOptions.merge());
    }

    /**
     * Get recent viewed rentals for current user.
     */
    public Query getRecentViewed() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return null;
        }

        return db.collection("users")
                .document(currentUser.getUid())
                .collection("recent_viewed")
                .orderBy("viewedAt", Query.Direction.DESCENDING)
                .limit(20);
    }

    /**
     * Clear all recent views (batch delete, returns Task<Void>).
     */
    public Task<Void> clearRecentViews() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            return Tasks.forException(new IllegalStateException("User not logged in"));
        }

        CollectionReference coll = db.collection("users")
                .document(currentUser.getUid())
                .collection("recent_viewed");

        return coll.get().continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();

            WriteBatch batch = db.batch();
            for (DocumentSnapshot doc : task.getResult()) {
                batch.delete(doc.getReference());
            }
            return batch.commit();
        });
    }
}
