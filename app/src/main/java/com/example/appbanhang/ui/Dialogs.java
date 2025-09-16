package com.example.appbanhang.ui;

import android.app.AlertDialog;
import android.content.Context;

public class Dialogs {
    public static void noInternet(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("Mất kết nối")
                .setMessage("Không có internet. Vui lòng kiểm tra kết nối và thử lại.")
                .setPositiveButton("OK", null)
                .show();
    }
}
