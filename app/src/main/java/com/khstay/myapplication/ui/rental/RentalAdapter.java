package com.khstay.myapplication.ui.rental;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.khstay.myapplication.R;

public class RentalAdapter extends RecyclerView.Adapter<RentalAdapter.RentalViewHolder> {

    private Context context;
    private List<Rental> rentalList;
    private OnRentalClickListener onRentalClickListener;

    public interface OnRentalClickListener {
        void onRentalClick(Rental rental);
        void onMoreClick(Rental rental);
    }

    public RentalAdapter(Context context, List<Rental> rentalList) {
        this.context = context;
        this.rentalList = rentalList;
    }

    public void setOnRentalClickListener(OnRentalClickListener listener) {
        this.onRentalClickListener = listener;
    }

    @Override
    public RentalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_rental, parent, false);
        return new RentalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RentalViewHolder holder, int position) {
        Rental rental = rentalList.get(position);

        holder.ivProperty.setImageResource(rental.getImageResId());
        holder.tvTitle.setText(rental.getTitle());
        holder.tvLocation.setText(rental.getLocation());
        holder.tvPrice.setText("$" + rental.getPrice() + "/month");
        holder.tvStatus.setText(rental.getStatus());

        // Set status badge based on status
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

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (onRentalClickListener != null) {
                onRentalClickListener.onRentalClick(rental);
            }
        });

        holder.btnMore.setOnClickListener(v -> {
            if (onRentalClickListener != null) {
                onRentalClickListener.onMoreClick(rental);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rentalList != null ? rentalList.size() : 0;
    }

    public void updateList(List<Rental> newList) {
        this.rentalList = newList;
        notifyDataSetChanged();
    }

    public static class RentalViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProperty;
        TextView tvTitle;
        TextView tvLocation;
        TextView tvPrice;
        TextView tvStatus;
        ImageButton btnMore;

        public RentalViewHolder(View itemView) {
            super(itemView);
            ivProperty = itemView.findViewById(R.id.iv_property);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnMore = itemView.findViewById(R.id.btn_more);
        }
    }
}