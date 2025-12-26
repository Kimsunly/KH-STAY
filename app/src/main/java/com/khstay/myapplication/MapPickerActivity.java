package com.khstay.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.khstay.myapplication.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String EXTRA_LATITUDE = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_ADDRESS = "address";

    private static final int REQ_LOCATION = 1011;

    private MapView mapView;
    private GoogleMap googleMap;
    private ImageView ivBack;
    private TextView tvSelectedAddress;
    private Button btnConfirmLocation;

    private LatLng selectedLocation;
    private String selectedAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        ivBack = findViewById(R.id.ivBack);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        mapView = findViewById(R.id.mapView);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        ivBack.setOnClickListener(v -> finish());

        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(EXTRA_LATITUDE, selectedLocation.latitude);
                resultIntent.putExtra(EXTRA_LONGITUDE, selectedLocation.longitude);
                resultIntent.putExtra(EXTRA_ADDRESS, selectedAddress);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Please select a location on the map", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Enable my location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQ_LOCATION);
        }

        // Default location: Phnom Penh
        LatLng phnomPenh = new LatLng(11.5564, 104.9282);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(phnomPenh, 12f));

        // Set map click listener
        googleMap.setOnMapClickListener(latLng -> {
            selectedLocation = latLng;
            updateMarker(latLng);
            getAddressFromLatLng(latLng);
        });
    }

    private void updateMarker(LatLng latLng) {
        if (googleMap != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location"));
        }
    }

    private void getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                if (address.getThoroughfare() != null) {
                    sb.append(address.getThoroughfare()).append(", ");
                }
                if (address.getSubLocality() != null) {
                    sb.append(address.getSubLocality()).append(", ");
                }
                if (address.getLocality() != null) {
                    sb.append(address.getLocality()).append(", ");
                }
                if (address.getCountryName() != null) {
                    sb.append(address.getCountryName());
                }

                selectedAddress = sb.toString();
                tvSelectedAddress.setText(selectedAddress);
            } else {
                selectedAddress = "Lat: " + String.format("%.4f", latLng.latitude) +
                        ", Lng: " + String.format("%.4f", latLng.longitude);
                tvSelectedAddress.setText(selectedAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
            selectedAddress = "Lat: " + String.format("%.4f", latLng.latitude) +
                    ", Lng: " + String.format("%.4f", latLng.longitude);
            tvSelectedAddress.setText(selectedAddress);
        }
    }

    @Override
    protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override
    protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override
    protected void onPause() { mapView.onPause(); super.onPause(); }
    @Override
    protected void onStop() { mapView.onStop(); super.onStop(); }
    @Override
    protected void onDestroy() { mapView.onDestroy(); super.onDestroy(); }
    @Override
    public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && googleMap != null) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                }
            }
        }
    }
}