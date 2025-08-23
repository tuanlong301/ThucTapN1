package com.example.appbanhang;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class AdminMenu extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Khởi tạo TabLayout và ViewPager
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Thiết lập adapter cho ViewPager
        AdminAdapter adapter = new AdminAdapter(this);
        viewPager.setAdapter(adapter);

        // Kết nối TabLayout với ViewPager
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Đơn hàng");
                            break;
                        case 1:
                            tab.setText("Đơn hàng đã xác nhận");
                            break;
                        case 2:
                            tab.setText("Quản lý bàn");
                            break;
                    }
                }).attach();

        // Xử lý nút Refresh
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            // Placeholder: Logic refresh (sẽ kết nối với Firestore sau)
            // Ví dụ: Gọi adapter.notifyDataSetChanged() trong fragment
        });

        // Xử lý nút In tất cả hóa đơn
        findViewById(R.id.btnPrintAll).setOnClickListener(v -> {
            // Placeholder: Logic in tất cả hóa đơn (sẽ tích hợp PrintManager sau)
        });
    }
}