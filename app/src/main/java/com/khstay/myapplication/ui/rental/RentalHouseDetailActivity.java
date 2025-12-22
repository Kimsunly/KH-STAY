package com.khstay.myapplication.ui.rental;


import android.view.View; // avoids "Cannot resolve symbol View" in some cases

import androidx.annotation.NonNull;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khstay.myapplication.R;

/**
 * Detail screen that shows one rental house.
 * Expects Intent extra: EXTRA_RENTAL_ID => Firestore doc id in collection "rental_houses".
 */
public class RentalHouseDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_RENTAL_ID = "RENTAL_ID";
    private static final int REQ_LOCATION = 1011;

    // Firestore
    private FirebaseFirestore db;

    // Views
    private ImageView ivHeaderImage, ivOwnerAvatar;
    private ImageButton btnBack, btnFavorite, btnCallOwner, btnChatOwner;
    private TextView tvTitle, tvPrice, tvLocation, tvDescription, tvOwnerName, tvOwnerRole;
    private Button btnViewMore, btnBookNow, btnOpenMap;
    private ProgressBar progressBar;

    // Map
    private MapView mapView;
    private GoogleMap googleMap;

    // State
    private String rentalId;
    private Rental rental;
    private boolean expanded = false;

    // Optional owner fields
    private String ownerPhone = null;
    private String ownerName = null;
    private String ownerAvatarUrl = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_house_detail);

        db = FirebaseFirestore.getInstance();

        // ---- Get doc id ----
        rentalId = getIntent().getStringExtra(EXTRA_RENTAL_ID);
        if (TextUtils.isEmpty(rentalId)) {
            Toast.makeText(this, "Missing rental id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // ---- Bind views ----
        ivHeaderImage = findViewById(R.id.iv_header_image);
        btnBack       = findViewById(R.id.btn_back);
        btnFavorite   = findViewById(R.id.btn_favorite);

        tvTitle       = findViewById(R.id.tv_title);
        tvPrice       = findViewById(R.id.tv_price);
        tvLocation    = findViewById(R.id.tv_location);
        tvDescription = findViewById(R.id.tv_description);

        btnViewMore   = findViewById(R.id.btn_view_more);
        btnBookNow    = findViewById(R.id.btn_book_now);
        btnOpenMap    = findViewById(R.id.btn_open_map);

        ivOwnerAvatar = findViewById(R.id.iv_owner_avatar);
        tvOwnerName   = findViewById(R.id.tv_owner_name);
        tvOwnerRole   = findViewById(R.id.tv_owner_role);
        btnCallOwner  = findViewById(R.id.btn_call_owner);
        btnChatOwner  = findViewById(R.id.btn_chat_owner);

        progressBar   = findViewById(R.id.progress_bar);
        mapView       = findViewById(R.id.mapView);

        // ---- MapView lifecycle ----
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // ---- Actions ----
        btnBack.setOnClickListener(v -> onBackPressed());

        btnFavorite.setOnClickListener(v -> {
            if (rental != null) {
                rental.setFavorite(!rental.isFavorite());
                updateFavoriteIcon(rental.isFavorite());
                // TODO: Persist favorite if needed (e.g., users/{uid}/favorites/{rentalId})
            }
        });

        btnViewMore.setOnClickListener(v -> toggleDescription());

        btnBookNow.setOnClickListener(v ->
                Toast.makeText(this, "Booking flow not implemented yet.", Toast.LENGTH_SHORT).show()
        );

        btnOpenMap.setOnClickListener(v -> openExternalNavigation());

        btnCallOwner.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(ownerPhone)) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ownerPhone)));
            } else {
                Toast.makeText(this, "Owner phone not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnChatOwner.setOnClickListener(v ->
                Toast.makeText(this, "Chat flow not implemented yet.", Toast.LENGTH_SHORT).show()
        );

        // ---- Fetch data ----
        loadRental();
    }

    private void loadRental() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("rental_houses")
                .document(rentalId)
                .get()
                .addOnSuccessListener(d -> {
                    progressBar.setVisibility(View.GONE);
                    if (!d.exists()) {
                        Toast.makeText(this, "Rental not found", Toast.LENGTH_SHORT).show();
                        finish(); return;
                    }
                    rental = d.toObject(Rental.class);
                    if (rental == null) {
                        Toast.makeText(this, "Failed to parse rental document", Toast.LENGTH_SHORT).show();
                        finish(); return;
                    }
                    rental.setId(d.getId());
                    bindRentalToViews(rental);
                    fetchOwnerIfAvailable(rental.getOwnerId());
                    updateMapMarkerIfReady(); // in case map is already ready
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindRentalToViews(@NonNull Rental r) {
        // Header image
        String url = r.getImageUrl();
        if (url != null && !url.trim().isEmpty()) {
            Glide.with(this)
                    .load(url)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(ivHeaderImage);
        } else if (r.getImageResId() != 0) {
            ivHeaderImage.setImageResource(r.getImageResId());
        } else {
            ivHeaderImage.setImageResource(R.drawable.ic_placeholder);
        }

        tvTitle.setText(safe(r.getTitle()));
        tvPrice.setText(formatPrice(r.getPrice()));
        tvLocation.setText(safe(r.getLocation()));

        tvDescription.setText(safe(r.getDescription()));
        applyCollapsedDescription();

        updateFavoriteIcon(r.isFavorite());
    }

    private void fetchOwnerIfAvailable(@Nullable String ownerId) {
        if (TextUtils.isEmpty(ownerId)) {
            tvOwnerName.setText(R.string.owner_unknown);
            tvOwnerRole.setText(R.string.owner_role);
            ivOwnerAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }
        db.collection("users").document(ownerId)
                .get()
                .addOnSuccessListener((DocumentSnapshot d) -> {
                    if (d.exists()) {
                        ownerName = d.getString("displayName");
                        ownerPhone = d.getString("phone");
                        ownerAvatarUrl = d.getString("photoUrl");

                        tvOwnerName.setText(!TextUtils.isEmpty(ownerName) ? ownerName : getString(R.string.owner_unknown));
                        tvOwnerRole.setText(R.string.owner_role);

                        if (!TextUtils.isEmpty(ownerAvatarUrl)) {
                            Glide.with(this)
                                    .load(ownerAvatarUrl)
                                    .placeholder(R.drawable.ic_avatar_placeholder)
                                    .error(R.drawable.ic_avatar_placeholder)
                                    .circleCrop()
                                    .into(ivOwnerAvatar);
                        } else {
                            ivOwnerAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                        }
                    } else {
                        tvOwnerName.setText(R.string.owner_unknown);
                        tvOwnerRole.setText(R.string.owner_role);
                        ivOwnerAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                    }
                })
                .addOnFailureListener(e -> {
                    tvOwnerName.setText(R.string.owner_unknown);
                    tvOwnerRole.setText(R.string.owner_role);
                    ivOwnerAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
                });
    }

    // Map ready callback
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        // Basic UI
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // My location (optional) — requires runtime permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{ Manifest.permission.ACCESS_FINE_LOCATION },
                    REQ_LOCATION);
        }

        updateMapMarkerIfReady(); // place marker after map init
    }

    private void updateMapMarkerIfReady() {
        if (googleMap == null || rental == null) return;

        Double lat = rental.getLatitude();
        Double lng = rental.getLongitude();
        if (lat == null || lng == null) return;

        LatLng house = new LatLng(lat, lng);
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(house)
                .title(safe(rental.getTitle())));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(house, 15f));
    }

    private void openExternalNavigation() {
        if (rental == null || rental.getLatitude() == null || rental.getLongitude() == null) {
            Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            return;
        }
        double lat = rental.getLatitude();
        double lng = rental.getLongitude();
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(mapIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Google Maps app not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDescription() {
        expanded = !expanded;
        if (expanded) {
            tvDescription.setMaxLines(Integer.MAX_VALUE);
            tvDescription.setEllipsize(null);
            btnViewMore.setText(R.string.view_less);
        } else {
            applyCollapsedDescription();
        }
    }

    private void applyCollapsedDescription() {
        tvDescription.setMaxLines(4);
        tvDescription.setEllipsize(TextUtils.TruncateAt.END);
        btnViewMore.setText(R.string.view_more);
    }

    private void updateFavoriteIcon(boolean favorite) {
        btnFavorite.setImageResource(favorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
    }

    private String formatPrice(@Nullable Double price) {
        int p = price != null ? price.intValue() : 0;
        return getString(R.string.price_template, p);
    }

    private String safe(@Nullable String s) { return s == null ? "" : s; }

    // ---- MapView lifecycle forwarding ----
    @Override protected void onStart()   { super.onStart();   mapView.onStart(); }
    @Override protected void onResume()  { super.onResume();  mapView.onResume(); }
    @Override protected void onPause()   { mapView.onPause(); super.onPause(); }
    @Override protected void onStop()    { mapView.onStop();  super.onStop(); }
    @Override protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override public void onLowMemory()  { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && googleMap != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                }
            }
        }
    }
}
