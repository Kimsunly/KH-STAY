package com.khstay.myapplication.ui.rental;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.khstay.myapplication.MainActivity;
import com.khstay.myapplication.R;
import com.khstay.myapplication.data.repository.RentalRepository;
import com.khstay.myapplication.ui.rental.model.Rental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PostRentalActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQUEST_MAP_PICKER = 1001;
    private static final int MAX_IMAGES = 3;

    private EditText etTitle, etPrice, etBedrooms, etBathrooms, etLocation, etDescription;
    private Spinner spinnerCurrency, spinnerCategory;
    private ImageView ivBack;
    private ImageButton btnAddCategory;
    private View btnAddImage, btnSelectLocation;
    private Button btnPostListing;
    private MapView mapView;
    private GoogleMap googleMap;

    // Multiple images support
    private LinearLayout imagePreviewContainer;
    private TextView tvImageCount;
    private List<Uri> selectedImageUris = new ArrayList<>();

    private Double selectedLatitude;
    private Double selectedLongitude;

    private RentalRepository rentalRepository;
    private FirebaseAuth auth;

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    private List<String> categoryList;
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_rental);

        auth = FirebaseAuth.getInstance();
        rentalRepository = new RentalRepository();

        initViews();
        setupSpinners();
        setupImagePicker();
        setupClickListeners();

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etTitle = findViewById(R.id.etTitle);
        etPrice = findViewById(R.id.etPrice);
        spinnerCurrency = findViewById(R.id.spinnerCurrency);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        etBedrooms = findViewById(R.id.etBedrooms);
        etBathrooms = findViewById(R.id.etBathrooms);
        etLocation = findViewById(R.id.etLocation);
        etDescription = findViewById(R.id.etDescription);
        btnAddImage = findViewById(R.id.btnAddImage);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        tvImageCount = findViewById(R.id.tvImageCount);
        btnSelectLocation = findViewById(R.id.btnSelectLocation);
        mapView = findViewById(R.id.mapView);
        btnPostListing = findViewById(R.id.btnPostListing);

        updateImageCountText();
    }

    private void setupSpinners() {
        String[] currencies = {"USD", "KHR", "EUR", "THB"};
        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(currencyAdapter);

        categoryList = new ArrayList<>(Arrays.asList(
                "Toul Kok", "Psar Derm Tkev", "Dangkao", "Chamkar Mon",
                "Mean Chey", "Sen Sok", "House", "Apartment", "Villa",
                "Condo", "Studio", "Townhouse"
        ));

        categoryAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            addImage(imageUri);
                        }
                    }
                }
        );
    }

    private void addImage(Uri imageUri) {
        if (selectedImageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Maximum " + MAX_IMAGES + " images allowed", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedImageUris.add(imageUri);
        updateImagePreview();
        updateImageCountText();
    }

    private void removeImage(int position) {
        if (position >= 0 && position < selectedImageUris.size()) {
            selectedImageUris.remove(position);
            updateImagePreview();
            updateImageCountText();
        }
    }

    private void updateImagePreview() {
        imagePreviewContainer.removeAllViews();

        for (int i = 0; i < selectedImageUris.size(); i++) {
            final int position = i;
            Uri uri = selectedImageUris.get(i);

            // Create container for each image
            LinearLayout imageContainer = new LinearLayout(this);
            imageContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            );
            containerParams.setMargins(8, 0, 8, 0);
            imageContainer.setLayoutParams(containerParams);

            // Image view
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    300
            );
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            Glide.with(this)
                    .load(uri)
                    .into(imageView);

            // Remove button
            Button removeBtn = new Button(this);
            removeBtn.setText("Remove");
            removeBtn.setTextColor(getResources().getColor(R.color.white));
            removeBtn.setBackgroundColor(getResources().getColor(R.color.primary));
            removeBtn.setOnClickListener(v -> removeImage(position));

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btnParams.setMargins(0, 8, 0, 0);
            removeBtn.setLayoutParams(btnParams);

            // Badge showing image number
            TextView badge = new TextView(this);
            badge.setText("Image " + (position + 1));
            badge.setTextColor(getResources().getColor(R.color.white));
            badge.setBackgroundColor(getResources().getColor(R.color.primary));
            badge.setPadding(12, 6, 12, 6);
            badge.setTextSize(12);

            imageContainer.addView(badge);
            imageContainer.addView(imageView);
            imageContainer.addView(removeBtn);

            imagePreviewContainer.addView(imageContainer);
        }

        // Show/hide add button based on image count
        btnAddImage.setVisibility(selectedImageUris.size() < MAX_IMAGES ? View.VISIBLE : View.GONE);
    }

    private void updateImageCountText() {
        tvImageCount.setText(selectedImageUris.size() + " / " + MAX_IMAGES + " images selected");
        tvImageCount.setTextColor(getResources().getColor(
                selectedImageUris.isEmpty() ? R.color.text_hint : R.color.primary
        ));
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());

        btnAddImage.setOnClickListener(v -> openImagePicker());

        btnSelectLocation.setOnClickListener(v -> openMapPicker());

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        btnPostListing.setOnClickListener(v -> validateAndPostRental());
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Category");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint("Enter category name");

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(50, 20, 50, 20);
        input.setLayoutParams(params);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String newCategory = input.getText().toString().trim();
            if (!newCategory.isEmpty()) {
                if (!categoryList.contains(newCategory)) {
                    categoryList.add(newCategory);
                    categoryAdapter.notifyDataSetChanged();
                    spinnerCategory.setSelection(categoryList.size() - 1);
                    Toast.makeText(this, "Category added: " + newCategory, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Category name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void openMapPicker() {
        Intent intent = new Intent(this, com.khstay.myapplication.MapPickerActivity.class);
        startActivityForResult(intent, REQUEST_MAP_PICKER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MAP_PICKER && resultCode == RESULT_OK && data != null) {
            selectedLatitude = data.getDoubleExtra(com.khstay.myapplication.MapPickerActivity.EXTRA_LATITUDE, 0);
            selectedLongitude = data.getDoubleExtra(com.khstay.myapplication.MapPickerActivity.EXTRA_LONGITUDE, 0);
            String address = data.getStringExtra(com.khstay.myapplication.MapPickerActivity.EXTRA_ADDRESS);

            etLocation.setText(address);
            updateMapMarker(new LatLng(selectedLatitude, selectedLongitude));
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setAllGesturesEnabled(false);

        LatLng phnomPenh = new LatLng(11.5564, 104.9282);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(phnomPenh, 12f));
    }

    private void updateMapMarker(LatLng location) {
        if (googleMap != null) {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions()
                    .position(location)
                    .title("Property Location"));
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        }
    }

    private void validateAndPostRental() {
        String title = etTitle.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String bedroomsStr = etBedrooms.getText().toString().trim();
        String bathroomsStr = etBathrooms.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String currency = spinnerCurrency.getSelectedItem().toString();
        String category = spinnerCategory.getSelectedItem().toString();

        // Validation
        if (title.isEmpty()) {
            etTitle.setError("Title is required");
            etTitle.requestFocus();
            return;
        }

        if (priceStr.isEmpty()) {
            etPrice.setError("Price is required");
            etPrice.requestFocus();
            return;
        }

        if (location.isEmpty()) {
            etLocation.setError("Location is required");
            etLocation.requestFocus();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return;
        }

        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, "Please select at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            etPrice.setError("Invalid price");
            return;
        }

        int bedrooms = bedroomsStr.isEmpty() ? 0 : Integer.parseInt(bedroomsStr);
        int bathrooms = bathroomsStr.isEmpty() ? 0 : Integer.parseInt(bathroomsStr);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to post", Toast.LENGTH_SHORT).show();
            return;
        }

        Rental rental = new Rental();
        rental.setTitle(title);
        rental.setPrice(price);
        rental.setLocation(location);
        rental.setDescription(description);
        rental.setCategory(category);
        rental.setBedrooms(bedrooms);
        rental.setBathrooms(bathrooms);
        rental.setStatus("active");
        rental.setIsPopular(false);
        rental.setOwnerId(currentUser.getUid());

        if (selectedLatitude != null && selectedLongitude != null) {
            rental.setLatitude(selectedLatitude);
            rental.setLongitude(selectedLongitude);
        }

        btnPostListing.setEnabled(false);
        btnPostListing.setText("Uploading " + selectedImageUris.size() + " images...");

        rentalRepository.createRental(rental, selectedImageUris)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(PostRentalActivity.this,
                            "Rental posted successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(PostRentalActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("REFRESH_HOME", true);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnPostListing.setEnabled(true);
                    btnPostListing.setText("Post Listing");
                    Toast.makeText(PostRentalActivity.this,
                            "Failed to post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
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
}