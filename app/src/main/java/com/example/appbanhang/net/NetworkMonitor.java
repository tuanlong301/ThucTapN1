package com.example.appbanhang.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkMonitor {
    private static NetworkMonitor instance;
    private final Context ctx;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean hasInternet = true;
    private Listener listener;

    public interface Listener {
        void onInternetChanged(boolean available);
    }

    private NetworkMonitor(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public static synchronized NetworkMonitor get(Context ctx) {
        if (instance == null) instance = new NetworkMonitor(ctx);
        return instance;
    }

    public void setListener(Listener l) {
        this.listener = l;
        startCheckLoop();
    }

    private void startCheckLoop() {
        executor.submit(() -> {
            while (true) {
                boolean ok = checkInternet();
                if (ok != hasInternet) {
                    hasInternet = ok;
                    if (listener != null) listener.onInternetChanged(ok);
                }
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        });
    }

    private boolean checkInternet() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = cm.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(nw);
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false;

        // Test thật bằng socket (Google DNS 8.8.8.8:53)
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean hasInternetNow() {
        return hasInternet;
    }
}
