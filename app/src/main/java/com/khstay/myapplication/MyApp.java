
package com.khstay.myapplication;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.storage.FirebaseStorage;

public class MyApp extends Application {

    private static MyApp instance;

    @Override
    public void onCreate() {
        super.onCreate();

        // Keep a static reference to the Application for app-wide context
        instance = this;

        FirebaseApp.initializeApp(this);

        boolean isDebug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
        appCheck.installAppCheckProviderFactory(
                isDebug
                        ? DebugAppCheckProviderFactory.getInstance()
                        : PlayIntegrityAppCheckProviderFactory.getInstance()
        );

        // OPTIONAL: only needed if your google-services.json does NOT have storage_bucket yet
        FirebaseStorage storageForKhStay = FirebaseStorage.getInstance("gs://kh-stay.firebasestorage.app");

        String defaultBucket = FirebaseStorage.getInstance().getReference().getBucket();
        Log.d("Storage", "Default bucket via SDK: " + defaultBucket);
        Log.d("Storage", "Override bucket via SDK: " + storageForKhStay.getReference().getBucket());
    }

    /** Get the Application singleton */
    public static MyApp get() {
        return instance;
    }

    /** Get the application-level Context safely anywhere */
    public static Context appContext() {
        return instance.getApplicationContext();
    }
}
