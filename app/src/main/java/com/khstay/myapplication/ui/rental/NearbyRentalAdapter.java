package com.khstay.myapplication.ui.rental;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.khstay.myapplication.R;

import java.util.List;

public class NearbyRentalAdapter extends RecyclerView.Adapter<NearbyRentalAdapter.ViewHolder> {

    private final List<Rental> rentals;

    public NearbyRentalAdapter(List<Rental> rentals) { this.rentals = rentals; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_nearby_rental, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Rental r = rentals.get(position);
        h.tvTitle.setText(r.getTitle());
        h.tvLocation.setText(r.getLocation());
        h.tvPrice.setText("$" + (r.getPrice() != null ? r.getPrice().intValue() : 0));

        String url = r.getImageUrl();
        if (url != null && !url.isEmpty()) {
            Glide.with(h.imgHouse.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .centerCrop()
                    .into(h.imgHouse);
        } else {
            h.imgHouse.setImageResource(R.drawable.ic_placeholder);
        }

        // 🔗 Item click -> open detail
        h.itemView.setOnClickListener(v -> {
            if (r.getId() == null || r.getId().isEmpty()) return;
            Context ctx = v.getContext();
            Intent i = new Intent(ctx, RentalHouseDetailActivity.class);
            i.putExtra(RentalHouseDetailActivity.EXTRA_RENTAL_ID, r.getId());
            ctx.startActivity(i);
        });
    }

    @Override
    public int getItemCount() { return rentals.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLocation, tvPrice;
        ImageView imgHouse;
        ViewHolder(View v) {
            super(v);
            imgHouse  = v.findViewById(R.id.imgHouse);
            tvTitle   = v.findViewById(R.id.tvTitle);
            tvLocation= v.findViewById(R.id.tvLocation);
            tvPrice   = v.findViewById(R.id.tvPrice);
        }
    }
}
