package com.khstay.myapplication.ui.rental;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.khstay.myapplication.R;

public class MyRentFragment extends Fragment {

    public MyRentFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_rent, container, false);
    }
}
