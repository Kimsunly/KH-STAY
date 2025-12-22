package com.khstay.myapplication.data.repository;

import com.google.firebase.firestore.Query;
import com.khstay.myapplication.data.firebase.RentalService;

public class RentalRepository {

    private final RentalService rentalService;

    public RentalRepository() {
        rentalService = new RentalService();
    }

    public Query fetchAllActiveRentals() {
        return rentalService.getAllActiveRentals();
    }

    public Query fetchPopularRentals() {
        return rentalService.getPopularRentals();
    }
}
