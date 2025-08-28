package com.example.appbanhang;

import android.os.Bundle;
import android.widget.Toast;

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

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Tabs + Pager
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Lấy orderId vừa tạo (nếu đi từ CartActivity)
        String highlightId = getIntent().getStringExtra("justCreatedOrderId");

        // Adapter có hỗ trợ truyền highlightId vào tab "Đơn hàng"
        AdminAdapter adapter = new AdminAdapter(this, highlightId);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2); // giữ 3 tab sống

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Đơn hàng"); break;
                case 1: tab.setText("Đơn hàng đã xác nhận"); break;
                case 2: tab.setText("Quản lý bàn"); break;
            }
        }).attach();

        // Nút Refresh: gửi "force_refresh" cho các fragment đang lắng nghe
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            getSupportFragmentManager().setFragmentResult("force_refresh", new Bundle());
            Toast.makeText(this, "Đã làm mới dữ liệu", Toast.LENGTH_SHORT).show();
        });

        // Nút In tất cả hóa đơn (placeholder)
        findViewById(R.id.btnPrintAll).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng in đang được phát triển!", Toast.LENGTH_SHORT).show()
        );
    }
}
