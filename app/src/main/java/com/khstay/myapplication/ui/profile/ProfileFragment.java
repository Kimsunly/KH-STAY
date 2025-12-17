package com.khstay.myapplication.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.khstay.myapplication.R;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Buttons
        ImageView backButton = view.findViewById(R.id.backButton);
        ImageView cameraIcon = view.findViewById(R.id.cameraIcon);
        TextView signOut = view.findViewById(R.id.signOut);

        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        cameraIcon.setOnClickListener(v ->
                Toast.makeText(getContext(), "Change Profile Picture", Toast.LENGTH_SHORT).show()
        );
        signOut.setOnClickListener(v ->
                Toast.makeText(getContext(), "Sign Out clicked", Toast.LENGTH_SHORT).show()
        );

        // Menu items (THIS REMOVES ? ICONS)
        setMenuItem(view, R.id.settingsItem, R.drawable.ic_settings, "Settings");
        setMenuItem(view, R.id.paymentItem, R.drawable.ic_payment, "Payment");
        setMenuItem(view, R.id.notificationItem, R.drawable.ic_notifications, "Notifications");
        setMenuItem(view, R.id.recentViewedItem, R.drawable.ic_recentview, "Recent Viewed");
        setMenuItem(view, R.id.aboutItem, R.drawable.ic_info, "About");

        return view;
    }

    private void setMenuItem(View root, int itemId, int icon, String title) {
        View item = root.findViewById(itemId);
        ImageView iconView = item.findViewById(R.id.menuIcon);
        TextView titleView = item.findViewById(R.id.menuTitle);

        iconView.setImageResource(icon);
        titleView.setText(title);
    }
}
