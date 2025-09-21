package com.example.appbanhang.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public final class OrderBus {
    private static List<Order> orders = new ArrayList<>();

    private OrderBus() {}

    public static synchronized void setOrders(List<Order> list) {
        orders = (list == null) ? new ArrayList<>() : new ArrayList<>(list);
    }

    public static synchronized List<Order> getOrders() {
        return Collections.unmodifiableList(orders);
    }
}
