
// Add imports at the very top of the file
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

val localProps = Properties().apply {
    // Safely load local.properties
    val lp = rootProject.file("local.properties")
    if (lp.exists()) {
        load(FileInputStream(lp))
    }
}

android {
    namespace = "com.khstay.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.khstay.myapplication"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Manifest placeholders (with null-safe fallback)
        manifestPlaceholders["MAPS_API_KEY"] = localProps.getProperty("MAPS_API_KEY") ?: ""
        manifestPlaceholders["facebook_app_id"] = localProps.getProperty("facebookAppId") ?: ""
        manifestPlaceholders["facebook_client_token"] = localProps.getProperty("facebookClientToken") ?: ""

        // BuildConfig: Cloud Function URL for sending messages (HTTP v1)
        buildConfigField("String", "FCM_FUNCTION_URL", "\"https://us-central1-kh-stay.cloudfunctions.net/sendNotification\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // If you ever need a different function URL for debug, you can override here:
            // buildConfigField("String", "FCM_FUNCTION_URL", "\"https://us-central1-kh-stay.cloudfunctions.net/sendNotification\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    // Firebase BOM - manage versions centrally
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))

    // Firebase core libs
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging:23.4.0") // OK to pin if needed

    // OkHttp for calling your Cloud Function (HTTP v1)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ViewPager2 for image slider
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    // Material Components for TabLayout
    implementation("com.google.android.material:material:1.9.0")
    // Google / Android UI libs
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Facebook
    implementation("com.facebook.android:facebook-login:18.1.3")
    // UI helpers
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // App Check (you already use these in your project)
    implementation(libs.firebase.appcheck.debug)
    implementation(libs.firebase.appcheck.playintegrity)

    // Misc
    implementation("androidx.viewpager:viewpager:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
