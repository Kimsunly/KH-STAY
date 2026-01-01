package com.khstay.myapplication.ui.rental.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.khstay.myapplication.R;
import com.khstay.myapplication.ui.rental.RentalHouseDetailActivity;
import com.khstay.myapplication.ui.rental.model.Rental;

import java.util.List;

public class PopularRentalAdapter extends RecyclerView.Adapter<PopularRentalAdapter.ViewHolder> {

    private final List<Rental> rentals;

    public PopularRentalAdapter(List<Rental> rentals) {
        this.rentals = rentals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_popular_rental, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Rental r = rentals.get(position);

        // Basic info
        h.tvTitle.setText(r.getTitle());
        h.tvLocation.setText(r.getLocation());
        h.tvPrice.setText("$" + (r.getPrice() != null ? r.getPrice().intValue() : 0));

        // Image
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

        // NEW: Popularity Stats
        int views = r.getViewCount();
        int favorites = r.getFavoriteCount();
        double score = r.getPopularityScore();

        // Show popularity badge if score > 0
        if (score > 0) {
            h.popularityBadge.setVisibility(View.VISIBLE);

            // Format score nicely
            String scoreText;
            if (score >= 1000) {
                scoreText = String.format("%.1fk", score / 1000);
            } else {
                scoreText = String.valueOf((int) score);
            }
            h.tvPopularityScore.setText(scoreText);
        } else {
            h.popularityBadge.setVisibility(View.GONE);
        }

        // View count
        h.tvViews.setText(formatCount(views));

        // Favorite count
        h.tvFavorites.setText(formatCount(favorites));

        // Item click -> open detail
        h.itemView.setOnClickListener(v -> {
            if (r.getId() == null || r.getId().isEmpty()) return;
            Context ctx = v.getContext();
            Intent i = new Intent(ctx, RentalHouseDetailActivity.class);
            i.putExtra(RentalHouseDetailActivity.EXTRA_RENTAL_ID, r.getId());
            ctx.startActivity(i);
        });
    }

    /**
     * Format large numbers (e.g., 1500 -> 1.5k)
     */
    private String formatCount(int count) {
        if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }

    @Override
    public int getItemCount() {
        return rentals.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvLocation, tvPrice;
        TextView tvViews, tvFavorites, tvPopularityScore;
        ImageView imgHouse;
        LinearLayout popularityBadge;

        ViewHolder(View v) {
            super(v);
            imgHouse = v.findViewById(R.id.imgHouse);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvLocation = v.findViewById(R.id.tvLocation);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvViews = v.findViewById(R.id.tvViews);
            tvFavorites = v.findViewById(R.id.tvFavorites);
            tvPopularityScore = v.findViewById(R.id.tvPopularityScore);
            popularityBadge = v.findViewById(R.id.popularityBadge);
        }
    }
}