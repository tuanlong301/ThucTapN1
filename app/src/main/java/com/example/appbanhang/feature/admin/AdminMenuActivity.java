package com.example.appbanhang.feature.admin;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.appbanhang.common.base.BaseActivity;
import com.example.appbanhang.R;
import com.example.appbanhang.feature.admin.adapter.AdminPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.viewpager2.widget.ViewPager2;

/**
 * View-only: observe LiveData từ AdminMenuViewModel.
 */
public class AdminMenuActivity extends BaseActivity {

    private AdminMenuViewModel vm;
    private MediaPlayer dingPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        String highlightId = getIntent().getStringExtra("highlight_order_id");
        viewPager.setAdapter(new AdminPagerAdapter(this, highlightId));

        new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> {
            switch (pos) {
                case 0: tab.setText("Đơn hàng"); break;
                case 1: tab.setText("Đã xác nhận"); break;
                case 2: tab.setText("Bàn"); break;
            }
        }).attach();

        setSupportActionBar(findViewById(R.id.toolbar));

        // ViewModel for staff calls
        vm = new ViewModelProvider(this).get(AdminMenuViewModel.class);

        vm.getNextCall().observe(this, event -> {
            AdminMenuViewModel.StaffCall call = event.getContentIfNotHandled();
            if (call == null) return;
            playDing();
            new AlertDialog.Builder(this)
                    .setTitle("Gọi nhân viên")
                    .setMessage(call.tableName + " đang gọi nhân viên!")
                    .setCancelable(false)
                    .setPositiveButton("Đã nhận", (d, w) -> {
                        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                        String adminUid = u != null ? u.getUid() : null;
                        vm.acknowledgeCall(call.callId, adminUid);
                    })
                    .show();
        });

        vm.getToast().observe(this, event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        vm.startListening();
    }

    private void playDing() {
        try {
            if (dingPlayer != null) { dingPlayer.release(); dingPlayer = null; }
            dingPlayer = MediaPlayer.create(this, R.raw.ting);
            if (dingPlayer != null) {
                dingPlayer.setOnCompletionListener(MediaPlayer::release);
                dingPlayer.start();
            }
        } catch (Exception ignore) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        vm.stopListening();
        if (dingPlayer != null) { dingPlayer.release(); dingPlayer = null; }
    }
}
