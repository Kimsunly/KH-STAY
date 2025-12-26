package com.khstay.myapplication.ui.rental;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khstay.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private static final String TAG = "BookingActivity";
    public static final String EXTRA_RENTAL_ID = "RENTAL_ID";
    public static final String EXTRA_RENTAL_TITLE = "RENTAL_TITLE";
    public static final String EXTRA_RENTAL_PRICE = "RENTAL_PRICE";
    public static final String EXTRA_OWNER_ID = "OWNER_ID";

    private ImageButton btnBack;
    private TextView tvRentalTitle, tvRentalPrice, tvTotalPrice;
    private EditText etFullName, etPhone, etEmail, etNotes;
    private TextView tvCheckInDate, tvCheckOutDate;
    private Button btnConfirmBooking;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String rentalId;
    private String rentalTitle;
    private double rentalPrice;
    private String ownerId;

    private Calendar checkInCalendar;
    private Calendar checkOutCalendar;
    private SimpleDateFormat dateFormat;

    // 🎨 Skeleton loading
    private com.facebook.shimmer.ShimmerFrameLayout shimmerBooking;
    private View contentLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

        // Get data from intent
        rentalId = getIntent().getStringExtra(EXTRA_RENTAL_ID);
        rentalTitle = getIntent().getStringExtra(EXTRA_RENTAL_TITLE);
        rentalPrice = getIntent().getDoubleExtra(EXTRA_RENTAL_PRICE, 0.0);
        ownerId = getIntent().getStringExtra(EXTRA_OWNER_ID);

        if (TextUtils.isEmpty(rentalId)) {
            Toast.makeText(this, "Invalid booking data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupClickListeners();
        displayRentalInfo();
        loadUserData();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvRentalTitle = findViewById(R.id.tvRentalTitle);
        tvRentalPrice = findViewById(R.id.tvRentalPrice);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        etFullName = findViewById(R.id.etFullName);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        etNotes = findViewById(R.id.etNotes);
        tvCheckInDate = findViewById(R.id.tvCheckInDate);
        tvCheckOutDate = findViewById(R.id.tvCheckOutDate);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        // 🎨 Initialize skeleton views
        try {
            shimmerBooking = findViewById(R.id.shimmerBooking);
            contentLayout = findViewById(R.id.contentLayout);
        } catch (Exception e) {
            Log.e(TAG, "Shimmer views not found in layout", e);
            shimmerBooking = null;
            contentLayout = null;
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        tvCheckInDate.setOnClickListener(v -> showDatePicker(true));
        tvCheckOutDate.setOnClickListener(v -> showDatePicker(false));

        btnConfirmBooking.setOnClickListener(v -> confirmBooking());
    }

    private void loadUserData() {
        if (auth.getCurrentUser() == null) {
            showSkeletonLoading(false);
            return;
        }

        // 🎨 Show skeleton loading
        showSkeletonLoading(true);

        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("displayName");
                        String phone = doc.getString("phone");
                        String email = doc.getString("email");

                        if (name != null) etFullName.setText(name);
                        if (phone != null) etPhone.setText(phone);
                        if (email != null) etEmail.setText(email);
                    }

                    // 🎨 Hide skeleton after data loaded
                    showSkeletonLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    // 🎨 Hide skeleton even on error
                    showSkeletonLoading(false);
                });
    }

    // 🎨 Helper method to show/hide skeleton
    private void showSkeletonLoading(boolean show) {
        if (shimmerBooking != null && contentLayout != null) {
            if (show) {
                shimmerBooking.startShimmer();
                shimmerBooking.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.GONE);
            } else {
                shimmerBooking.stopShimmer();
                shimmerBooking.setVisibility(View.GONE);
                contentLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    private void displayRentalInfo() {
        tvRentalTitle.setText(rentalTitle);
        tvRentalPrice.setText(String.format("$%.0f/month", rentalPrice));
        calculateTotal();
    }

    private void showDatePicker(boolean isCheckIn) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog picker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    if (isCheckIn) {
                        checkInCalendar = selected;
                        tvCheckInDate.setText(dateFormat.format(selected.getTime()));
                    } else {
                        checkOutCalendar = selected;
                        tvCheckOutDate.setText(dateFormat.format(selected.getTime()));
                    }

                    calculateTotal();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        picker.getDatePicker().setMinDate(System.currentTimeMillis());
        picker.show();
    }

    private void calculateTotal() {
        if (checkInCalendar == null || checkOutCalendar == null) {
            tvTotalPrice.setText("Select dates to see total");
            return;
        }

        long diffInMillis = checkOutCalendar.getTimeInMillis() - checkInCalendar.getTimeInMillis();
        int days = (int) (diffInMillis / (1000 * 60 * 60 * 24));

        if (days <= 0) {
            tvTotalPrice.setText("Invalid date range");
            return;
        }

        double dailyRate = rentalPrice / 30.0;
        double total = dailyRate * days;

        tvTotalPrice.setText(String.format("Total: $%.2f (%d days)", total, days));
    }

    private void confirmBooking() {
        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String notes = etNotes.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Name required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Phone required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email required");
            return;
        }
        if (checkInCalendar == null || checkOutCalendar == null) {
            Toast.makeText(this, "Please select check-in and check-out dates",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to book", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText("Processing...");

        // Create booking document
        Map<String, Object> booking = new HashMap<>();
        booking.put("rentalId", rentalId);
        booking.put("rentalTitle", rentalTitle);
        booking.put("rentalPrice", rentalPrice);
        booking.put("ownerId", ownerId);
        booking.put("userId", auth.getCurrentUser().getUid());
        booking.put("guestName", fullName);
        booking.put("guestPhone", phone);
        booking.put("guestEmail", email);
        booking.put("notes", notes);
        booking.put("checkInDate", new Timestamp(checkInCalendar.getTime()));
        booking.put("checkOutDate", new Timestamp(checkOutCalendar.getTime()));
        booking.put("status", "pending");
        booking.put("createdAt", Timestamp.now());

        // Calculate total
        long diffInMillis = checkOutCalendar.getTimeInMillis() - checkInCalendar.getTimeInMillis();
        int days = (int) (diffInMillis / (1000 * 60 * 60 * 24));
        double dailyRate = rentalPrice / 30.0;
        double total = dailyRate * days;
        booking.put("totalPrice", total);
        booking.put("numberOfDays", days);

        db.collection("bookings")
                .add(booking)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Booking successful!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText("Confirm Booking");
                });
    }
}