package com.khstay.myapplication.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.khstay.myapplication.data.repository.AuthRepository;
import com.google.firebase.auth.FirebaseUser;

public class AuthViewModel extends ViewModel {
    private final AuthRepository repository = new AuthRepository();
    private final MutableLiveData<FirebaseUser> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public LiveData<FirebaseUser> getUserLiveData() { return userLiveData; }
    public LiveData<String> getErrorLiveData() { return errorLiveData; }

    public void login(String email, String password) {
        repository.login(email, password, task -> {
            if (task.isSuccessful()) {
                userLiveData.setValue(repository.getCurrentUser());
            } else {
                errorLiveData.setValue(task.getException().getMessage());
            }
        });
    }

    public void signup(String email, String password) {
        repository.signup(email, password, task -> {
            if (task.isSuccessful()) {
                userLiveData.setValue(repository.getCurrentUser());
            } else {
                errorLiveData.setValue(task.getException().getMessage());
            }
        });
    }

    public void logout() {
        repository.logout();
        userLiveData.setValue(null);
    }
}
