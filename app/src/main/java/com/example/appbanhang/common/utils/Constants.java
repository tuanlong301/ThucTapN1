package com.example.appbanhang.common.utils;

/**
 * Hằng số toàn app – tránh magic strings.
 */
public final class Constants {

    private Constants() {}

    // ========== Roles ==========
    public static final String ROLE_USER  = "user";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_OWNER = "king";

    public static boolean isAdmin(String role) {
        return ROLE_ADMIN.equalsIgnoreCase(role) || ROLE_OWNER.equalsIgnoreCase(role);
    }

    // ========== Order Status ==========
    public static final String STATUS_PENDING   = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_CANCELED  = "canceled";

    // ========== Payment Status ==========
    public static final String PAY_UNPAID            = "unpaid";
    public static final String PAY_AWAITING_TRANSFER = "awaiting_transfer";
    public static final String PAY_PAID              = "paid";

    // ========== Table Status ==========
    public static final String TABLE_AVAILABLE = "available";
    public static final String TABLE_OCCUPIED  = "occupied";
    public static final String TABLE_RESERVED  = "reserved";

    // ========== Product Categories ==========
    public static final String CAT_BESTSELLER = "bestseller";
    public static final String CAT_KM         = "km";
    public static final String CAT_CM         = "cm";
    public static final String CAT_NUOC       = "nuoc";

    // ========== Staff Call ==========
    public static final String CALL_QUEUED  = "queued";
    public static final String CALL_HANDLED = "handled";

    // ========== Collections ==========
    public static final String COLL_ACC         = "acc";
    public static final String COLL_FOOD        = "food_001";
    public static final String COLL_CARTS       = "carts";
    public static final String COLL_ORDERS      = "orders";
    public static final String COLL_TABLE_STATES = "table_states";
    public static final String COLL_STAFF_CALLS = "staff_calls";

    // ========== Limits ==========
    public static final long MAX_QTY            = 9;
    public static final long CALL_SPAM_MILLIS   = 2 * 60 * 1000; // 2 phút

    // ========== Payment Methods ==========
    public static final String PM_CASH     = "Tiền mặt";
    public static final String PM_TRANSFER = "Chuyển khoản";
}
