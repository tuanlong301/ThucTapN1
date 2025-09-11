package com.example.appbanhang;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public abstract class BaseActivity extends AppCompatActivity {

    private ConnectivityManager cm;
    private boolean launchingOffline = false;

    private final ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onLost(Network network) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    goOfflineIfNeeded();
                }
            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        // Ẩn ActionBar nếu còn
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Ẩn thanh status + navigation bar
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerNet();
        if (!isOnline()) {
            goOfflineIfNeeded();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterNet();
    }

    private void registerNet() {
        if (cm == null) return;
        if (Build.VERSION.SDK_INT >= 24) {
            cm.registerDefaultNetworkCallback(cb);
        } else {
            NetworkRequest req = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            cm.registerNetworkCallback(req, cb);
        }
    }

    private void unregisterNet() {
        if (cm == null) return;
        try { cm.unregisterNetworkCallback(cb); } catch (Exception ignore) {}
    }

    private boolean isOnline() {
        if (cm == null) return false;
        Network nw = cm.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(nw);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private void goOfflineIfNeeded() {
        if (launchingOffline) return;


        launchingOffline = true;
        Intent i = new Intent(this, OfflineActivity.class);
        // Không phá back stack; khi có mạng OfflineActivity sẽ finish() và quay về
        startActivity(i);
        launchingOffline = false;
    }
}
