
package com.khstay.myapplication.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.RentalRepository;
import com.khstay.myapplication.ui.rental.PostRentalActivity;
import com.khstay.myapplication.ui.rental.adapters.NearbyRentalAdapter;
import com.khstay.myapplication.ui.rental.adapters.PopularRentalAdapter;
import com.khstay.myapplication.ui.rental.model.Rental;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int LOCATION_PERMISSION_CODE = 1001;
    private static final int POPULAR_LIMIT = 5;
    private static final int NEARBY_LIMIT = 5;

    // Views
    private RecyclerView rvPopular, rvNearby;
    private FloatingActionButton fabAdd;
    private TextView tvGreeting, tvMorePopular, tvMoreNearby;
    private ImageView ivProfile;
    private CardView searchBarCard;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerPopular, shimmerNearby;

    // Data
    private final List<Rental> popularList = new ArrayList<>();
    private final List<Rental> nearbyList = new ArrayList<>();
    private PopularRentalAdapter popularAdapter;
    private NearbyRentalAdapter nearbyAdapter;

    // Firebase & Location
    private RentalRepository rentalRepository;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private Location userLocation;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        rentalRepository = new RentalRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize views
        initializeViews(v);
        setupRecyclerViews();
        setupClickListeners();

        // Load data
        loadUserProfile();
        checkLocationPermissionAndLoad();

        return v;
    }

    private void initializeViews(View v) {
        rvPopular = v.findViewById(R.id.rvPopular);
        rvNearby = v.findViewById(R.id.rvNearby);
        fabAdd = v.findViewById(R.id.fabAdd);
        tvGreeting = v.findViewById(R.id.tvGreeting);
        ivProfile = v.findViewById(R.id.ivProfile);
        tvMorePopular = v.findViewById(R.id.tvMorePopular);
        tvMoreNearby = v.findViewById(R.id.tvMoreNearby);
        searchBarCard = v.findViewById(R.id.searchBarCard);
        shimmerPopular = v.findViewById(R.id.shimmerPopular);
        shimmerNearby = v.findViewById(R.id.shimmerNearby);
    }

    private void setupRecyclerViews() {
        popularAdapter = new PopularRentalAdapter(popularList);
        rvPopular.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPopular.setHasFixedSize(true);
        rvPopular.setAdapter(popularAdapter);

        nearbyAdapter = new NearbyRentalAdapter(nearbyList);
        rvNearby.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNearby.setAdapter(nearbyAdapter);
    }

    private void setupClickListeners() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), PostRentalActivity.class);
            startActivity(intent);
        });

        if (searchBarCard != null) {
            searchBarCard.setOnClickListener(v -> navigateToSearchFragment());
        }
        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> navigateToProfileFragment());
        }
        if (tvMorePopular != null) {
            tvMorePopular.setOnClickListener(v -> loadAllPopular());
        }
        if (tvMoreNearby != null) {
            tvMoreNearby.setOnClickListener(v -> loadAllNearby());
        }
    }

    private void navigateToSearchFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mainFragmentContainer, new com.khstay.myapplication.ui.search.SearchFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void navigateToProfileFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mainFragmentContainer, new com.khstay.myapplication.ui.profile.ProfileFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void checkLocationPermissionAndLoad() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        } else {
            getUserLocationAndLoadData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocationAndLoadData();
            } else {
                Toast.makeText(getContext(), "Location permission denied. Showing all rentals.", Toast.LENGTH_SHORT).show();
                loadPopularRentals(POPULAR_LIMIT);
                loadNearbyRentals(NEARBY_LIMIT, null);
            }
        }
    }

    private void getUserLocationAndLoadData() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            loadPopularRentals(POPULAR_LIMIT);
            loadNearbyRentals(NEARBY_LIMIT, null);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    userLocation = location;
                    loadPopularRentals(POPULAR_LIMIT);
                    loadNearbyRentals(NEARBY_LIMIT, location);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    loadPopularRentals(POPULAR_LIMIT);
                    loadNearbyRentals(NEARBY_LIMIT, null);
                });
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null || !isAdded() || getView() == null) return;

        String uid = currentUser.getUid();
        String fallbackName = currentUser.getDisplayName();
        String fallbackPhoto = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null;

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!isAdded() || getView() == null) return;
                    String name = doc.exists() ? doc.getString("displayName") : fallbackName;
                    String photo = doc.exists() ? doc.getString("photoUrl") : fallbackPhoto;
                    updateProfileUI(name, photo);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded() || getView() == null) return;
                    updateProfileUI(fallbackName, fallbackPhoto);
                });
    }

    private void updateProfileUI(String displayName, String photoUrl) {
        if (!isAdded() || getView() == null) return;

        tvGreeting.setText(
                (displayName != null && !displayName.isEmpty())
                        ? "Hey, " + displayName + "!"
                        : "Hey there!"
        );

        if (photoUrl != null && !photoUrl.isEmpty() && ivProfile != null) {
            if (ivProfile.isAttachedToWindow()) {
                Glide.with(ivProfile)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .error(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(ivProfile);
            } else {
                ivProfile.post(() -> {
                    if (isAdded() && ivProfile.isAttachedToWindow()) {
                        Glide.with(ivProfile)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_avatar_placeholder)
                                .error(R.drawable.ic_avatar_placeholder)
                                .circleCrop()
                                .into(ivProfile);
                    }
                });
            }
        }
    }

    private void loadPopularRentals(int limit) {
        if (shimmerPopular != null) {
            shimmerPopular.startShimmer();
            shimmerPopular.setVisibility(View.VISIBLE);
        }
        if (rvPopular != null) rvPopular.setVisibility(View.GONE);

        Query query = rentalRepository.fetchPopularRentals();
        if (limit > 0) query = query.limit(limit);

        query.get()
                .addOnSuccessListener(qs -> {
                    popularList.clear();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Rental r = d.toObject(Rental.class);
                        if (r != null) { r.setId(d.getId()); popularList.add(r); }
                    }
                    popularAdapter.notifyDataSetChanged();

                    if (shimmerPopular != null) { shimmerPopular.stopShimmer(); shimmerPopular.setVisibility(View.GONE); }
                    if (rvPopular != null) rvPopular.setVisibility(View.VISIBLE);

                    Log.d(TAG, "Loaded " + popularList.size() + " popular rentals");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load popular rentals", e);
                    popularList.clear();
                    popularAdapter.notifyDataSetChanged();
                    if (shimmerPopular != null) { shimmerPopular.stopShimmer(); shimmerPopular.setVisibility(View.GONE); }
                    if (rvPopular != null) rvPopular.setVisibility(View.VISIBLE);
                });
    }

    private void loadNearbyRentals(int limit, Location userLoc) {
        if (shimmerNearby != null) {
            shimmerNearby.startShimmer();
            shimmerNearby.setVisibility(View.VISIBLE);
        }
        if (rvNearby != null) rvNearby.setVisibility(View.GONE);

        Query query = rentalRepository.fetchAllActiveRentals();
        if (limit > 0) query = query.limit(limit);

        query.get()
                .addOnSuccessListener(qs -> {
                    nearbyList.clear();
                    List<Rental> tempList = new ArrayList<>();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Rental r = d.toObject(Rental.class);
                        if (r != null) { r.setId(d.getId()); tempList.add(r); }
                    }
                    if (userLoc != null) {
                        tempList.sort((r1, r2) -> {
                            float dist1 = calculateDistance(userLoc, r1);
                            float dist2 = calculateDistance(userLoc, r2);
                            return Float.compare(dist1, dist2);
                        });
                    }
                    nearbyList.addAll(tempList);
                    nearbyAdapter.notifyDataSetChanged();

                    if (shimmerNearby != null) { shimmerNearby.stopShimmer(); shimmerNearby.setVisibility(View.GONE); }
                    if (rvNearby != null) rvNearby.setVisibility(View.VISIBLE);

                    Log.d(TAG, "Loaded " + nearbyList.size() + " nearby rentals");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load nearby rentals", e);
                    nearbyList.clear();
                    nearbyAdapter.notifyDataSetChanged();
                    if (shimmerNearby != null) { shimmerNearby.stopShimmer(); shimmerNearby.setVisibility(View.GONE); }
                    if (rvNearby != null) rvNearby.setVisibility(View.VISIBLE);
                });
    }

    private float calculateDistance(Location userLoc, Rental rental) {
        if (rental.getLatitude() == null || rental.getLongitude() == null) return Float.MAX_VALUE;
        float[] results = new float[1];
        Location.distanceBetween(
                userLoc.getLatitude(), userLoc.getLongitude(),
                rental.getLatitude(), rental.getLongitude(),
                results
        );
        return results[0];
    }

    private void loadAllPopular() {
        loadPopularRentals(0);
        Toast.makeText(getContext(), "Loading all popular rentals...", Toast.LENGTH_SHORT).show();
    }

    private void loadAllNearby() {
        loadNearbyRentals(0, userLocation);
        Toast.makeText(getContext(), "Loading all nearby rentals...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Refreshing data");
        loadUserProfile();
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getUserLocationAndLoadData();
        } else {
            loadPopularRentals(POPULAR_LIMIT);
            loadNearbyRentals(NEARBY_LIMIT, null);
        }
    }
}

