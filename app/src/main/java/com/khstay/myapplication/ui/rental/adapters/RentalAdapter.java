package com.khstay.myapplication.ui.rental.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.khstay.myapplication.R;
import com.khstay.myapplication.ui.rental.model.Rental;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RentalAdapter extends RecyclerView.Adapter<RentalAdapter.RentalViewHolder> {

    private final Context context;
    private List<Rental> rentalList;
    private OnRentalClickListener onRentalClickListener;
    private boolean showStatusBadge = true;     // MyRentFragment
    private boolean showFavoriteButton = false; // SearchFragment
    private String imageUrl;
    public interface OnRentalClickListener {
        void onRentalClick(Rental rental);
        void onMoreClick(Rental rental);
        void onFavoriteClick(Rental rental);
    }

    public RentalAdapter(Context context, List<Rental> rentalList) {
        this.context = context;
        this.rentalList = rentalList;
    }

    public RentalAdapter(Context context, List<Rental> rentalList,
                         boolean showStatusBadge, boolean showFavoriteButton) {
        this.context = context;
        this.rentalList = rentalList;
        this.showStatusBadge = showStatusBadge;
        this.showFavoriteButton = showFavoriteButton;
    }

    public void setOnRentalClickListener(OnRentalClickListener listener) {
        this.onRentalClickListener = listener;
    }

    public void setShowStatusBadge(boolean show) {
        this.showStatusBadge = show;
        notifyDataSetChanged();
    }

    public void setShowFavoriteButton(boolean show) {
        this.showFavoriteButton = show;
        notifyDataSetChanged();
    }

    @Override
    public RentalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_rental, parent, false);
        return new RentalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RentalViewHolder holder, int position) {
        Rental rental = rentalList.get(position);

        // Image
        if (rental.hasImageUrl()) {
            loadImageFromUrl(rental.getImageUrl(), holder.ivProperty);
        } else {
            holder.ivProperty.setImageResource(
                    rental.getImageResId() != 0 ? rental.getImageResId() : R.drawable.ic_placeholder
            );
        }

        // Texts
        holder.tvTitle.setText(rental.getTitle());
        holder.tvLocation.setText(rental.getLocation());
        holder.tvPrice.setText("$" + (rental.getPrice() != null ? rental.getPrice().intValue() : 0) + "/month");

        // Bed/Bath
        if (holder.tvBedBath != null) {
            String bb = rental.getBedroomBathroomText();
            holder.tvBedBath.setVisibility(bb.isEmpty() ? View.GONE : View.VISIBLE);
            holder.tvBedBath.setText(bb);
        }

        // Status badge
        if (holder.tvStatus != null) {
            if (showStatusBadge && rental.getStatus() != null && !rental.getStatus().isEmpty()) {
                holder.tvStatus.setVisibility(View.VISIBLE);
                holder.tvStatus.setText(rental.getStatus());
                if (rental.getStatus().equalsIgnoreCase("Active")) {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active);
                    holder.tvStatus.setTextColor(context.getResources().getColor(R.color.success_green));
                } else if (rental.getStatus().equalsIgnoreCase("Pending")) {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                    holder.tvStatus.setTextColor(context.getResources().getColor(R.color.text_secondary));
                } else if (rental.getStatus().equalsIgnoreCase("Archived")) {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_archived);
                    holder.tvStatus.setTextColor(context.getResources().getColor(R.color.text_hint));
                }
            } else {
                holder.tvStatus.setVisibility(View.GONE);
            }
        }

        // Favorite button (Search)
        if (holder.ivFavorite != null) {
            if (showFavoriteButton) {
                holder.ivFavorite.setVisibility(View.VISIBLE);
                updateFavoriteIcon(holder, rental);
                holder.ivFavorite.setOnClickListener(v -> {
                    rental.setFavorite(!rental.isFavorite());
                    updateFavoriteIcon(holder, rental);
                    if (onRentalClickListener != null) onRentalClickListener.onFavoriteClick(rental);
                });
            } else {
                holder.ivFavorite.setVisibility(View.GONE);
            }
        }

        // More button (MyRent)
        if (holder.btnMore != null) {
            if (!showFavoriteButton) {
                holder.btnMore.setVisibility(View.VISIBLE);
                holder.btnMore.setOnClickListener(v -> {
                    if (onRentalClickListener != null) onRentalClickListener.onMoreClick(rental);
                });
            } else {
                holder.btnMore.setVisibility(View.GONE);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (onRentalClickListener != null) onRentalClickListener.onRentalClick(rental);
        });
    }

    private void updateFavoriteIcon(RentalViewHolder holder, Rental rental) {
        if (holder.ivFavorite != null) {
            holder.ivFavorite.setImageResource(
                    rental.isFavorite() ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline
            );
        }
    }

    private void loadImageFromUrl(String imageUrl, ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_placeholder);
        new ImageLoadTask(imageView).execute(imageUrl);
    }

    private static class ImageLoadTask extends AsyncTask<String, Void, Bitmap> {
        private final ImageView imageView;
        public ImageLoadTask(ImageView imageView) { this.imageView = imageView; }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String imageUrl = urls[0];
            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                try (InputStream input = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(input);
                }
            } catch (Exception e) { e.printStackTrace(); return null; }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null && imageView != null) imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public int getItemCount() { return rentalList != null ? rentalList.size() : 0; }

    public void updateList(List<Rental> newList) {
        this.rentalList = newList;
        notifyDataSetChanged();
    }

    public static class RentalViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProperty;
        TextView tvTitle, tvLocation, tvPrice, tvStatus, tvBedBath;
        ImageButton btnMore;
        ImageView ivFavorite;

        public RentalViewHolder(View itemView) {
            super(itemView);
            ivProperty = itemView.findViewById(R.id.iv_property);
            tvTitle   = itemView.findViewById(R.id.tv_title);
            tvLocation= itemView.findViewById(R.id.tv_location);
            tvPrice   = itemView.findViewById(R.id.tv_price);
            tvStatus  = itemView.findViewById(R.id.tv_status);
            btnMore   = itemView.findViewById(R.id.btn_more);
            tvBedBath = itemView.findViewById(R.id.tv_bed_bath);
            ivFavorite= itemView.findViewById(R.id.iv_favorite);
        }
    }
}
