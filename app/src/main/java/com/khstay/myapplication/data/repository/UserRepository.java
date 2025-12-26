package com.khstay.myapplication.data.repository;

import android.net.Uri;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.khstay.myapplication.data.firebase.FavoriteService;
import com.khstay.myapplication.data.firebase.NotificationService;
import com.khstay.myapplication.data.firebase.RecentViewedService;
import com.khstay.myapplication.data.firebase.UserService;

/**
 * Repository for user-related operations
 */
public class UserRepository {

    private final UserService userService;
    private final RecentViewedService recentViewedService;
    private final NotificationService notificationService;
    private final FavoriteService favoriteService;

    public UserRepository() {
        this.userService = new UserService();
        this.recentViewedService = new RecentViewedService();
        this.notificationService = new NotificationService();
        this.favoriteService = new FavoriteService();
    }

    // ===== User Profile =====

    public Task<DocumentSnapshot> getCurrentUserData() {
        return userService.getCurrentUserData();
    }

    public Task<Void> updateUserProfile(String displayName, String phone, String birthDate) {
        return userService.updateUserProfile(displayName, phone, birthDate);
    }

    public Task<Void> updateEmail(String newEmail) {
        return userService.updateEmail(newEmail);
    }

    public Task<Void> updatePassword(String newPassword) {
        return userService.updatePassword(newPassword);
    }

    public Task<String> uploadProfilePicture(Uri imageUri) {
        return userService.uploadProfilePicture(imageUri);
    }

    public Task<Void> updateSettings(String key, Object value) {
        return userService.updateSettings(key, value);
    }

    public Task<DocumentSnapshot> getUserById(String userId) {
        return userService.getUserById(userId);
    }

    // ===== Recent Viewed =====

    public Task<Void> addRecentView(String propertyId, String propertyName,
                                    String propertyImage, String propertyPrice,
                                    String propertyLocation) {
        return recentViewedService.addRecentView(propertyId, propertyName,
                propertyImage, propertyPrice, propertyLocation);
    }

    public Query getRecentViewed() {
        return recentViewedService.getRecentViewed();
    }

    public Task<Void> clearRecentViews() {
        return recentViewedService.clearRecentViews();
    }

    // ===== Favorites =====

    public Task<Void> addFavorite(String propertyId, String propertyName,
                                  String propertyImage, String propertyPrice,
                                  String propertyLocation) {
        return favoriteService.addFavorite(propertyId, propertyName,
                propertyImage, propertyPrice, propertyLocation);
    }

    public Task<Void> removeFavorite(String propertyId) {
        return favoriteService.removeFavorite(propertyId);
    }

    public Task<Boolean> isFavorite(String propertyId) {
        return favoriteService.isFavorite(propertyId);
    }

    public Query getFavorites() {
        return favoriteService.getFavorites();
    }

    public Task<Void> clearFavorites() {
        return favoriteService.clearFavorites();
    }

    // ===== Notifications =====

    public Task<Void> createNotification(String userId, String title,
                                         String message, String type) {
        return notificationService.createNotification(userId, title, message, type);
    }

    public Query getNotifications() {
        return notificationService.getNotifications();
    }

    public Task<Void> markNotificationAsRead(String notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    public Task<Void> markAllNotificationsAsRead() {
        return notificationService.markAllAsRead();
    }

    public Task<Void> deleteNotification(String notificationId) {
        return notificationService.deleteNotification(notificationId);
    }

    public Task<Integer> getUnreadNotificationCount() {
        return notificationService.getUnreadCount();
    }
}