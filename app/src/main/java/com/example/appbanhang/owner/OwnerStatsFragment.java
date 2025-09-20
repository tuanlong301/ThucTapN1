package com.example.appbanhang.owner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.appbanhang.R;

public class OwnerStatsFragment extends Fragment {

    private Button btnToday, btnThisWeek, btnThisMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_owner_stats, container, false);

        btnToday = v.findViewById(R.id.btnToday);
        btnThisWeek = v.findViewById(R.id.btnThisWeek);
        btnThisMonth = v.findViewById(R.id.btnThisMonth);

        View.OnClickListener chipClick = view -> setChipSelected((Button) view);

        btnToday.setOnClickListener(chipClick);
        btnThisWeek.setOnClickListener(chipClick);
        btnThisMonth.setOnClickListener(chipClick);

        // Mặc định chọn "Hôm nay"
        setChipSelected(btnToday);

        return v;
    }

    private void setChipSelected(Button selected) {
        Button[] all = {btnToday, btnThisWeek, btnThisMonth};
        for (Button b : all) {
            b.setSelected(b == selected);
        }
    }
}
