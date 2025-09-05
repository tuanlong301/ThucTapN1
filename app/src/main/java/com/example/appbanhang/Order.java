package com.example.appbanhang;

import java.util.Date;

public class Order {
    public String id;

    // Đơn hàng
    public String userId;        // uid người đặt
    public String name;          // tên bàn (name trong acc)
    public String items;         // chuỗi JSON món
    public String total;         // tổng tiền dạng chuỗi hiển thị
    public String notes;
    public String paymentMethod; // "Tiền mặt" | "Chuyển khoản"

    // Trạng thái
    public String status;        // pending | confirmed | fulfilled | canceled

    // Thanh toán
    public String paymentStatus; // unpaid | awaiting_transfer | paid | refunded
    public String transactionRef; // mã giao dịch (CK)

    // Thời gian
    public Date createdAt;       // (map từ "timestamp" Firestore)
    public String timestampStr;  // hiển thị
    public Date confirmedAt;
    public Date paidAt;
    public Date updatedAt;

    public Order() {}
}
