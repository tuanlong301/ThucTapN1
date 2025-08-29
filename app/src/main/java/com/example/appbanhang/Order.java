package com.example.appbanhang;

import java.util.Date;

public class Order {
    public String id;
    public String userId;
    public String name;          // số bàn = name từ acc
    public String items;         // danh sách món (chuỗi json)
    public String total;
    public String notes;
    public String paymentMethod;
    public String status;        // pending / confirmed / paid / canceled
    public Date createdAt;       // dùng để orderBy trong Firestore
    public String timestampStr;  // show cho người dùng

    public Order() {}
}
