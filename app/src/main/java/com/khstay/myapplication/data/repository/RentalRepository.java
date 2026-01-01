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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RentalRepository {

    private final RentalService rentalService;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    public RentalRepository() {
        rentalService = new RentalService();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public Query fetchAllActiveRentals() {
        return rentalService.getAllActiveRentals();
    }

    public Query fetchPopularRentals() {
        return rentalService.getPopularRentals();
    }

    /**
     * Create a new rental with MULTIPLE image uploads (max 3) to Firebase Storage,
     * then save doc to Firestore.
     */
    public Task<DocumentReference> createRental(Rental rental, List<Uri> imageUris) {
        if (imageUris == null || imageUris.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("At least one image is required"));
        }

        if (imageUris.size() > 3) {
            return Tasks.forException(new IllegalArgumentException("Maximum 3 images allowed"));
        }

        // Upload all images in parallel
        List<Task<Uri>> uploadTasks = new ArrayList<>();

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);
            String imageFileName = "rental_" + System.currentTimeMillis() + "_" + i + "_" + UUID.randomUUID() + ".jpg";

            StorageReference imageRef = storage.getReference()
                    .child("rental_images")
                    .child(imageFileName);

            StorageMetadata metadata = new StorageMetadata.Builder()
                    .setContentType("image/jpeg")
                    .build();

            Task<Uri> uploadTask = imageRef.putFile(imageUri, metadata)
                    .continueWithTask(task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return imageRef.getDownloadUrl();
                    });

            uploadTasks.add(uploadTask);
        }

        // Wait for all uploads to complete
        return Tasks.whenAllSuccess(uploadTasks)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    // Get all download URLs
                    List<String> downloadUrls = new ArrayList<>();
                    for (Object result : task.getResult()) {
                        if (result instanceof Uri) {
                            downloadUrls.add(result.toString());
                        }
                    }

                    // Create rental data
                    Map<String, Object> rentalData = new HashMap<>();
                    rentalData.put("title", rental.getTitle());
                    rentalData.put("location", rental.getLocation());
                    rentalData.put("price", rental.getPrice());
                    rentalData.put("status", rental.getStatus());
                    rentalData.put("category", rental.getCategory());
                    rentalData.put("isPopular", rental.getIsPopular() != null ? rental.getIsPopular() : false);

                    // Save all image URLs
                    rentalData.put("imageUrls", downloadUrls);
                    // Also save first image as primary for backward compatibility
                    rentalData.put("imageUrl", !downloadUrls.isEmpty() ? downloadUrls.get(0) : null);

                    rentalData.put("description", rental.getDescription());
                    rentalData.put("ownerId", rental.getOwnerId());
                    rentalData.put("createdAt", Timestamp.now());

                    // NEW: Initialize popularity tracking fields
                    rentalData.put("viewCount", 0);
                    rentalData.put("favoriteCount", 0);
                    rentalData.put("bookingCount", 0);
                    rentalData.put("popularityScore", 0.0);

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

    /**
     * Legacy method for single image upload (for backward compatibility)
     */
    public Task<DocumentReference> createRental(Rental rental, Uri imageUri) {
        List<Uri> imageUris = new ArrayList<>();
        imageUris.add(imageUri);
        return createRental(rental, imageUris);
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