package com.khstay.myapplication.ui.rental;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.khstay.myapplication.R;
import com.google.android.material.chip.Chip;
import java.util.ArrayList;
import java.util.List;

public class MyRentFragment extends Fragment {

    private RecyclerView rvRentals;
    private RentalAdapter rentalAdapter;
    private Chip chipActive, chipPending, chipArchived;
    private List<Rental> allRentalsList;
    private String currentTab = "active";

    public MyRentFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_rent, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        rvRentals = view.findViewById(R.id.rv_rentals);
        chipActive = view.findViewById(R.id.chip_active);
        chipPending = view.findViewById(R.id.chip_pending);
        chipArchived = view.findViewById(R.id.chip_archived);

        // Initialize data list
        allRentalsList = new ArrayList<>();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup chip listeners
        setupChipListeners();

        // Load rentals data
        loadRentals();
    }

    private void setupRecyclerView() {
        rvRentals.setLayoutManager(new LinearLayoutManager(getContext()));
        rentalAdapter = new RentalAdapter(getContext(), new ArrayList<>());
        rvRentals.setAdapter(rentalAdapter);
    }

    private void setupChipListeners() {
        chipActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTab = "active";
                filterRentals("Active");
                updateChipStates(chipActive);
            }
        });

        chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTab = "pending";
                filterRentals("Pending");
                updateChipStates(chipPending);
            }
        });

        chipArchived.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTab = "archived";
                filterRentals("Archived");
                updateChipStates(chipArchived);
            }
        });
    }

    private void updateChipStates(Chip selectedChip) {
        chipActive.setChecked(false);
        chipPending.setChecked(false);
        chipArchived.setChecked(false);
        selectedChip.setChecked(true);
    }

    private void loadRentals() {
        // Add sample data
        allRentalsList.add(new Rental(
                1,
                "Modern 3-Bedroom House with Garden",
                "Sen Sok District, Phnom Penh, Cambodia",
                "600",
                "Active",
                R.drawable.house_image
        ));

        allRentalsList.add(new Rental(
                2,
                "Modern 3-Bedroom House with Garden",
                "Sen Sok District, Phnom Penh, Cambodia",
                "600",
                "Active",
                R.drawable.house_image
        ));

        allRentalsList.add(new Rental(
                3,
                "Modern 3-Bedroom House with Garden",
                "Sen Sok District, Phnom Penh, Cambodia",
                "600",
                "Active",
                R.drawable.house_image
        ));

        allRentalsList.add(new Rental(
                4,
                "Cozy 2-Bedroom Apartment",
                "Tuol Kouk District, Phnom Penh, Cambodia",
                "450",
                "Pending",
                R.drawable.house_image
        ));

        allRentalsList.add(new Rental(
                5,
                "Luxury Villa with Pool",
                "Boeng Keng Kong I District, Phnom Penh, Cambodia",
                "1200",
                "Archived",
                R.drawable.house_image
        ));

        // Initial filter
        filterRentals("Active");
    }

    private void filterRentals(String status) {
        List<Rental> filteredList = new ArrayList<>();

        for (Rental rental : allRentalsList) {
            if (rental.getStatus().equalsIgnoreCase(status)) {
                filteredList.add(rental);
            }
        }

        rentalAdapter.updateList(filteredList);
    }

    // Method to set rental data from outside (if needed)
    public void setRentalsData(List<Rental> rentals) {
        this.allRentalsList = rentals;
        filterRentals("Active");
    }
}