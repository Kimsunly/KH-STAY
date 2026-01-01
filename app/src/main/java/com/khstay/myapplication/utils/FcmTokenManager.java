
package com.khstay.myapplication.utils;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class FcmTokenManager {
    private static final String TAG = "FcmTokenManager";

    /** Clears the fcmToken field for the current user document in Firestore. */
    public static void clearTokenForCurrentUser() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", null)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Cleared fcmToken for " + uid))
                .addOnFailureListener(e -> Log.e(TAG, "Failed clearing fcmToken", e));
    }
}

