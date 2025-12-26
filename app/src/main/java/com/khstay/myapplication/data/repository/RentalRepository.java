
package com.khstay.myapplication.data.repository;

import android.net.Uri;

import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.khstay.myapplication.data.firebase.RentalService;
import com.khstay.myapplication.ui.rental.model.Rental;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RentalRepository {

    private final RentalService rentalService;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public RentalRepository() {
        rentalService = new RentalService();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance(); // bucket comes from MyApp override / google-services.json
    }

    public Query fetchAllActiveRentals() {
        return rentalService.getAllActiveRentals();
    }

    public Query fetchPopularRentals() {
        return rentalService.getPopularRentals();
    }

    /**
     * Create a new rental with image upload to Firebase Storage, then save doc to Firestore.
     */
    public Task<DocumentReference> createRental(Rental rental, Uri imageUri) {
        if (imageUri == null) {
            return Tasks.forException(new IllegalArgumentException("Image Uri is null"));
        }

        String imageFileName = "rental_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + ".jpg";

        StorageReference imageRef = storage.getReference()
                .child("rental_images")
                .child(imageFileName);

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build();

        return imageRef.putFile(imageUri, metadata)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    return imageRef.getDownloadUrl();
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    Uri downloadUrl = task.getResult();

                    Map<String, Object> rentalData = new HashMap<>();
                    rentalData.put("title", rental.getTitle());
                    rentalData.put("location", rental.getLocation());
                    rentalData.put("price", rental.getPrice());
                    rentalData.put("status", rental.getStatus());
                    rentalData.put("category", rental.getCategory());
                    rentalData.put("isPopular", rental.getIsPopular() != null ? rental.getIsPopular() : false);
                    rentalData.put("imageUrl", downloadUrl != null ? downloadUrl.toString() : null);
                    rentalData.put("description", rental.getDescription());
                    rentalData.put("ownerId", rental.getOwnerId());
                    rentalData.put("createdAt", Timestamp.now());

                    if (rental.getBedrooms() != null && rental.getBedrooms() > 0) {
                        rentalData.put("bedrooms", rental.getBedrooms());
                    }
                    if (rental.getBathrooms() != null && rental.getBathrooms() > 0) {
                        rentalData.put("bathrooms", rental.getBathrooms());
                    }
                    if (rental.getLatitude() != null) {
                        rentalData.put("latitude", rental.getLatitude());
                    }
                    if (rental.getLongitude() != null) {
                        rentalData.put("longitude", rental.getLongitude());
                    }

                    return db.collection("rental_houses").add(rentalData);
                });
    }

    public Task<Void> updateRental(String rentalId, Map<String, Object> updates) {
        updates.put("updatedAt", Timestamp.now());
        return db.collection("rental_houses")
                .document(rentalId)
                .update(updates);
    }

    public Task<Void> deleteRental(String rentalId) {
        return db.collection("rental_houses")
                .document(rentalId)
                .delete();
    }

    public Task<Void> updateRentalStatus(String rentalId, String status) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", Timestamp.now());
        return db.collection("rental_houses")
                .document(rentalId)
                .update(updates);
    }

    public Query getRentalsByOwnerId(String ownerId) {
        return db.collection("rental_houses")
                .whereEqualTo("ownerId", ownerId)
                .orderBy("createdAt", Query.Direction.DESCENDING);
    }
}

