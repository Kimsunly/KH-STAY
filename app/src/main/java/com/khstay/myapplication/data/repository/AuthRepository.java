package com.khstay.myapplication.data.repository;

import com.khstay.myapplication.data.firebase.AuthService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;

public class AuthRepository {
    private final AuthService authService = new AuthService();

    public void login(String email, String password, OnCompleteListener<AuthResult> listener) {
        authService.login(email, password, listener);
    }

    public void signup(String email, String password, OnCompleteListener<AuthResult> listener) {
        authService.signup(email, password, listener);
    }

    public void logout() {
        authService.logout();
    }

    public FirebaseUser getCurrentUser() {
        return authService.getCurrentUser();
    }

    public boolean isUserSignedIn() {
        return authService.isUserSignedIn();
    }

    public void sendPasswordResetEmail(String email, OnCompleteListener<Void> listener) {
        authService.sendPasswordResetEmail(email, listener);
    }
}