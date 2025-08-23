package com.example.appbanhang;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class ConfirmedOrdersFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Placeholder: Hiện một TextView tạm thời
        View view = inflater.inflate(android.R.layout.simple_list_item_1, container, false);
        ((TextView) view.findViewById(android.R.id.text1)).setText("Đơn hàng đã xác nhận (Placeholder)");
        return view;
    }
}