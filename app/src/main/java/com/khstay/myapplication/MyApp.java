
package com.khstay.myapplication;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.storage.FirebaseStorage;

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseApp.initializeApp(this);

        // Detect debug vs release without BuildConfig
        // boolean isDebug = (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

//        FirebaseAppCheck appCheck = FirebaseAppCheck.getInstance();
//        if (isDebug) {
//            appCheck.installAppCheckProviderFactory(
//                    DebugAppCheckProviderFactory.getInstance()
//            );
//        } else {
//            appCheck.installAppCheckProviderFactory(
//                    PlayIntegrityAppCheckProviderFactory.getInstance()
//            );
//        }

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
}
