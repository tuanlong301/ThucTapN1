package com.example.appbanhang.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class InvoiceUtilss {

    private static final Locale VI = new Locale("vi", "VN");
    private static final NumberFormat VND = NumberFormat.getInstance(VI);
    static { VND.setMaximumFractionDigits(0); VND.setMinimumFractionDigits(0); }

    /** Nhận long (VND) -> "10.502.000 đ" (không scientific) */
    public static String formatVnd(long amount) {
        return VND.format(amount) + " đ";
    }

    /** Nhận BigDecimal -> "10.502.000 đ" (an toàn cho số lớn) */
    public static String formatVnd(BigDecimal amount) {
        if (amount == null) return "0 đ";
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return VND.format(rounded) + " đ";
    }

    /** Nếu bạn vẫn nhận từ double, chuyển về BigDecimal an toàn. */
    public static String formatVnd(double amount) {
        return formatVnd(BigDecimal.valueOf(amount));
    }

    public static String formatVnd(String amount) {
        if (amount == null || amount.trim().isEmpty()) return "0 đ";
        try {
            BigDecimal bd = new BigDecimal(amount);
            BigDecimal rounded = bd.setScale(0, RoundingMode.HALF_UP);
            return VND.format(rounded) + " đ"; // ✅ luôn có " đ"
        } catch (NumberFormatException e) {
            return "0 đ"; // ✅ fallback cũng có " đ"
        }
    }

}
