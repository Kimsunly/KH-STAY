
package com.khstay.myapplication.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.RentalRepository;
import com.khstay.myapplication.ui.rental.NearbyRentalAdapter;
import com.khstay.myapplication.ui.rental.PopularRentalAdapter;
import com.khstay.myapplication.ui.rental.Rental;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rvPopular, rvNearby;
    private final List<Rental> popularList = new ArrayList<>();
    private final List<Rental> nearbyList  = new ArrayList<>();

    private PopularRentalAdapter popularAdapter;
    private NearbyRentalAdapter nearbyAdapter;

    private RentalRepository rentalRepository;

    public HomeFragment() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        rvPopular = v.findViewById(R.id.rvPopular);
        rvNearby  = v.findViewById(R.id.rvNearby);

        rentalRepository = new RentalRepository();

        setupRecyclerViews();
        loadPopularRentals();
        loadNearbyRentals();

        return v;
    }

    private void setupRecyclerViews() {
        popularAdapter = new PopularRentalAdapter(popularList);
        nearbyAdapter  = new NearbyRentalAdapter(nearbyList);

        rvPopular.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPopular.setHasFixedSize(true);
        rvPopular.setAdapter(popularAdapter);

        rvNearby.setLayoutManager(new LinearLayoutManager(getContext()));
        rvNearby.setAdapter(nearbyAdapter);
    }


    private void loadPopularRentals() {
        rentalRepository.fetchPopularRentals()
                .get()
                .addOnSuccessListener(qs -> {
                    popularList.clear();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Rental r = d.toObject(Rental.class);
                        if (r != null) {
                            r.setId(d.getId());         // ✅ required
                            popularList.add(r);
                        }
                    }
                    popularAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    popularList.clear();
                    popularAdapter.notifyDataSetChanged();
                });
    }

    private void loadNearbyRentals() {
        rentalRepository.fetchAllActiveRentals()
                .get()
                .addOnSuccessListener(qs -> {
                    nearbyList.clear();
                    for (DocumentSnapshot d : qs.getDocuments()) {
                        Rental r = d.toObject(Rental.class);
                        if (r != null) {
                            r.setId(d.getId());         // ✅ required
                            nearbyList.add(r);
                        }
                    }
                    nearbyAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    nearbyList.clear();
                    nearbyAdapter.notifyDataSetChanged();
                });
    }

}
