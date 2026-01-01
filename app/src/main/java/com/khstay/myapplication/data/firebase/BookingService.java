package com.khstay.myapplication.data.firebase;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khstay.myapplication.utils.PopularityScoreHelper;

import java.util.HashMap;
import java.util.Map;

public class BookingService {

    private static final String TAG = "BookingService";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public BookingService() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    /**
     * Create booking with notification to owner (and track booking count)
     */
    public Task<DocumentReference> createBookingWithNotification(Map<String, Object> booking) {
        String rentalId = (String) booking.get("rentalId");
        if (rentalId == null) {
            return Tasks.forException(new IllegalArgumentException("rentalId is required"));
        }

        return db.collection("rental_houses")
                .document(rentalId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    var rentalDoc = task.getResult();
                    if (!rentalDoc.exists()) {
                        throw new Exception("Rental not found");
                    }

                    String ownerId = rentalDoc.getString("ownerId");
                    if (ownerId == null) {
                        throw new Exception("Owner ID not found in rental document");
                    }

                    String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
                    if (userId == null) {
                        throw new Exception("User not authenticated");
                    }

                    booking.put("ownerId", ownerId);
                    booking.put("userId", userId);

                    return db.collection("bookings").add(booking);
                })
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    DocumentReference bookingRef = task.getResult();

                    String ownerId = (String) booking.get("ownerId");
                    String guestName = (String) booking.get("guestName");
                    String rentalTitle = (String) booking.get("rentalTitle");

                    Map<String, Object> notification = new HashMap<>();
                    notification.put("receiverId", ownerId);
                    notification.put("senderId", auth.getCurrentUser().getUid());
                    notification.put("title", "New Booking Request");
                    notification.put("message", guestName + " wants to book your property: " + rentalTitle);
                    notification.put("type", "booking_request");
                    notification.put("bookingId", bookingRef.getId());
                    notification.put("rentalId", booking.get("rentalId"));
                    notification.put("timestamp", Timestamp.now());
                    notification.put("read", false);

                    return db.collection("users")
                            .document(ownerId)
                            .collection("notifications")
                            .add(notification)
                            .continueWith(notifTask -> {
                                // Don't fail the booking if notification fails
                                if (!notifTask.isSuccessful()) {
                                    Log.e(TAG, "Failed to send notification", notifTask.getException());
                                }
                                return bookingRef;
                            });
                });
    }

    /**
     * Update booking status (simple version - no notification)
     */
    public Task<Void> updateBookingStatus(String bookingId, String newStatus) {
        return db.collection("bookings")
                .document(bookingId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    var bookingDoc = task.getResult();
                    if (!bookingDoc.exists()) {
                        throw new Exception("Booking not found");
                    }

                    String oldStatus = bookingDoc.getString("status");
                    String rentalId = bookingDoc.getString("rentalId");

                    // Update status
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", newStatus);
                    updates.put("updatedAt", Timestamp.now());

                    return db.collection("bookings")
                            .document(bookingId)
                            .update(updates)
                            .continueWithTask(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    throw updateTask.getException();
                                }

                                // NEW: Track booking count for popularity score
                                // Only increment when status changes to "approved" for the first time
                                if ("approved".equals(newStatus) &&
                                        !"approved".equals(oldStatus) &&
                                        rentalId != null) {
                                    PopularityScoreHelper.incrementBookingCount(rentalId);
                                }

                                return Tasks.forResult(null);
                            });
                });
    }

    /**
     * Update booking status WITH notification to guest
     */
    public Task<Void> updateBookingStatus(String bookingId, String newStatus,
                                          String guestUserId, String rentalTitle) {
        return db.collection("bookings")
                .document(bookingId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    var bookingDoc = task.getResult();
                    if (!bookingDoc.exists()) {
                        throw new Exception("Booking not found");
                    }

                    String oldStatus = bookingDoc.getString("status");
                    String rentalId = bookingDoc.getString("rentalId");

                    // Update status
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", newStatus);
                    updates.put("updatedAt", Timestamp.now());

                    return db.collection("bookings")
                            .document(bookingId)
                            .update(updates)
                            .continueWithTask(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    throw updateTask.getException();
                                }

                                // Track booking count for popularity score
                                if ("approved".equals(newStatus) &&
                                        !"approved".equals(oldStatus) &&
                                        rentalId != null) {
                                    PopularityScoreHelper.incrementBookingCount(rentalId);
                                }

                                // Send notification to guest
                                String notificationTitle;
                                String notificationMessage;
                                String notificationType;

                                if ("approved".equals(newStatus)) {
                                    notificationTitle = "Booking Approved! 🎉";
                                    notificationMessage = "Your booking for " + rentalTitle + " has been approved!";
                                    notificationType = "booking_approved";
                                } else if ("rejected".equals(newStatus)) {
                                    notificationTitle = "Booking Update";
                                    notificationMessage = "Your booking for " + rentalTitle + " was not approved.";
                                    notificationType = "booking_rejected";
                                } else {
                                    notificationTitle = "Booking Status Update";
                                    notificationMessage = "Your booking for " + rentalTitle + " status has changed to: " + newStatus;
                                    notificationType = "booking_status_changed";
                                }

                                Map<String, Object> notification = new HashMap<>();
                                notification.put("receiverId", guestUserId);
                                notification.put("senderId", auth.getCurrentUser() != null ?
                                        auth.getCurrentUser().getUid() : "system");
                                notification.put("title", notificationTitle);
                                notification.put("message", notificationMessage);
                                notification.put("type", notificationType);
                                notification.put("bookingId", bookingId);
                                notification.put("rentalId", rentalId);
                                notification.put("timestamp", Timestamp.now());
                                notification.put("read", false);

                                return db.collection("users")
                                        .document(guestUserId)
                                        .collection("notifications")
                                        .add(notification)
                                        .continueWith(notifTask -> {
                                            // Don't fail the status update if notification fails
                                            if (!notifTask.isSuccessful()) {
                                                Log.e(TAG, "Failed to send notification to guest",
                                                        notifTask.getException());
                                            }
                                            return null;
                                        });
                            });
                });
    }

    /**
     * Cancel booking (by renter/guest) - updates status to "cancelled" and notifies owner
     */
    public Task<Void> cancelBooking(String bookingId) {
        return db.collection("bookings")
                .document(bookingId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    var bookingDoc = task.getResult();
                    if (!bookingDoc.exists()) {
                        throw new Exception("Booking not found");
                    }

                    String ownerId = bookingDoc.getString("ownerId");
                    String guestName = bookingDoc.getString("guestName");
                    String rentalTitle = bookingDoc.getString("rentalTitle");
                    String rentalId = bookingDoc.getString("rentalId");
                    String oldStatus = bookingDoc.getString("status");

                    // Update status to cancelled
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", "cancelled");
                    updates.put("cancelledAt", Timestamp.now());
                    updates.put("updatedAt", Timestamp.now());

                    return db.collection("bookings")
                            .document(bookingId)
                            .update(updates)
                            .continueWithTask(updateTask -> {
                                if (!updateTask.isSuccessful()) {
                                    throw updateTask.getException();
                                }

                                // If rental was pending/approved, revert status to active
                                if ("approved".equals(oldStatus) && rentalId != null) {
                                    Map<String, Object> rentalUpdates = new HashMap<>();
                                    rentalUpdates.put("status", "active");
                                    rentalUpdates.put("updatedAt", Timestamp.now());

                                    db.collection("rental_houses")
                                            .document(rentalId)
                                            .update(rentalUpdates)
                                            .addOnFailureListener(e ->
                                                    Log.e(TAG, "Failed to update rental status", e));
                                }

                                // Notify owner about cancellation
                                if (ownerId != null) {
                                    Map<String, Object> notification = new HashMap<>();
                                    notification.put("receiverId", ownerId);
                                    notification.put("senderId", auth.getCurrentUser() != null ?
                                            auth.getCurrentUser().getUid() : "system");
                                    notification.put("title", "Booking Cancelled");
                                    notification.put("message", guestName + " cancelled their booking for " + rentalTitle);
                                    notification.put("type", "booking_cancelled");
                                    notification.put("bookingId", bookingId);
                                    notification.put("rentalId", rentalId);
                                    notification.put("timestamp", Timestamp.now());
                                    notification.put("read", false);

                                    return db.collection("users")
                                            .document(ownerId)
                                            .collection("notifications")
                                            .add(notification)
                                            .continueWith(notifTask -> {
                                                if (!notifTask.isSuccessful()) {
                                                    Log.e(TAG, "Failed to send cancellation notification",
                                                            notifTask.getException());
                                                }
                                                return null;
                                            });
                                }

                                return Tasks.forResult(null);
                            });
                });
    }

    /**
     * Delete booking completely (for owner or guest after reject/cancel)
     */
    public Task<Void> deleteBooking(String bookingId) {
        return db.collection("bookings")
                .document(bookingId)
                .delete()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "Failed to delete booking", task.getException());
                        throw task.getException();
                    }
                    Log.d(TAG, "Booking deleted successfully: " + bookingId);
                    return null;
                });
    }

    /**
     * Delete booking with notification (optional - notifies the other party)
     */
    public Task<Void> deleteBookingWithNotification(String bookingId, String notifyUserId,
                                                    String notifyUserRole, String rentalTitle) {
        return db.collection("bookings")
                .document(bookingId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();

                    var bookingDoc = task.getResult();
                    if (!bookingDoc.exists()) {
                        throw new Exception("Booking not found");
                    }

                    String status = bookingDoc.getString("status");

                    // Delete the booking
                    return db.collection("bookings")
                            .document(bookingId)
                            .delete()
                            .continueWithTask(deleteTask -> {
                                if (!deleteTask.isSuccessful()) {
                                    throw deleteTask.getException();
                                }

                                // Send notification if needed
                                if (notifyUserId != null && !notifyUserId.isEmpty()) {
                                    String message;
                                    if ("owner".equals(notifyUserRole)) {
                                        message = "The owner has removed the booking request for " + rentalTitle;
                                    } else {
                                        message = "A booking request for " + rentalTitle + " has been removed";
                                    }

                                    Map<String, Object> notification = new HashMap<>();
                                    notification.put("receiverId", notifyUserId);
                                    notification.put("senderId", auth.getCurrentUser() != null ?
                                            auth.getCurrentUser().getUid() : "system");
                                    notification.put("title", "Booking Removed");
                                    notification.put("message", message);
                                    notification.put("type", "booking_deleted");
                                    notification.put("timestamp", Timestamp.now());
                                    notification.put("read", false);

                                    return db.collection("users")
                                            .document(notifyUserId)
                                            .collection("notifications")
                                            .add(notification)
                                            .continueWith(notifTask -> {
                                                if (!notifTask.isSuccessful()) {
                                                    Log.e(TAG, "Failed to send deletion notification",
                                                            notifTask.getException());
                                                }
                                                return null;
                                            });
                                }

                                return Tasks.forResult(null);
                            });
                });
    }
}