package com.khstay.myapplication.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.khstay.myapplication.R;

public class AboutActivity extends AppCompatActivity {

    private ImageView backButton, appLogo;
    private TextView appName, appVersion, appDescription;
    private TextView contactEmail, contactPhone, websiteLink;
    private TextView privacyPolicy, termsOfService, licenses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        initViews();
        setupClickListeners();
        loadAppInfo();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        appLogo = findViewById(R.id.appLogo);
        appName = findViewById(R.id.appName);
        appVersion = findViewById(R.id.appVersion);
        appDescription = findViewById(R.id.appDescription);
        contactEmail = findViewById(R.id.contactEmail);
        contactPhone = findViewById(R.id.contactPhone);
        websiteLink = findViewById(R.id.websiteLink);
        privacyPolicy = findViewById(R.id.privacyPolicy);
        termsOfService = findViewById(R.id.termsOfService);
        licenses = findViewById(R.id.licenses);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        contactEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:support@khstay.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "KH-Stay Support");
            startActivity(Intent.createChooser(intent, "Send Email"));
        });

        contactPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+855123456789"));
            startActivity(intent);
        });

        websiteLink.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://khstay.com"));
            startActivity(intent);
        });

        privacyPolicy.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://khstay.com/privacy"));
            startActivity(intent);
        });

        termsOfService.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://khstay.com/terms"));
            startActivity(intent);
        });

        licenses.setOnClickListener(v -> {
            // TODO: Show licenses dialog
            android.widget.Toast.makeText(this, "Open source licenses", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void loadAppInfo() {
        appName.setText(R.string.app_name);
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            appVersion.setText("Version " + version);
        } catch (Exception e) {
            appVersion.setText("Version 1.0.0");
        }

        appDescription.setText("KH-Stay is your trusted platform for finding and renting properties in Cambodia. We connect property owners with tenants, making the rental process simple and secure.");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}