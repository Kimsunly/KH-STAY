package com.khstay.myapplication.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.khstay.myapplication.R;
import com.khstay.myapplication.ui.rental.Rental;
import com.khstay.myapplication.ui.rental.RentalAdapter;
import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    // Views
    private SearchView searchView;
    private ImageButton btnFilter;
    private ImageButton btnSort;
    private ChipGroup chipGroupCategories;
    private RecyclerView recyclerHouses;
    private View emptyState;
    private ImageView ivEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;

    // Data
    private RentalAdapter rentalAdapter;
    private List<Rental> allRentals = new ArrayList<>();
    private List<Rental> filteredRentals = new ArrayList<>();
    private String currentSearchQuery = "";
    private String selectedCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupSearchView();
        setupCategoryChips();
        setupButtons();

        // Load initial data
        loadRentals();
    }

    private void initializeViews(View view) {
        searchView = view.findViewById(R.id.searchView);
        btnFilter = view.findViewById(R.id.btnFilter);
        btnSort = view.findViewById(R.id.btnSort);
        chipGroupCategories = view.findViewById(R.id.chipGroupCategories);
        recyclerHouses = view.findViewById(R.id.recyclerHouses);
        emptyState = view.findViewById(R.id.emptyState);
        ivEmpty = view.findViewById(R.id.ivEmpty);
        tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = view.findViewById(R.id.tvEmptySubtitle);
    }

    private void setupRecyclerView() {
        recyclerHouses.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create adapter for search mode (no status badge, show favorite button)
        rentalAdapter = new RentalAdapter(getContext(), filteredRentals, false, true);
        recyclerHouses.setAdapter(rentalAdapter);

        // Set click listener
        rentalAdapter.setOnRentalClickListener(new RentalAdapter.OnRentalClickListener() {
            @Override
            public void onRentalClick(Rental rental) {
                // TODO: Navigate to rental detail page
                // Example:
                // Bundle bundle = new Bundle();
                // bundle.putInt("rental_id", rental.getId());
                // NavHostFragment.findNavController(SearchFragment.this)
                //     .navigate(R.id.action_searchFragment_to_detailFragment, bundle);
            }

            @Override
            public void onMoreClick(Rental rental) {
                // Not used in search mode
            }

            @Override
            public void onFavoriteClick(Rental rental) {
                // TODO: Save favorite to database/preferences
                // Example:
                // if (rental.isFavorite()) {
                //     saveFavorite(rental.getId());
                // } else {
                //     removeFavorite(rental.getId());
                // }
            }
        });
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

        // Clear search when close button is clicked
        searchView.setOnCloseListener(() -> {
            currentSearchQuery = "";
            filterRentals();
            return false;
        });
    }

    private void setupCategoryChips() {
        chipGroupCategories.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                // No chip selected, default to "All"
                selectedCategory = "All";
            } else {
                Chip selectedChip = group.findViewById(checkedId);
                if (selectedChip != null) {
                    selectedCategory = selectedChip.getText().toString();
                }
            }
            filterRentals();
        });

        // Set default selection to "All"
        Chip chipAll = chipGroupCategories.findViewById(R.id.chipAll);
        if (chipAll != null) {
            chipAll.setChecked(true);
        }
    }

    private void setupButtons() {
        // Filter button click
        btnFilter.setOnClickListener(v -> showFilterDialog());

        // Sort button click
        btnSort.setOnClickListener(v -> showSortDialog());
    }

    private void showFilterDialog() {
        // TODO: Implement filter dialog
        // Create and show a dialog with filter options
        // Example: FilterDialogFragment.show(getParentFragmentManager(), selectedFilters -> {
        //     applyFilters(selectedFilters);
        // });
    }

    private void showSortDialog() {
        // TODO: Implement sort dialog
        // Create and show a dialog with sort options
        // Example: SortDialogFragment.show(getParentFragmentManager(), sortOption -> {
        //     sortRentals(sortOption);
        // });
    }

    private void loadRentals() {
        // TODO: Replace with your actual data loading
        // This could be from:
        // - Firebase Firestore
        // - Room Database
        // - REST API
        // - SharedViewModel

        // For now, using sample data
        allRentals.clear();
        allRentals.addAll(getSampleRentals());
        filterRentals();
    }

    private void filterRentals() {
        filteredRentals.clear();

        for (Rental rental : allRentals) {
            boolean matchesSearch = matchesSearchQuery(rental);
            boolean matchesCategory = matchesCategory(rental);

            if (matchesSearch && matchesCategory) {
                filteredRentals.add(rental);
            }
        }

        updateUI();
    }

    private boolean matchesSearchQuery(Rental rental) {
        if (currentSearchQuery == null || currentSearchQuery.trim().isEmpty()) {
            return true;
        }

        String query = currentSearchQuery.toLowerCase().trim();
        String title = rental.getTitle() != null ? rental.getTitle().toLowerCase() : "";
        String location = rental.getLocation() != null ? rental.getLocation().toLowerCase() : "";
        String description = rental.getDescription() != null ? rental.getDescription().toLowerCase() : "";

        return title.contains(query) ||
                location.contains(query) ||
                description.contains(query);
    }

    private boolean matchesCategory(Rental rental) {
        if (selectedCategory == null || selectedCategory.equals("All")) {
            return true;
        }

        String rentalCategory = rental.getCategory();
        return rentalCategory != null && rentalCategory.equals(selectedCategory);
    }

    private void updateUI() {
        if (filteredRentals.isEmpty()) {
            // Show empty state
            emptyState.setVisibility(View.VISIBLE);
            recyclerHouses.setVisibility(View.GONE);

            // Update empty state message based on context
            if (currentSearchQuery != null && !currentSearchQuery.trim().isEmpty()) {
                tvEmptyTitle.setText("No results found");
                tvEmptySubtitle.setText("We couldn't find any places matching \"" +
                        currentSearchQuery + "\"");
            } else {
                tvEmptyTitle.setText("No places available");
                tvEmptySubtitle.setText("There are no places in " + selectedCategory + " at the moment");
            }
        } else {
            // Show results
            emptyState.setVisibility(View.GONE);
            recyclerHouses.setVisibility(View.VISIBLE);
            rentalAdapter.notifyDataSetChanged();
        }
    }

    // Sample data - Replace with actual data loading
    private List<Rental> getSampleRentals() {
        List<Rental> rentals = new ArrayList<>();

        // Using your existing Rental constructor
        rentals.add(new Rental(
                1,
                "Modern 3-Bedroom House with Garden",
                "Sen Sok District, Phnom Penh",
                "600",
                "Active",
                R.drawable.house_image,
                "Sen Sok",
                "Beautiful modern house with spacious garden and parking",
                3,
                2
        ));

        rentals.add(new Rental(
                2,
                "Cozy Studio Apartment",
                "Toul Kok District, Phnom Penh",
                "250",
                "Active",
                R.drawable.house_image,
                "Toul Kok",
                "Perfect studio for single professionals",
                1,
                1
        ));

        rentals.add(new Rental(
                3,
                "Luxury 2-Bedroom Condo",
                "Psar Derm Tkev, Phnom Penh",
                "800",
                "Active",
                R.drawable.house_image,
                "Psar Derm Tkev",
                "High-rise condo with amazing city views",
                2,
                2
        ));

        rentals.add(new Rental(
                4,
                "Spacious Family Home",
                "Dangkao District, Phnom Penh",
                "950",
                "Active",
                R.drawable.house_image,
                "Dangkao",
                "Perfect for families with large living areas",
                4,
                3
        ));

        rentals.add(new Rental(
                5,
                "Affordable Room",
                "Chamkar Mon District, Phnom Penh",
                "180",
                "Active",
                R.drawable.house_image,
                "Chamkar Mon",
                "Budget-friendly room for students",
                1,
                1
        ));

        rentals.add(new Rental(
                6,
                "Luxury Villa with Pool",
                "Mean Chey District, Phnom Penh",
                "1500",
                "Active",
                R.drawable.house_image,
                "Mean Chey",
                "Exclusive villa with private pool and gym",
                5,
                4
        ));

        return rentals;
    }

    // Public method to set rentals from parent activity/shared viewmodel
    public void setRentalsData(List<Rental> rentals) {
        this.allRentals = rentals;
        filterRentals();
    }

    // Refresh data method
    public void refreshData() {
        loadRentals();
    }

    // Clear search method
    public void clearSearch() {
        searchView.setQuery("", false);
        searchView.clearFocus();
        currentSearchQuery = "";

        // Reset category to "All"
        Chip chipAll = chipGroupCategories.findViewById(R.id.chipAll);
        if (chipAll != null) {
            chipAll.setChecked(true);
        }

        filterRentals();
    }
}