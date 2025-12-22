
package com.khstay.myapplication.ui.rental;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.khstay.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class MyRentFragment extends Fragment {

    private static final String TAG = "MyRentFragment";

    private RecyclerView rvRentals;
    private RentalAdapter rentalAdapter;
    private Chip chipActive, chipPending, chipArchived;

    private final List<Rental> visibleRentals = new ArrayList<>();
    private String currentTab = "active";

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public MyRentFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_rent, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvRentals   = view.findViewById(R.id.rv_rentals);
        chipActive  = view.findViewById(R.id.chip_active);
        chipPending = view.findViewById(R.id.chip_pending);
        chipArchived= view.findViewById(R.id.chip_archived);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupRecyclerView();
        setupChipListeners();

        updateChipStates(chipActive);
        fetchRentalsForTab(currentTab);
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
                updateChipStates(chipActive);
                fetchRentalsForTab(currentTab);
            }
        });
        chipPending.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTab = "pending";
                updateChipStates(chipPending);
                fetchRentalsForTab(currentTab);
            }
        });
        chipArchived.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                currentTab = "archived";
                updateChipStates(chipArchived);
                fetchRentalsForTab(currentTab);
            }
        });
    }

    private void updateChipStates(Chip selectedChip) {
        chipActive.setChecked(false);
        chipPending.setChecked(false);
        chipArchived.setChecked(false);
        selectedChip.setChecked(true);
    }

    private void fetchRentalsForTab(String statusKey) {
        String uid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        Query query = db.collection("rental_houses")
                .whereEqualTo("status", statusKey)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        // OPTIONAL: show only "my" rentals
        // if (uid != null) query = query.whereEqualTo("ownerId", uid);

        query.get()
                .addOnSuccessListener(qs -> {
                    visibleRentals.clear();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Rental r = d.toObject(Rental.class);
                        if (r != null) { r.setId(d.getId()); visibleRentals.add(r); }
                    }
                    rentalAdapter.updateList(new ArrayList<>(visibleRentals));
                    Log.d(TAG, "Loaded " + visibleRentals.size() + " rentals for status=" + statusKey);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Fetch failed: " + e.getMessage(), e);
                    visibleRentals.clear();
                    rentalAdapter.updateList(new ArrayList<>(visibleRentals));
                });
    }

    public void setRentalsData(List<Rental> rentals) {
        visibleRentals.clear();
        visibleRentals.addAll(rentals);
        rentalAdapter.updateList(new ArrayList<>(visibleRentals));
    }
}

