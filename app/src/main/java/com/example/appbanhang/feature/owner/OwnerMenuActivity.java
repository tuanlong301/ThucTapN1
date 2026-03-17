package com.example.appbanhang.feature.owner;

import android.os.Bundle;

import com.example.appbanhang.common.base.BaseActivity;
import com.example.appbanhang.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.viewpager2.widget.ViewPager2;

public class OwnerMenuActivity extends BaseActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_menu);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new OwnerPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Doanh thu"); break;
                case 1: tab.setText("Quản lý bàn và nhân viên"); break;
                case 2: tab.setText("Quản lý món"); break;
            }
        }).attach();

        setSupportActionBar(findViewById(R.id.toolbar));
    }
}
