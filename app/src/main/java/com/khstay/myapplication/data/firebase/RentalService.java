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

    // Fetch most popular rentals
    public Query getPopularRentals() {
        return db.collection("rental_houses")
                .whereEqualTo("status", "active")
                .whereEqualTo("isPopular", true)
                .limit(10);
    }
}
