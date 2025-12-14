package com.khstay.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;
import com.khstay.myapplication.R;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private LinearLayout dotsLayout;
    private Button btnNext, btnSkip;
    private LinearLayout layoutAuth;
    private OnboardingAdapter adapter;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        layoutAuth = findViewById(R.id.layoutAuth);

        adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);

        addDotsIndicator(0);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                addDotsIndicator(position);
                currentPage = position;

                if (position == 2) {
                    btnNext.setVisibility(View.GONE);
                    btnSkip.setVisibility(View.GONE);
                    layoutAuth.setVisibility(View.VISIBLE);
                } else {
                    btnNext.setVisibility(View.VISIBLE);
                    btnSkip.setVisibility(View.VISIBLE);
                    layoutAuth.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(currentPage + 1);
            }
        });

        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewPager.setCurrentItem(2);
            }
        });

        findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Removed finishOnboarding() - just go to login
                Intent intent = new Intent(OnboardingActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.btnRegister).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Removed finishOnboarding() - just go to register
                Intent intent = new Intent(OnboardingActivity.this, RegisterActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void addDotsIndicator(int position) {
        dotsLayout.removeAllViews();
        ImageView[] dots = new ImageView[3];

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new ImageView(this);
            if (i == position) {
                dots[i].setImageResource(R.drawable.dot_active);
            } else {
                dots[i].setImageResource(R.drawable.dot_inactive);
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dotsLayout.addView(dots[i], params);
        }
    }
}