package com.example.appbanhang.common.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Tiện ích hiển thị dialog chuẩn với giao diện đẹp và
 * thông báo tiếng Việt chuyên nghiệp.
 */
public class Dialogs {

    // ==================== Quick dialogs ====================

    /** Dialog thông báo mất kết nối */
    public static void noInternet(@NonNull Context ctx) {
        info(ctx, "📡 Mất kết nối",
                "Không có internet.\nVui lòng kiểm tra kết nối và thử lại.");
    }

    /** Dialog thông báo lỗi chung */
    public static void error(@NonNull Context ctx, @NonNull String message) {
        new AlertDialog.Builder(ctx)
                .setTitle("❌ Có lỗi xảy ra")
                .setMessage(message)
                .setPositiveButton("Đóng", null)
                .show();
    }

    /** Dialog thông tin */
    public static void info(@NonNull Context ctx, @NonNull String title, @NonNull String message) {
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /** Dialog thành công */
    public static void success(@NonNull Context ctx, @NonNull String message,
                               @Nullable DialogInterface.OnClickListener onOk) {
        new AlertDialog.Builder(ctx)
                .setTitle("✅ Thành công")
                .setMessage(message)
                .setPositiveButton("OK", onOk)
                .setCancelable(false)
                .show();
    }

    /** Dialog xác nhận (Có / Hủy) */
    public static void confirm(@NonNull Context ctx, @NonNull String title,
                               @NonNull String message,
                               @NonNull DialogInterface.OnClickListener onConfirm) {
        new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xác nhận", onConfirm)
                .show();
    }

    /** Dialog xác nhận hành động nguy hiểm (Xóa, Hủy đơn...) */
    public static void confirmDanger(@NonNull Context ctx, @NonNull String title,
                                     @NonNull String message,
                                     @NonNull String positiveText,
                                     @NonNull DialogInterface.OnClickListener onConfirm) {
        new AlertDialog.Builder(ctx)
                .setTitle("⚠️ " + title)
                .setMessage(message)
                .setNegativeButton("Quay lại", null)
                .setPositiveButton(positiveText, onConfirm)
                .show();
    }
}
