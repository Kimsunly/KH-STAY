package com.khstay.myapplication.utils;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Log;

/**
 * Helper class to calculate and update popularity scores
 * Formula: Score = (views × 1.0) + (favorites × 3.0) + (bookings × 5.0)
 */
public class PopularityScoreHelper {

    private static final String TAG = "PopularityScoreHelper";
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Weight factors for popularity calculation
    private static final double VIEW_WEIGHT = 1.0;
    private static final double FAVORITE_WEIGHT = 3.0;
    private static final double BOOKING_WEIGHT = 5.0;

    /**
     * Calculate popularity score from counts
     */
    public static double calculateScore(int views, int favorites, int bookings) {
        return (views * VIEW_WEIGHT) +
                (favorites * FAVORITE_WEIGHT) +
                (bookings * BOOKING_WEIGHT);
    }

    /**
     * Increment view count and update popularity score
     */
    public static void incrementViewCount(String rentalId) {
        db.collection("rental_houses")
                .document(rentalId)
                .update("viewCount", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "View count incremented for: " + rentalId);
                    recalculateScore(rentalId);
                })
                .addOnFailureListener(e -> {
                    // If field doesn't exist, create it
                    Log.w(TAG, "Creating viewCount field for: " + rentalId);
                    db.collection("rental_houses")
                            .document(rentalId)
                            .update("viewCount", 1)
                            .addOnSuccessListener(v -> recalculateScore(rentalId))
                            .addOnFailureListener(err ->
                                    Log.e(TAG, "Failed to create viewCount", err));
                });
    }

    /**
     * Increment favorite count and update popularity score
     */
    public static void incrementFavoriteCount(String rentalId) {
        db.collection("rental_houses")
                .document(rentalId)
                .update("favoriteCount", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Favorite count incremented for: " + rentalId);
                    recalculateScore(rentalId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Creating favoriteCount field for: " + rentalId);
                    db.collection("rental_houses")
                            .document(rentalId)
                            .update("favoriteCount", 1)
                            .addOnSuccessListener(v -> recalculateScore(rentalId))
                            .addOnFailureListener(err ->
                                    Log.e(TAG, "Failed to create favoriteCount", err));
                });
    }

    /**
     * Decrement favorite count and update popularity score
     */
    public static void decrementFavoriteCount(String rentalId) {
        db.collection("rental_houses")
                .document(rentalId)
                .update("favoriteCount", FieldValue.increment(-1))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Favorite count decremented for: " + rentalId);
                    recalculateScore(rentalId);
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to decrement favoriteCount", e));
    }

    /**
     * Increment booking count and update popularity score
     */
    public static void incrementBookingCount(String rentalId) {
        db.collection("rental_houses")
                .document(rentalId)
                .update("bookingCount", FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Booking count incremented for: " + rentalId);
                    recalculateScore(rentalId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Creating bookingCount field for: " + rentalId);
                    db.collection("rental_houses")
                            .document(rentalId)
                            .update("bookingCount", 1)
                            .addOnSuccessListener(v -> recalculateScore(rentalId))
                            .addOnFailureListener(err ->
                                    Log.e(TAG, "Failed to create bookingCount", err));
                });
    }

    /**
     * Recalculate and update popularity score for a rental
     */
    public static void recalculateScore(String rentalId) {
        db.collection("rental_houses")
                .document(rentalId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    // Get current counts (default to 0 if null)
                    Long viewsLong = doc.getLong("viewCount");
                    Long favoritesLong = doc.getLong("favoriteCount");
                    Long bookingsLong = doc.getLong("bookingCount");

                    int views = viewsLong != null ? viewsLong.intValue() : 0;
                    int favorites = favoritesLong != null ? favoritesLong.intValue() : 0;
                    int bookings = bookingsLong != null ? bookingsLong.intValue() : 0;

                    // Calculate new score
                    double score = calculateScore(views, favorites, bookings);

                    // Update score in Firestore
                    db.collection("rental_houses")
                            .document(rentalId)
                            .update("popularityScore", score)
                            .addOnSuccessListener(aVoid ->
                                    Log.d(TAG, String.format(
                                            "Score updated for %s: %.2f (v:%d f:%d b:%d)",
                                            rentalId, score, views, favorites, bookings)))
                            .addOnFailureListener(e ->
                                    Log.e(TAG, "Failed to update score", e));
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to fetch rental for score calculation", e));
    }

    /**
     * Initialize scores for all rentals (run once for existing data)
     */
    public static void initializeAllScores() {
        db.collection("rental_houses")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = 0;
                    for (var doc : querySnapshot.getDocuments()) {
                        recalculateScore(doc.getId());
                        count++;
                    }
                    Log.d(TAG, "Initialized scores for " + count + " rentals");
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Failed to initialize scores", e));
    }
}