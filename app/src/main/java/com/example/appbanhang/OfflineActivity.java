package com.example.appbanhang;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class OfflineActivity extends AppCompatActivity {

    private ConnectivityManager cm;

    private final ConnectivityManager.NetworkCallback cb = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            // Có mạng lại thì thoát màn Offline để quay về chỗ cũ
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    finish();
                }
            });
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Button btnRetry = findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> {
            if (isOnline()) {
                finish();
            } else {
                Toast.makeText(this, "Chưa có mạng, thử lại sau.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        register();
        if (isOnline()) {
            finish();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregister();
    }

    private void register() {
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

    private void unregister() {
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
}
