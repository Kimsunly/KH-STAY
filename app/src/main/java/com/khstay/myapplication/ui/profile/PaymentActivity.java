package com.khstay.myapplication.ui.profile;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.khstay.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class PaymentActivity extends AppCompatActivity {

    private ImageView backButton;
    private RecyclerView paymentMethodsRecyclerView;
    private Button addPaymentMethodButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private PaymentMethodAdapter adapter;
    private List<PaymentMethod> paymentMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadPaymentMethods();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        paymentMethodsRecyclerView = findViewById(R.id.paymentMethodsRecyclerView);
        addPaymentMethodButton = findViewById(R.id.addPaymentMethodButton);
    }

    private void setupRecyclerView() {
        paymentMethods = new ArrayList<>();
        adapter = new PaymentMethodAdapter(paymentMethods, this::onPaymentMethodClick);
        paymentMethodsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        paymentMethodsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        addPaymentMethodButton.setOnClickListener(v -> {
            // TODO: Open add payment method dialog
            Toast.makeText(this, "Add payment method coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadPaymentMethods() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
                .collection("paymentMethods")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    paymentMethods.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        PaymentMethod method = doc.toObject(PaymentMethod.class);
                        paymentMethods.add(method);
                    }
                    adapter.notifyDataSetChanged();

                    if (paymentMethods.isEmpty()) {
                        Toast.makeText(this, "No payment methods found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onPaymentMethodClick(PaymentMethod method) {
        Toast.makeText(this, "Selected: " + method.getCardType(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}