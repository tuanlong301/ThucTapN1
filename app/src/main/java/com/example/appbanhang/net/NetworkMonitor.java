package com.example.appbanhang.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NetworkMonitor {
    public interface Listener { void onInternetChanged(boolean available); }

    private static NetworkMonitor instance;

    private final Context ctx;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

    private volatile boolean hasInternet = true;
    private volatile boolean loopStarted = false;

    private NetworkMonitor(Context ctx) { this.ctx = ctx.getApplicationContext(); }

    public static synchronized NetworkMonitor get(Context ctx) {
        if (instance == null) instance = new NetworkMonitor(ctx);
        return instance;
    }

    public void addListener(Listener l) {
        if (l == null) return;
        listeners.addIfAbsent(l);
        l.onInternetChanged(hasInternet);   // báo ngay trạng thái hiện tại
        ensureLoop();
    }

    public void removeListener(Listener l) {
        if (l == null) return;
        listeners.remove(l);
    }

    public boolean hasInternetNow() { return hasInternet; }

    private synchronized void ensureLoop() {
        if (loopStarted) return;
        loopStarted = true;
        executor.submit(() -> {
            hasInternet = checkInternet();
            notifyAllListeners(hasInternet);
            while (true) {
                boolean ok = checkInternet();
                if (ok != hasInternet) {
                    hasInternet = ok;
                    notifyAllListeners(ok);
                }
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        });
    }

    private void notifyAllListeners(boolean ok) {
        for (Listener l : listeners) {
            try { l.onInternetChanged(ok); } catch (Throwable ignored) {}
        }
    }

    private boolean checkInternet() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network nw = cm.getActiveNetwork();
        if (nw == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(nw);
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false;

        // Kết nối socket ra 8.8.8.8:53 để xác thực “internet thật”
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("8.8.8.8", 53), 1500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
