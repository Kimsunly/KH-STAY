
package com.khstay.myapplication.data.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.khstay.myapplication.MainActivity;
import com.khstay.myapplication.R;
import com.khstay.myapplication.ui.chat.ChatActivity;
import com.khstay.myapplication.ui.profile.NotificationActivity;
import com.khstay.myapplication.ui.rental.BookingRequestsActivity;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFMS";
    private static final String CHANNEL_ID = "khstay_notifications";
    private static final String CHANNEL_NAME = "KH-Stay Notifications";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "New FCM Token: " + token);
        // Save/refresh token whenever FCM rotates it
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token updated"))
                    .addOnFailureListener(e -> Log.e(TAG, "Token update failed", e));
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();

        // 🔒 Device-side safety: drop if not intended for current UID
        String targetUid = data != null ? data.get("targetUid") : null;
        String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (targetUid != null && currentUid != null && !targetUid.equals(currentUid)) {
            Log.d(TAG, "Drop notification: targetUid=" + targetUid + " currentUid=" + currentUid);
            return;
        }

        // Prefer OS notification payload; fallback to data
        String title = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getTitle()
                : (data != null ? data.get("title") : null);

        String body = remoteMessage.getNotification() != null
                ? remoteMessage.getNotification().getBody()
                : (data != null ? (data.get("message") != null ? data.get("message") : data.get("body")) : null);

        String type = data != null ? data.get("type") : null;

        if (title == null) title = "KH-Stay";
        if (body == null)  body  = "";

        showNotification(title, body, type, data);
    }

    private void showNotification(String title, String message, String type, Map<String, String> data) {
        Intent intent = getIntentForNotificationType(type, data);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Color by type
        if (type != null) {
            int colorId = R.color.primary;
            switch (type) {
                case "booking_approved": colorId = R.color.success_green; break;
                case "booking_rejected": colorId = R.color.error_red; break;
                case "chat":             colorId = R.color.primary; break;
                case "booking_request":  colorId = R.color.primary; break;
            }
            nb.setColor(ContextCompat.getColor(this, colorId));
        }

        // ✅ Chat: use MessagingStyle and sender avatar as large icon
        if ("chat".equals(type)) {
            String otherUserName = data != null ? data.get("otherUserName") : null;
            String otherUserPhoto = data != null ? data.get("otherUserPhoto") : null;

            // MessagingStyle (richer chat look)
            NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(
                    otherUserName != null ? otherUserName : "User"
            ).addMessage(message, System.currentTimeMillis(), otherUserName != null ? otherUserName : "User");
            nb.setStyle(style);

            // If we have a sender photo URL, load as large icon
            if (otherUserPhoto != null && !otherUserPhoto.isEmpty()) {
                try {
                    Glide.with(getApplicationContext())
                            .asBitmap()
                            .load(otherUserPhoto)
                            .circleCrop()
                            .into(new CustomTarget<Bitmap>() {
                                @Override
                                public void onResourceReady(@NonNull Bitmap resource,
                                                            @Nullable Transition<? super Bitmap> transition) {
                                    nb.setLargeIcon(resource);
                                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                    nm.notify(getNotificationId(type), nb.build());
                                }
                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) { /* no-op */ }
                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    // Fallback: notify without large icon
                                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                    nm.notify(getNotificationId(type), nb.build());
                                }
                            });
                    // Return here: we'll notify after image loads
                    return;
                } catch (Exception e) {
                    Log.w(TAG, "Failed to load chat avatar, proceeding without large icon", e);
                }
            }

            // No photo: show default style
            nb.setStyle(style);
        } else {
            // Non-chat: default BigText style
            nb.setStyle(new NotificationCompat.BigTextStyle().bigText(message));
        }

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(getNotificationId(type), nb.build());
    }

    private Intent getIntentForNotificationType(String type, Map<String, String> data) {
        if (type == null) return new Intent(this, MainActivity.class);

        switch (type) {
            case "booking_request":
                return new Intent(this, BookingRequestsActivity.class);

            case "booking_approved":
            case "booking_rejected":
                return new Intent(this, NotificationActivity.class);

            case "chat":
                Intent i = new Intent(this, ChatActivity.class);
                if (data != null) {
                    i.putExtra(ChatActivity.EXTRA_OTHER_USER_ID,   data.get("otherUserId"));
                    i.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, data.get("otherUserName"));
                    i.putExtra(ChatActivity.EXTRA_OTHER_USER_PHOTO,data.get("otherUserPhoto"));
                }
                return i;

            default:
                return new Intent(this, MainActivity.class);
        }
    }

    private int getNotificationId(String type) {
        if (type == null) return 0;
        switch (type) {
            case "booking_request":  return 1001;
            case "booking_approved": return 1002;
            case "booking_rejected": return 1003;
            case "chat":             return (int) System.currentTimeMillis();
            default:                 return 0;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Notifications for bookings, messages, and updates");
            ch.enableVibration(true);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }
}
