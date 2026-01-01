package com.khstay.myapplication.data.firebase;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class RentalService {

    private final FirebaseFirestore db;

    public RentalService() {
        db = FirebaseFirestore.getInstance();
    }

    // Fetch all active rentals (Nearby)
    public Query getAllActiveRentals() {
        return db.collection("rental_houses")
                .whereEqualTo("status", "active")
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }

    // Fetch most popular rentals - HYBRID SCORE SYSTEM
    // Combines views (1x), favorites (3x), and bookings (5x)
    public Query getPopularRentals() {
        return db.collection("rental_houses")
                .whereEqualTo("status", "active")
                .orderBy("popularityScore", Query.Direction.DESCENDING)  // ← Sorted by hybrid score
                .limit(10);
    }

    // Alternative: Manual popular (if you want to keep manual control option)
    public Query getManualPopularRentals() {
        return db.collection("rental_houses")
                .whereEqualTo("status", "active")
                .whereEqualTo("isPopular", true)
                .limit(10);
    }
}