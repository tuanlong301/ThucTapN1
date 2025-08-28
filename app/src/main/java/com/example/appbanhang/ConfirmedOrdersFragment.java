package com.example.appbanhang;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ConfirmedOrdersFragment extends Fragment {

    public static ConfirmedOrdersFragment newInstance() {
        ConfirmedOrdersFragment f = new ConfirmedOrdersFragment();
        f.setArguments(new Bundle());
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        TextView tv = new TextView(requireContext());
        tv.setText("Đơn hàng đã xác nhận (Placeholder)");
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return tv;

        // Hoặc dùng layout có sẵn:
        // View v = inflater.inflate(android.R.layout.simple_list_item_1, container, false);
        // ((TextView) v.findViewById(android.R.id.text1)).setText("Đơn hàng đã xác nhận (Placeholder)");
        // return v;
    }
}
