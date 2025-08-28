package com.example.appbanhang;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class PendingOrdersFragment extends Fragment {

    private RecyclerView rvOrders;
    private OrderAdapter orderAdapter;
    private List<Order> orders = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_orders, container, false);

        // Khởi tạo RecyclerView
        rvOrders = view.findViewById(R.id.rvOrders); // Giả định ID trong layout
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        orderAdapter = new OrderAdapter(getContext(), true); // true cho tab pending
        rvOrders.setAdapter(orderAdapter);

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();
        loadPendingOrders();

        return view;
    }

    private void loadPendingOrders() {
        db.collection("orders")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(getContext(), "Lỗi tải đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snap != null) {
                        orders.clear();
                        for (DocumentSnapshot doc : snap.getDocuments()) { // Thay var bằng DocumentSnapshot
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                order.setId(doc.getId()); // Gán ID từ document
                                orders.add(order);
                            }
                        }
                        orderAdapter.setOrders(orders);
                    }
                });
    }
}