package com.khstay.myapplication.ui.auth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.khstay.myapplication.R;

public class OnboardingAdapter extends PagerAdapter {

    private Context context;

    private int[] images = {
            R.drawable.ic_search,
            R.drawable.ic_location,
            R.drawable.ic_heart
    };

    private String[] titles = {
            "Discover Your Perfect Home",
            "Location-Based Search",
            "Save Your Favorites"
    };

    private String[] descriptions = {
            "Browse thousands of properties and find the perfect place that matches your lifestyle and budget",
            "Find homes near you or explore properties in your desired neighborhood with our smart location features",
            "Keep track of properties you love and get notified when similar homes become available"
    };

    public OnboardingAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getCount() {
        return titles.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.onboardingslide, container, false);

        ImageView imageView = view.findViewById(R.id.slideImage);
        TextView titleView = view.findViewById(R.id.slideTitle);
        TextView descView = view.findViewById(R.id.slideDescription);

        imageView.setImageResource(images[position]);
        titleView.setText(titles[position]);
        descView.setText(descriptions[position]);

        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }
}