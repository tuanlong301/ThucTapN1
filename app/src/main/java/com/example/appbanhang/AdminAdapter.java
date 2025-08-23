package com.example.appbanhang;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminAdapter extends FragmentStateAdapter {

    public AdminAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PendingOrdersFragment(); // Placeholder cho tab Đơn hàng
            case 1:
                return new ConfirmedOrdersFragment(); // Placeholder cho tab Đơn hàng đã xác nhận
            case 2:
                return new TablesFragment(); // Placeholder cho tab Quản lý bàn
            default:
                return new PendingOrdersFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // 3 tabs
    }
}