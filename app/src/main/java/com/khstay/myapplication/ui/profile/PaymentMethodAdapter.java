package com.khstay.myapplication.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.khstay.myapplication.R;

import java.util.List;

public class PaymentMethodAdapter extends RecyclerView.Adapter<PaymentMethodAdapter.ViewHolder> {

    private List<PaymentMethod> paymentMethods;
    private OnPaymentMethodClickListener listener;

    public interface OnPaymentMethodClickListener {
        void onPaymentMethodClick(PaymentMethod method);
    }

    public PaymentMethodAdapter(List<PaymentMethod> paymentMethods, OnPaymentMethodClickListener listener) {
        this.paymentMethods = paymentMethods;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentMethod method = paymentMethods.get(position);
        holder.bind(method, listener);
    }

    @Override
    public int getItemCount() {
        return paymentMethods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cardIcon;
        TextView cardType, cardNumber, expiryDate, defaultBadge;

        ViewHolder(View itemView) {
            super(itemView);
            cardIcon = itemView.findViewById(R.id.cardIcon);
            cardType = itemView.findViewById(R.id.cardType);
            cardNumber = itemView.findViewById(R.id.cardNumber);
            expiryDate = itemView.findViewById(R.id.expiryDate);
            defaultBadge = itemView.findViewById(R.id.defaultBadge);
        }

        void bind(PaymentMethod method, OnPaymentMethodClickListener listener) {
            cardType.setText(method.getCardType());
            cardNumber.setText("•••• " + method.getLastFourDigits());
            expiryDate.setText("Expires " + method.getExpiryDate());
            defaultBadge.setVisibility(method.isDefault() ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> listener.onPaymentMethodClick(method));
        }
    }
}