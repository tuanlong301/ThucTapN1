package com.example.appbanhang.owner;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OwnerPagerAdapter extends FragmentStateAdapter {
    public OwnerPagerAdapter(@NonNull FragmentActivity fa) { super(fa); }
    @NonNull @Override public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new OwnerStatsFragment();
            case 1: return new OwnerTablesFragment();
            default: return new OwnerProductsFragment();
        }
    }
    @Override public int getItemCount() { return 3; }
}
