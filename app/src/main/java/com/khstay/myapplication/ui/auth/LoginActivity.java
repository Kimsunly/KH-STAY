package com.khstay.myapplication.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.khstay.myapplication.MainActivity;
import com.khstay.myapplication.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogle, btnFacebook;
    private TextView tvForgotPassword, tvSignUp;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private CallbackManager mCallbackManager;
    private static final int RC_SIGN_IN = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mCallbackManager = CallbackManager.Factory.create();
        registerFacebookCallback();

        setupClickListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        btnFacebook = findViewById(R.id.btnFacebook);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> handleEmailLogin());
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        btnFacebook.setOnClickListener(v -> {
            LoginManager.getInstance().logInWithReadPermissions(
                    LoginActivity.this, Arrays.asList("email", "public_profile"));
        });
        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Forgot Password - Coming Soon", Toast.LENGTH_SHORT).show());
        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }

    private void handleEmailLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Email and Password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login Failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void registerFacebookCallback() {
        LoginManager.getInstance().registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Toast.makeText(LoginActivity.this, "Facebook login cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(LoginActivity.this, "Facebook login error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                error.printStackTrace();
            }
        });
    }

    private void handleFacebookAccessToken(AccessToken token) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveOrUpdateUserData(user);
                        }
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Facebook Authentication Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveOrUpdateUserData(user);
                        }
                        navigateToMain();
                    } else {
                        Toast.makeText(LoginActivity.this, "Google Authentication Failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mCallbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                        .getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In Failed. Error Code: " + e.getStatusCode(),
                        Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private void saveOrUpdateUserData(FirebaseUser user) {
        if (user == null) return;

        String uid = user.getUid();
        String displayName = user.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            displayName = "User";
        }

        String avatarUrl = user.getPhotoUrl() != null ?
                user.getPhotoUrl().toString() :
                "https://ui-avatars.com/api/?name=" +
                        displayName.replace(" ", "+") +
                        "&size=200&background=FF6B35&color=fff";

        Map<String, Object> userData = new HashMap<>();
        userData.put("userId", uid);
        userData.put("displayName", displayName);
        userData.put("email", user.getEmail());
        userData.put("photoUrl", avatarUrl);
        userData.put("phone", user.getPhoneNumber());
        userData.put("updatedAt", Timestamp.now());
        userData.put("role", "user");

        // Use set with merge to create or update
        db.collection("users").document(uid)
                .set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    // Add createdAt only for new documents
                    db.collection("users").document(uid).get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.contains("createdAt")) {
                                    db.collection("users").document(uid)
                                            .update("createdAt", Timestamp.now());
                                }
                            });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save user data: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void navigateToMain() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Login failed, please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = user.getDisplayName();
        if (name == null || name.isEmpty()) {
            name = user.getEmail();
        }

        Toast.makeText(this, "Welcome " + name, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}