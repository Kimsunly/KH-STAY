
// app/src/main/java/com/khstay/myapplication/utils/FCMHelper.java
package com.khstay.myapplication.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.khstay.myapplication.MyApp;
import com.khstay.myapplication.R;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FCMHelper {

    private static final String TAG = "FCMHelper";
    private static final OkHttpClient client = new OkHttpClient();
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    /** Call once after login or in MainActivity.onCreate() */
    public static void initializeFCMToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w(TAG, "Fetching FCM token failed", task.getException());
                return;
            }
            String token = task.getResult();
            Log.d(TAG, "FCM Token: " + token);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null && token != null) {
                // Save to users/{uid}.fcmToken
                db.collection("users").document(user.getUid())
                        .update("fcmToken", token)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved"))
                        .addOnFailureListener(e -> Log.e(TAG, "Save token failed", e));

                // Reverse index: tokens/{token}.uid
                Map<String, Object> map = new HashMap<>();
                map.put("uid", user.getUid());
                db.collection("tokens").document(token)
                        .set(map)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Token registry saved"))
                        .addOnFailureListener(e -> Log.e(TAG, "Token registry failed", e));
            }
        });
    }

    /** Function URL from configs.xml */
    private static String functionUrl(Context context) {
        return context.getString(R.string.fcm_function_url);
    }

    /** Base: send via Cloud Run Function */
    public static void sendNotificationToUser(
            @NonNull Context context,
            @NonNull String targetUserId,
            @NonNull String title,
            @NonNull String body,
            String type,
            Map<String, String> additionalData
    ) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("targetUserId", targetUserId);
            payload.put("title", title);
            payload.put("body", body);
            if (type != null) payload.put("type", type);

            if (additionalData != null && !additionalData.isEmpty()) {
                JSONObject dataObj = new JSONObject();
                for (Map.Entry<String, String> entry : additionalData.entrySet()) {
                    dataObj.put(entry.getKey(), entry.getValue());
                }
                payload.put("data", dataObj);
            }

            Log.d(TAG, "sendNotificationToUser -> targetUid=" + targetUserId + ", title=" + title + ", type=" + type);

            RequestBody reqBody = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(functionUrl(context))
                    .post(reqBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "sendNotificationToUser failed", e);
                }

                @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Push requested successfully");
                    } else {
                        String err = response.body() != null ? response.body().string() : ("HTTP " + response.code());
                        Log.e(TAG, "Push request failed: " + err);
                    }
                    response.close();
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error building push payload", e);
        }
    }

    // Booking request
    public static void sendBookingRequestNotification(Context context, String ownerId, String guestName, String rentalTitle) {
        String title = "New Booking Request";
        String msg = guestName + " has requested to book " + rentalTitle;
        sendNotificationToUser(context, ownerId, title, msg, "booking_request", null);
    }
    public static void sendBookingRequestNotification(String ownerId, String guestName, String rentalTitle) {
        sendBookingRequestNotification(MyApp.appContext(), ownerId, guestName, rentalTitle);
    }

    // Booking approved
    public static void sendBookingApprovedNotification(Context context, String userId, String rentalTitle) {
        String title = "Booking Approved! 🎉";
        String msg = "Your booking for " + rentalTitle + " has been approved";
        sendNotificationToUser(context, userId, title, msg, "booking_approved", null);
    }
    public static void sendBookingApprovedNotification(String userId, String rentalTitle) {
        sendBookingApprovedNotification(MyApp.appContext(), userId, rentalTitle);
    }

    // Booking rejected
    public static void sendBookingRejectedNotification(Context context, String userId, String rentalTitle) {
        String title = "Booking Update";
        String msg = "Your booking for " + rentalTitle + " has been rejected";
        sendNotificationToUser(context, userId, title, msg, "booking_rejected", null);
    }
    public static void sendBookingRejectedNotification(String userId, String rentalTitle) {
        sendBookingRejectedNotification(MyApp.appContext(), userId, rentalTitle);
    }

    // Chat
    public static void sendChatMessageNotification(
            Context context,
            String receiverId,
            String senderName,
            String messageText,
            String senderId,
            String senderPhoto
    ) {
        String title = "New message from " + senderName;
        String display = messageText != null && messageText.length() > 100
                ? messageText.substring(0, 100) + "..."
                : (messageText != null ? messageText : "");

        Map<String, String> data = new HashMap<>();
        data.put("otherUserId", senderId);
        data.put("otherUserName", senderName);
        data.put("otherUserPhoto", senderPhoto != null ? senderPhoto : "");

        sendNotificationToUser(context, receiverId, title, display, "chat", data);
    }
    public static void sendChatMessageNotification(
            String receiverId,
            String senderName,
            String messageText,
            String senderId,
            String senderPhoto
    ) {
        sendChatMessageNotification(MyApp.appContext(), receiverId, senderName, messageText, senderId, senderPhoto);
    }
}
