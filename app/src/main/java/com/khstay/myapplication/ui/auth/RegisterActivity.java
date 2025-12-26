package com.khstay.myapplication.ui.auth;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khstay.myapplication.R;
import com.khstay.myapplication.MainActivity;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private ImageView ivBack;
    private TextInputEditText etFullName, etEmail, etBirthDate, etPassword;
    private Spinner spinnerCountryCode;
    private EditText etPhoneNumber;
    private MaterialButton btnRegister;
    private View tvLogin;
    private AuthViewModel viewModel;
    private FirebaseFirestore db;

    // Default profile picture URL (you can use a Firebase Storage URL or drawable)
    private static final String DEFAULT_PROFILE_URL = "https://ui-avatars.com/api/?name=User&size=200&background=FF6B35&color=fff";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
        setupCountryCodeSpinner();
        setupDatePicker();
        observeViewModel();
    }

    private void initViews() {
        ivBack = findViewById(R.id.ivBack);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etBirthDate = findViewById(R.id.etBirthDate);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupClickListeners() {
        ivBack.setOnClickListener(v -> finish());
        btnRegister.setOnClickListener(v -> handleRegister());
        tvLogin.setOnClickListener(v -> finish());
    }

    private void setupCountryCodeSpinner() {
        String[] countryCodes = {"+855", "+1", "+44", "+91", "+86", "+81", "+82", "+84"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, countryCodes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCountryCode.setAdapter(adapter);
        spinnerCountryCode.setSelection(0); // Default to Cambodia +855
    }

    private void setupDatePicker() {
        // Allow manual typing by removing focusable=false
        etBirthDate.setFocusable(true);
        etBirthDate.setFocusableInTouchMode(true);

        // Show calendar when clicking the calendar icon (end icon)
        etBirthDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                RegisterActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format("%02d/%02d/%d",
                            selectedDay, selectedMonth + 1, selectedYear);
                    etBirthDate.setText(date);
                },
                year, month, day
        );
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private boolean isValidDateFormat(String date) {
        // Check if date matches DD/MM/YYYY format
        if (!date.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return false;
        }

        try {
            String[] parts = date.split("/");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);

            // Basic validation
            if (year < 1900 || year > Calendar.getInstance().get(Calendar.YEAR)) {
                return false;
            }
            if (month < 1 || month > 12) {
                return false;
            }
            if (day < 1 || day > 31) {
                return false;
            }

            // Check for valid day in month
            Calendar cal = Calendar.getInstance();
            cal.setLenient(false);
            cal.set(year, month - 1, day);
            cal.getTime(); // This will throw exception if date is invalid

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void handleRegister() {
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String birthDate = etBirthDate.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(birthDate)) {
            etBirthDate.setError("Birth date is required");
            etBirthDate.requestFocus();
            return;
        }

        if (!isValidDateFormat(birthDate)) {
            etBirthDate.setError("Invalid date format. Use DD/MM/YYYY");
            etBirthDate.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phoneNumber)) {
            etPhoneNumber.setError("Phone number is required");
            etPhoneNumber.requestFocus();
            return;
        }

        if (phoneNumber.length() < 7) {
            etPhoneNumber.setError("Please enter a valid phone number");
            etPhoneNumber.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");
        viewModel.signup(email, password);
    }

    private void observeViewModel() {
        viewModel.getUserLiveData().observe(this, user -> {
            if (user != null) {
                // Create user document in Firestore
                String fullName = etFullName.getText().toString().trim();
                String countryCode = spinnerCountryCode.getSelectedItem().toString();
                String phoneNumber = etPhoneNumber.getText().toString().trim();
                String fullPhone = countryCode + phoneNumber;
                String birthDate = etBirthDate.getText().toString().trim();

                // Generate default avatar with user's initials
                String avatarUrl = "https://ui-avatars.com/api/?name=" +
                        fullName.replace(" ", "+") +
                        "&size=200&background=FF6B35&color=fff";

                Map<String, Object> userData = new HashMap<>();
                userData.put("displayName", fullName);
                userData.put("email", user.getEmail());
                userData.put("phone", fullPhone);
                userData.put("birthDate", birthDate);
                userData.put("photoUrl", avatarUrl);
                userData.put("createdAt", Timestamp.now());
                userData.put("updatedAt", Timestamp.now());
                userData.put("role", "user"); // Default role

                db.collection("users")
                        .document(user.getUid())
                        .set(userData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Registration Successful! Welcome " + fullName,
                                    Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to save user data: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            btnRegister.setEnabled(true);
                            btnRegister.setText("Register");
                        });
            }
        });

        viewModel.getErrorLiveData().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, "Registration Failed: " + error, Toast.LENGTH_LONG).show();
                btnRegister.setEnabled(true);
                btnRegister.setText("Register");
            }
        });
    }
}