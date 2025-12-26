
package com.khstay.myapplication.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.khstay.myapplication.R;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecentViewedAdapter extends RecyclerView.Adapter<RecentViewedAdapter.ViewHolder> {

    private final List<RecentViewedItem> recentItems;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RecentViewedItem item);
    }

    public RecentViewedAdapter(List<RecentViewedItem> recentItems, OnItemClickListener listener) {
        this.recentItems = recentItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_viewed, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentViewedItem item = recentItems.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return recentItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImage;
        TextView propertyName, propertyPrice, propertyLocation, viewedAt;

        ViewHolder(View itemView) {
            super(itemView);
            propertyImage = itemView.findViewById(R.id.propertyImage);
            propertyName = itemView.findViewById(R.id.propertyName);
            propertyPrice = itemView.findViewById(R.id.propertyPrice);
            propertyLocation = itemView.findViewById(R.id.propertyLocation);
            viewedAt = itemView.findViewById(R.id.viewedAt);
        }

        void bind(RecentViewedItem item, OnItemClickListener listener) {
            propertyName.setText(item.getPropertyName());
            propertyPrice.setText(item.getPropertyPrice());
            propertyLocation.setText(item.getPropertyLocation());

            if (item.getViewedAt() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                viewedAt.setText("Viewed: " + sdf.format(item.getViewedAt().toDate()));
            } else {
                viewedAt.setText("");
            }

            Glide.with(itemView.getContext())
                    .load(item.getPropertyImage())
                    .placeholder(R.drawable.ic_house_placeholder) // add this vector
                    .error(R.drawable.ic_house_placeholder)
                    .centerCrop()
                    .into(propertyImage);

            // Support old data shape: if rentalId is null, try legacy propertyId via document ID
            String rentalId = item.getRentalId();
            if ((rentalId == null || rentalId.trim().isEmpty()) && item.getId() != null) {
                // Old entries stored the doc id as propertyId
                rentalId = item.getId();
            }
            final String finalRentalId = rentalId;

            itemView.setOnClickListener(v -> {
                if (finalRentalId == null || finalRentalId.trim().isEmpty()) {
                    // Avoid crash; optionally show a toast
                    return;
                }
                listener.onItemClick(item);
            });
        }
    }
}
