package com.khstay.myapplication.ui.rental;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khstay.myapplication.R;
import com.khstay.myapplication.common.IntentKeys;
import com.khstay.myapplication.data.repository.UserRepository;
import com.khstay.myapplication.ui.rental.adapters.ImageSliderAdapter;
import com.khstay.myapplication.ui.rental.model.Rental;
import com.google.firebase.firestore.FieldValue;
import com.khstay.myapplication.utils.PopularityScoreHelper;
import java.util.ArrayList;
import java.util.List;

public class RentalHouseDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_RENTAL_ID = "RENTAL_ID";
    private static final int REQ_LOCATION = 1011;
    private static final String TAG = "RentalDetailActivity";

    private FirebaseFirestore db;
    private UserRepository userRepository;

    // Views
    private ViewPager2 viewPagerImages;
    private TabLayout tabIndicator;
    private ImageView ivOwnerAvatar;
    private ImageButton btnBack, btnFavorite, btnCallOwner, btnChatOwner, btnShare;
    private TextView tvTitle, tvPrice, tvLocation, tvDescription;
    private TextView tvOwnerName, tvOwnerRole, tvImageCounter;
    private Button btnViewMore, btnBookNow, btnOpenMap;
    private ProgressBar progressBar;
    private MapView mapView;
    private GoogleMap googleMap;
    private ShimmerFrameLayout shimmerDetail;
    private View contentLayout;

    // Data
    private String rentalId;
    private Rental rental;
    private boolean expanded = false;
    private boolean isFavorited = false;

    private String ownerPhone = null;
    private String ownerName = null;
    private String ownerAvatarUrl = null;

    private ImageSliderAdapter imageSliderAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rental_house_detail);

        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();

        rentalId = getIntent().getStringExtra(IntentKeys.RENTAL_ID);
        if (TextUtils.isEmpty(rentalId)) {
            rentalId = getIntent().getStringExtra(IntentKeys.PROPERTY_ID);
        }
        if (TextUtils.isEmpty(rentalId)) {
            rentalId = getIntent().getStringExtra(EXTRA_RENTAL_ID);
        }
        if (TextUtils.isEmpty(rentalId)) {
            Toast.makeText(this, "Missing rental ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();

        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }

        loadRental();
        checkIfFavorited();
    }

    private void initializeViews() {
        viewPagerImages = findViewById(R.id.viewPagerImages);
        tabIndicator = findViewById(R.id.tabIndicator);
        tvImageCounter = findViewById(R.id.tvImageCounter);

        btnBack = findViewById(R.id.btn_back);
        btnFavorite = findViewById(R.id.btn_favorite);
        btnShare = findViewById(R.id.btn_share);

        tvTitle = findViewById(R.id.tv_title);
        tvPrice = findViewById(R.id.tv_price);
        tvLocation = findViewById(R.id.tv_location);
        tvDescription = findViewById(R.id.tv_description);

        btnViewMore = findViewById(R.id.btn_view_more);
        btnBookNow = findViewById(R.id.btn_book_now);
        btnOpenMap = findViewById(R.id.btn_open_map);

        ivOwnerAvatar = findViewById(R.id.iv_owner_avatar);
        tvOwnerName = findViewById(R.id.tv_owner_name);
        tvOwnerRole = findViewById(R.id.tv_owner_role);
        btnCallOwner = findViewById(R.id.btn_call_owner);
        btnChatOwner = findViewById(R.id.btn_chat_owner);

        progressBar = findViewById(R.id.progress_bar);
        mapView = findViewById(R.id.mapView);

        try {
            shimmerDetail = findViewById(R.id.shimmerDetail);
            contentLayout = findViewById(R.id.contentLayout);
        } catch (Exception e) {
            shimmerDetail = null;
            contentLayout = null;
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnShare.setOnClickListener(v -> shareRental());

        btnViewMore.setOnClickListener(v -> toggleDescription());

        btnBookNow.setOnClickListener(v -> {
            if (rental != null) {
                Intent intent = new Intent(this, BookingActivity.class);
                intent.putExtra(BookingActivity.EXTRA_RENTAL_ID, rentalId);
                intent.putExtra(BookingActivity.EXTRA_RENTAL_TITLE, rental.getTitle());
                intent.putExtra(BookingActivity.EXTRA_RENTAL_PRICE,
                        rental.getPrice() != null ? rental.getPrice() : 0.0);
                intent.putExtra(BookingActivity.EXTRA_OWNER_ID, rental.getOwnerId());
                startActivity(intent);
            }
        });

        btnOpenMap.setOnClickListener(v -> openExternalNavigation());

        btnCallOwner.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(ownerPhone)) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + ownerPhone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Owner phone not available", Toast.LENGTH_SHORT).show();
            }
        });

        btnChatOwner.setOnClickListener(v -> {
            if (rental != null && !TextUtils.isEmpty(rental.getOwnerId())) {
                Intent intent = new Intent(this, com.khstay.myapplication.ui.chat.ChatActivity.class);
                intent.putExtra(com.khstay.myapplication.ui.chat.ChatActivity.EXTRA_OTHER_USER_ID, rental.getOwnerId());
                intent.putExtra(com.khstay.myapplication.ui.chat.ChatActivity.EXTRA_OTHER_USER_NAME,
                        ownerName != null ? ownerName : "Owner");
                intent.putExtra(com.khstay.myapplication.ui.chat.ChatActivity.EXTRA_OTHER_USER_PHOTO, ownerAvatarUrl);
                intent.putExtra(com.khstay.myapplication.ui.chat.ChatActivity.EXTRA_RENTAL_ID, rentalId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Owner information not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareRental() {
        if (rental == null) return;

        String shareText = "Check out this property on KHSTAY!\n\n" +
                "🏠 " + rental.getTitle() + "\n" +
                "📍 " + rental.getLocation() + "\n" +
                "💰 $" + (rental.getPrice() != null ? rental.getPrice().intValue() : 0) + "/month\n\n" +
                (rental.getDescription() != null ? rental.getDescription() + "\n\n" : "") +
                "View details: [Your App Link or Website]";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, rental.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        try {
            startActivity(Intent.createChooser(shareIntent, "Share property via"));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No apps available to share", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfFavorited() {
        if (userRepository.isFavorite(rentalId) != null) {
            userRepository.isFavorite(rentalId)
                    .addOnSuccessListener(isFav -> {
                        isFavorited = isFav;
                        updateFavoriteIcon();
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to check favorite status", e));
        }
    }

    private void toggleFavorite() {
        if (rental == null) return;

        if (isFavorited) {
            userRepository.removeFavorite(rentalId)
                    .addOnSuccessListener(aVoid -> {
                        isFavorited = false;
                        updateFavoriteIcon();
                        Toast.makeText(this, "Removed from favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove favorite", e);
                        Toast.makeText(this, "Failed to update favorites", Toast.LENGTH_SHORT).show();
                    });
        } else {
            String propertyPrice = "$" + (rental.getPrice() != null ? rental.getPrice().intValue() : 0) + "/month";
            userRepository.addFavorite(rentalId, rental.getTitle(), rental.getImageUrl(),
                            propertyPrice, rental.getLocation())
                    .addOnSuccessListener(aVoid -> {
                        isFavorited = true;
                        updateFavoriteIcon();
                        Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add favorite", e);
                        Toast.makeText(this, "Failed to update favorites", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateFavoriteIcon() {
        btnFavorite.setImageResource(isFavorited ?
                R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
    }

    private void loadRental() {
        if (shimmerDetail != null) {
            shimmerDetail.startShimmer();
            shimmerDetail.setVisibility(View.VISIBLE);
        }

        if (contentLayout != null) {
            contentLayout.setVisibility(View.GONE);
        }
        progressBar.setVisibility(View.GONE);

        db.collection("rental_houses")
                .document(rentalId)
                .get()
                .addOnSuccessListener(d -> {
                    if (shimmerDetail != null) {
                        shimmerDetail.stopShimmer();
                        shimmerDetail.setVisibility(View.GONE);
                    }

                    if (contentLayout != null) {
                        contentLayout.setVisibility(View.VISIBLE);
                    }

                    if (!d.exists()) {
                        Toast.makeText(this, "Rental not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    rental = d.toObject(Rental.class);
                    if (rental == null) {
                        Toast.makeText(this, "Failed to load rental", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    rental.setId(d.getId());
                    bindRentalToViews(rental);
                    setupImageSlider(rental);
                    fetchOwnerIfAvailable(rental.getOwnerId());
                    updateMapMarkerIfReady();
                    trackRecentView(rental);
                })
                .addOnFailureListener(e -> {
                    if (shimmerDetail != null) {
                        shimmerDetail.stopShimmer();
                        shimmerDetail.setVisibility(View.GONE);
                    }
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void setupImageSlider(Rental rental) {
        List<String> imageUrls = rental.getImageUrls();

        if (imageUrls == null || imageUrls.isEmpty()) {
            // Fallback to single image if no imageUrls
            imageUrls = new ArrayList<>();
            if (rental.getImageUrl() != null && !rental.getImageUrl().isEmpty()) {
                imageUrls.add(rental.getImageUrl());
            }
        }

        // Make imageUrls effectively final by storing size
        final int imageCount = imageUrls.size();
        final List<String> finalImageUrls = imageUrls; // Create final reference

        imageSliderAdapter = new ImageSliderAdapter(this, finalImageUrls);
        viewPagerImages.setAdapter(imageSliderAdapter);

        // Show indicator only if multiple images
        if (imageCount > 1) {
            tabIndicator.setVisibility(View.VISIBLE);
            tvImageCounter.setVisibility(View.VISIBLE);

            // Attach TabLayout to ViewPager2
            new TabLayoutMediator(tabIndicator, viewPagerImages, (tab, position) -> {
                // Optional: customize tab appearance
            }).attach();

            // Update counter on page change
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    tvImageCounter.setText((position + 1) + " / " + imageCount);
                }
            });

            tvImageCounter.setText("1 / " + imageCount);
        } else {
            tabIndicator.setVisibility(View.GONE);
            tvImageCounter.setVisibility(View.GONE);
        }
    }

    private void trackRecentView(Rental rental) {
        if (rental == null) return;

        String canonicalRentalId = rental.getId();
        String propertyName = rental.getTitle();
        String propertyImage = rental.getImageUrl();
        String propertyPrice = "$" + (rental.getPrice() != null ? rental.getPrice().intValue() : 0) + "/month";
        String propertyLocation = rental.getLocation();

        // Track in user's recent views (existing functionality)
        userRepository.addRecentView(canonicalRentalId, propertyName, propertyImage, propertyPrice, propertyLocation)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Recent view tracked successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to track recent view", e));

        // NEW: Increment view count and update popularity score (HYBRID SYSTEM)
        PopularityScoreHelper.incrementViewCount(canonicalRentalId);
    }

    private void bindRentalToViews(@NonNull Rental r) {
        tvTitle.setText(safe(r.getTitle()));
        tvPrice.setText(formatPrice(r.getPrice()));
        tvLocation.setText(safe(r.getLocation()));
        tvDescription.setText(safe(r.getDescription()));
        applyCollapsedDescription();
    }

    private void fetchOwnerIfAvailable(@Nullable String ownerId) {
        if (TextUtils.isEmpty(ownerId)) {
            setDefaultOwnerInfo();
            return;
        }

        userRepository.getUserById(ownerId)
                .addOnSuccessListener((DocumentSnapshot d) -> {
                    if (d.exists()) {
                        ownerName = d.getString("displayName");
                        ownerPhone = d.getString("phone");
                        ownerAvatarUrl = d.getString("photoUrl");

                        tvOwnerName.setText(!TextUtils.isEmpty(ownerName) ?
                                ownerName : getString(R.string.owner_unknown));
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
                        setDefaultOwnerInfo();
                    }
                })
                .addOnFailureListener(e -> setDefaultOwnerInfo());
    }

    private void setDefaultOwnerInfo() {
        tvOwnerName.setText(R.string.owner_unknown);
        tvOwnerRole.setText(R.string.owner_role);
        ivOwnerAvatar.setImageResource(R.drawable.ic_avatar_placeholder);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }

        updateMapMarkerIfReady();
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
            Toast.makeText(this, "Google Maps not found", Toast.LENGTH_SHORT).show();
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

    private String formatPrice(@Nullable Double price) {
        int p = price != null ? price.intValue() : 0;
        return getString(R.string.price_template, p);
    }

    private String safe(@Nullable String s) {
        return s == null ? "" : s;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapView != null) mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
        checkIfFavorited();
    }

    @Override
    protected void onPause() {
        if (mapView != null) mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mapView != null) mapView.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
            mapView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }
}