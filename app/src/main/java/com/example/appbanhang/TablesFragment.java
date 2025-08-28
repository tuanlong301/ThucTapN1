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

public class TablesFragment extends Fragment {

    // newInstance PHẢI trả về một instance hợp lệ
    public static TablesFragment newInstance() {
        TablesFragment f = new TablesFragment();
        f.setArguments(new Bundle()); // để sau này truyền args nếu cần
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Placeholder: tạo TextView đơn giản
        TextView tv = new TextView(requireContext());
        tv.setText("Quản lý bàn (Placeholder)");
        tv.setGravity(Gravity.CENTER);
        tv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return tv;

        // Hoặc nếu muốn inflate layout riêng:
        // return inflater.inflate(R.layout.fragment_tables_placeholder, container, false);
    }
}
