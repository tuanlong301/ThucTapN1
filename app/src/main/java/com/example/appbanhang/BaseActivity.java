package com.example.appbanhang;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.appbanhang.net.NetworkMonitor;

public abstract class BaseActivity extends AppCompatActivity {

    private boolean launchingOffline = false;

    private final NetworkMonitor.Listener netListener = ok -> {
        if (!ok) goOfflineIfNeeded();
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ẩn ActionBar nếu còn
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Fullscreen (ẩn status + navigation bar)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat c =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (c != null) {
            c.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            c.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }

    @Override protected void onStart() {
        super.onStart();
        NetworkMonitor.get(this).addListener(netListener);
        if (!NetworkMonitor.get(this).hasInternetNow()) {
            goOfflineIfNeeded();
        }
    }

    @Override protected void onStop() {
        super.onStop();
        NetworkMonitor.get(this).removeListener(netListener);
    }

    protected boolean requireOnline() {
        if (!NetworkMonitor.get(this).hasInternetNow()) {
            // Hiện dialog báo mất mạng
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Mất kết nối")
                    .setMessage("Vui lòng kiểm tra lại Internet để tiếp tục.")
                    .setPositiveButton("OK", null)
                    .show();
            return false;
        }
        return true;
    }
    /** Gọi trước khi thực hiện Firestore/network call. */


    private void goOfflineIfNeeded() {
        if (launchingOffline) return;
        launchingOffline = true;
        startActivity(new Intent(this, OfflineActivity.class));
        launchingOffline = false;
    }
}
