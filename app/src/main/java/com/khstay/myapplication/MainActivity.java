package com.khstay.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.khstay.myapplication.ui.auth.LoginActivity;
import com.khstay.myapplication.ui.home.HomeFragment;
import com.khstay.myapplication.ui.profile.ProfileFragment;
import com.khstay.myapplication.ui.rental.MyRentFragment;
import com.khstay.myapplication.ui.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        auth = FirebaseAuth.getInstance();

        // ===============================
        // Redirect to Login if not logged in
        // ===============================
        if (auth.getCurrentUser() == null) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // ============================
        // TOP APP BAR
        // ============================
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        // ============================
        // BOTTOM NAVIGATION
        // ============================
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;

            int id = item.getItemId();
            if (id == R.id.nav_search) {
                fragment = new SearchFragment();
            } else if (id == R.id.nav_my_rent) {
                fragment = new MyRentFragment();
            } else if (id == R.id.nav_profile) {
                fragment = new ProfileFragment();
            } else {
                fragment = new HomeFragment();
            }

            return loadFragment(fragment);
        });
    }

    // ============================
    // Inflate menu for toolbar
    // ============================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // This line reads your menu XML and puts it on the toolbar
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }

    // ============================
    // Handle toolbar menu clicks
    // ============================
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            auth.signOut();
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ============================
    // Load fragment helper
    // ============================
    private boolean loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragmentContainer, fragment)
                .commit();
        return true;
    }
}
