package com.khstay.myapplication.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.UserRepository;
import com.khstay.myapplication.ui.rental.RentalHouseDetailActivity;
import com.khstay.myapplication.ui.rental.model.Rental;
import com.khstay.myapplication.ui.rental.adapters.RentalAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchFragment extends Fragment {

    private static final String TAG = "SearchFragment";

    // Views
    private SearchView searchView;
    private ImageButton btnFilter, btnSort;
    private ChipGroup chipGroupCategories;
    private RecyclerView recyclerHouses;
    private View emptyState;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerLoading;

    // Data
    private RentalAdapter rentalAdapter;
    private List<Rental> allRentals = new ArrayList<>();
    private List<Rental> filteredRentals = new ArrayList<>();

    // Filters
    private String currentSearchQuery = "";
    private String selectedCommune = "All";
    private String sortOption = "newest"; // newest, price_low, price_high
    private Double minPrice = null;
    private Double maxPrice = null;
    private Integer minBedrooms = null;

    // Available communes extracted from data
    private Set<String> availableCommunes = new HashSet<>();

    // Firebase & Repository
    private FirebaseFirestore db;
    private UserRepository userRepository;

    // Track favorite states
    private Map<String, Boolean> favoriteStates = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();

        initializeViews(view);
        setupRecyclerView();
        setupSearchView();
        setupButtons();

        loadRentals();
    }

    private void initializeViews(View view) {
        searchView = view.findViewById(R.id.searchView);
        btnFilter = view.findViewById(R.id.btnFilter);
        btnSort = view.findViewById(R.id.btnSort);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        recyclerHouses = view.findViewById(R.id.recyclerHouses);
        emptyState = view.findViewById(R.id.emptyState);

        // Initialize shimmer - will be null if not in layout yet
        try {
            shimmerLoading = view.findViewById(R.id.shimmerLoading);
        } catch (Exception e) {
            shimmerLoading = null;
        }
    }

    private void setupRecyclerView() {
        recyclerHouses.setLayoutManager(new LinearLayoutManager(getContext()));
        rentalAdapter = new RentalAdapter(getContext(), filteredRentals, false, true);
        recyclerHouses.setAdapter(rentalAdapter);

        rentalAdapter.setOnRentalClickListener(new RentalAdapter.OnRentalClickListener() {
            @Override
            public void onRentalClick(Rental rental) {
                if (rental.getId() != null && !rental.getId().isEmpty()) {
                    Intent intent = new Intent(getActivity(), RentalHouseDetailActivity.class);
                    intent.putExtra(RentalHouseDetailActivity.EXTRA_RENTAL_ID, rental.getId());
                    startActivity(intent);
                }
            }

            @Override
            public void onMoreClick(Rental rental) {
                // Not used in search
            }

            @Override
            public void onFavoriteClick(Rental rental) {
                toggleFavorite(rental);
            }
        });
    }

    private void toggleFavorite(Rental rental) {
        if (rental == null || rental.getId() == null) return;

        String rentalId = rental.getId();
        boolean currentlyFavorited = favoriteStates.getOrDefault(rentalId, false);

        if (currentlyFavorited) {
            // Remove from favorites
            userRepository.removeFavorite(rentalId)
                    .addOnSuccessListener(aVoid -> {
                        favoriteStates.put(rentalId, false);
                        rental.setFavorite(false);
                        rentalAdapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to remove favorite", e);
                        Toast.makeText(getContext(), "Failed to update favorites", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Add to favorites
            String propertyPrice = "$" + (rental.getPrice() != null ? rental.getPrice().intValue() : 0) + "/month";
            userRepository.addFavorite(
                            rentalId,
                            rental.getTitle(),
                            rental.getImageUrl(),
                            propertyPrice,
                            rental.getLocation()
                    )
                    .addOnSuccessListener(aVoid -> {
                        favoriteStates.put(rentalId, true);
                        rental.setFavorite(true);
                        rentalAdapter.notifyDataSetChanged();
                        Toast.makeText(getContext(), "Added to favorites", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to add favorite", e);
                        Toast.makeText(getContext(), "Failed to update favorites", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void loadFavoriteStates() {
        // Load favorite states for all rentals
        if (userRepository.getFavorites() != null) {
            userRepository.getFavorites()
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        favoriteStates.clear();
                        querySnapshot.forEach(doc -> {
                            String propertyId = doc.getString("propertyId");
                            if (propertyId != null) {
                                favoriteStates.put(propertyId, true);
                            }
                        });

                        // Update rental objects with favorite state
                        for (Rental rental : allRentals) {
                            if (rental.getId() != null) {
                                boolean isFav = favoriteStates.getOrDefault(rental.getId(), false);
                                rental.setFavorite(isFav);
                            }
                        }

                        // Refresh UI
                        filterRentals();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to load favorite states", e);
                    });
        }
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentSearchQuery = query;
                filterRentals();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                currentSearchQuery = newText;
                filterRentals();
                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            currentSearchQuery = "";
            filterRentals();
            return false;
        });
    }

    private void setupCommuneChips() {
        chipGroupCategories.removeAllViews();

        // Add "All" chip
        Chip chipAll = createChip("All", true);
        chipGroupCategories.addView(chipAll);

        // Add chips for each available commune
        List<String> sortedCommunes = new ArrayList<>(availableCommunes);
        java.util.Collections.sort(sortedCommunes);

        for (String commune : sortedCommunes) {
            Chip chip = createChip(commune, false);
            chipGroupCategories.addView(chip);
        }

        // Add "Add New" chip at the end
        Chip chipAddNew = createAddNewChip();
        chipGroupCategories.addView(chipAddNew);

        // Set up chip selection listener
        chipGroupCategories.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                selectedCommune = "All";
                chipAll.setChecked(true);
            } else {
                Chip selectedChip = group.findViewById(checkedId);
                if (selectedChip != null && selectedChip.getTag() != null && selectedChip.getTag().equals("addnew")) {
                    // Don't change selection, show dialog instead
                    chipGroupCategories.clearCheck();
                    showAddCommuneDialog();
                } else if (selectedChip != null) {
                    selectedCommune = selectedChip.getText().toString();
                    filterRentals();
                }
            }
        });

        chipAll.setChecked(true);
    }

    private Chip createChip(String text, boolean checked) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setChipBackgroundColorResource(checked ? R.color.primary : R.color.white);
        chip.setTextColor(getResources().getColor(checked ? R.color.white : R.color.black));
        return chip;
    }

    private Chip createAddNewChip() {
        Chip chip = new Chip(requireContext());
        chip.setText("+ Add Commune");
        chip.setCheckable(true);
        chip.setChecked(false);
        chip.setChipBackgroundColorResource(R.color.primary_light);
        chip.setTextColor(getResources().getColor(R.color.primary));
        chip.setTag("addnew");
        return chip;
    }

    private void showAddCommuneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Add New Commune");

        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Enter commune name");
        input.setPadding(50, 30, 50, 30);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newCommune = input.getText().toString().trim();
            if (!newCommune.isEmpty()) {
                if (!availableCommunes.contains(newCommune)) {
                    availableCommunes.add(newCommune);
                    setupCommuneChips();
                    Toast.makeText(getContext(), "Commune added: " + newCommune, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Commune already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void setupButtons() {
        btnFilter.setOnClickListener(v -> showFilterDialog());
        btnSort.setOnClickListener(v -> showSortDialog());
    }

    private void loadRentals() {
        Log.d(TAG, "Loading all active rentals from Firestore");

        // Show skeleton
        if (shimmerLoading != null) {
            shimmerLoading.startShimmer();
            shimmerLoading.setVisibility(View.VISIBLE);
        }
        recyclerHouses.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);

        db.collection("rental_houses")
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    allRentals.clear();
                    availableCommunes.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Rental rental = doc.toObject(Rental.class);
                        if (rental != null) {
                            rental.setId(doc.getId());
                            allRentals.add(rental);

                            // Extract commune from location
                            String location = rental.getLocation();
                            if (location != null && !location.isEmpty()) {
                                // Try to extract commune name from location
                                // Assuming format like "Commune Name, District, City"
                                String[] parts = location.split(",");
                                if (parts.length > 0) {
                                    String commune = parts[0].trim();
                                    if (!commune.isEmpty()) {
                                        availableCommunes.add(commune);
                                    }
                                }
                            }
                        }
                    }
                    Log.d(TAG, "Loaded " + allRentals.size() + " active rentals");
                    Log.d(TAG, "Found " + availableCommunes.size() + " unique communes");

                    // Setup commune chips after loading data
                    setupCommuneChips();

                    // Load favorite states
                    loadFavoriteStates();

                    // Hide skeleton
                    if (shimmerLoading != null) {
                        shimmerLoading.stopShimmer();
                        shimmerLoading.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load rentals", e);

                    // Hide skeleton on error
                    if (shimmerLoading != null) {
                        shimmerLoading.stopShimmer();
                        shimmerLoading.setVisibility(View.GONE);
                    }

                    updateUI();
                });
    }

    private void filterRentals() {
        filteredRentals.clear();

        for (Rental rental : allRentals) {
            if (matchesAllFilters(rental)) {
                filteredRentals.add(rental);
            }
        }

        sortRentals();
        updateUI();

        Log.d(TAG, "Filtered to " + filteredRentals.size() + " rentals");
    }

    private boolean matchesAllFilters(Rental rental) {
        // Search query
        if (!matchesSearchQuery(rental)) return false;

        // Commune filter
        if (!matchesCommune(rental)) return false;

        // Price range
        if (minPrice != null && (rental.getPrice() == null || rental.getPrice() < minPrice)) {
            return false;
        }
        if (maxPrice != null && (rental.getPrice() == null || rental.getPrice() > maxPrice)) {
            return false;
        }

        // Bedrooms
        if (minBedrooms != null && (rental.getBedrooms() == null || rental.getBedrooms() < minBedrooms)) {
            return false;
        }

        return true;
    }

    private boolean matchesSearchQuery(Rental rental) {
        if (currentSearchQuery == null || currentSearchQuery.trim().isEmpty()) {
            return true;
        }

        String query = currentSearchQuery.toLowerCase().trim();
        String title = rental.getTitle() != null ? rental.getTitle().toLowerCase() : "";
        String location = rental.getLocation() != null ? rental.getLocation().toLowerCase() : "";
        String description = rental.getDescription() != null ? rental.getDescription().toLowerCase() : "";

        return title.contains(query) || location.contains(query) || description.contains(query);
    }

    private boolean matchesCommune(Rental rental) {
        if (selectedCommune == null || selectedCommune.equals("All")) {
            return true;
        }

        String location = rental.getLocation();
        if (location == null || location.isEmpty()) {
            return false;
        }

        // Check if location contains the selected commune
        // Make it case-insensitive
        return location.toLowerCase().contains(selectedCommune.toLowerCase());
    }

    private void sortRentals() {
        switch (sortOption) {
            case "price_low":
                filteredRentals.sort((r1, r2) -> {
                    Double p1 = r1.getPrice() != null ? r1.getPrice() : 0.0;
                    Double p2 = r2.getPrice() != null ? r2.getPrice() : 0.0;
                    return Double.compare(p1, p2);
                });
                break;

            case "price_high":
                filteredRentals.sort((r1, r2) -> {
                    Double p1 = r1.getPrice() != null ? r1.getPrice() : 0.0;
                    Double p2 = r2.getPrice() != null ? r2.getPrice() : 0.0;
                    return Double.compare(p2, p1);
                });
                break;

            case "newest":
            default:
                filteredRentals.sort((r1, r2) -> {
                    if (r1.getCreatedAt() == null) return 1;
                    if (r2.getCreatedAt() == null) return -1;
                    return r2.getCreatedAt().compareTo(r1.getCreatedAt());
                });
                break;
        }
    }

    private void updateUI() {
        if (filteredRentals.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerHouses.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerHouses.setVisibility(View.VISIBLE);
            rentalAdapter.notifyDataSetChanged();
        }
    }

    private void showSortDialog() {
        String[] options = {"Newest First", "Price: Low to High", "Price: High to Low"};
        int selectedIndex = sortOption.equals("price_low") ? 1 :
                sortOption.equals("price_high") ? 2 : 0;

        new AlertDialog.Builder(requireContext())
                .setTitle("Sort By")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    switch (which) {
                        case 0: sortOption = "newest"; break;
                        case 1: sortOption = "price_low"; break;
                        case 2: sortOption = "price_high"; break;
                    }
                    filterRentals();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFilterDialog() {
        View filterView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_filter, null);

        // Get views from dialog
        com.google.android.material.textfield.TextInputEditText etMinPrice =
                filterView.findViewById(R.id.etMinPrice);
        com.google.android.material.textfield.TextInputEditText etMaxPrice =
                filterView.findViewById(R.id.etMaxPrice);
        com.google.android.material.textfield.TextInputEditText etMinBedrooms =
                filterView.findViewById(R.id.etMinBedrooms);

        // Set current values
        if (minPrice != null) etMinPrice.setText(String.valueOf(minPrice.intValue()));
        if (maxPrice != null) etMaxPrice.setText(String.valueOf(maxPrice.intValue()));
        if (minBedrooms != null) etMinBedrooms.setText(String.valueOf(minBedrooms));

        new AlertDialog.Builder(requireContext())
                .setTitle("Filter")
                .setView(filterView)
                .setPositiveButton("Apply", (dialog, which) -> {
                    try {
                        String minPriceStr = etMinPrice.getText().toString();
                        String maxPriceStr = etMaxPrice.getText().toString();
                        String minBedroomsStr = etMinBedrooms.getText().toString();

                        minPrice = minPriceStr.isEmpty() ? null : Double.parseDouble(minPriceStr);
                        maxPrice = maxPriceStr.isEmpty() ? null : Double.parseDouble(maxPriceStr);
                        minBedrooms = minBedroomsStr.isEmpty() ? null : Integer.parseInt(minBedroomsStr);

                        filterRentals();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear", (dialog, which) -> {
                    minPrice = null;
                    maxPrice = null;
                    minBedrooms = null;
                    filterRentals();
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - Refreshing rentals and favorites");
        // Reload all rentals and favorite states when fragment becomes visible
        loadRentals();
    }
}