package com.example.appbanhang;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class PendingOrdersFragment extends Fragment {

    public static PendingOrdersFragment newInstance() {
        PendingOrdersFragment f = new PendingOrdersFragment();
        f.setArguments(new Bundle());
        return f;
    }

    private RecyclerView rv;
    private OrderAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration reg;
    @Nullable private String highlightId;

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_orders, container, false);

        rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(OrderAdapter.Mode.PENDING, new OrderAdapter.OnAction() {
            @Override public void onConfirm(String orderId) { updateStatus(orderId, "confirmed"); }
            @Override public void onCancel(String orderId)  { updateStatus(orderId, "canceled"); }
            @Override public void onPay(String orderId)     { /* not dùng ở pending */ }
            @Override public void onPrint(String orderId)   { /* not dùng ở pending */ }
        });
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // nhận highlight_id nếu có
        Bundle args = getArguments();
        if (args != null) highlightId = args.getString("highlight_id");

        // nhận sự kiện refresh từ AdminMenu
        getParentFragmentManager().setFragmentResultListener(
                "force_refresh", this, (key, b) -> {
                    rv.scrollToPosition(0);
                    adapter.highlight(null);
                }
        );

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Query tất cả orders có status = pending, sắp xếp theo timestamp
        reg = db.collection("orders")
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        if (getContext()!=null) {
                            Toast.makeText(getContext(), "Lỗi tải đơn: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        e.printStackTrace();
                        return;
                    }
                    if (snap == null) return;

                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Order o = d.toObject(Order.class);
                        if (o == null) o = new Order();
                        o.id = d.getId();

                        // map timestamp Firestore -> createdAt nếu cần
                        o.createdAt = d.getDate("timestamp");

                        list.add(o);
                    }

                    adapter.submit(list);

                    if (!TextUtils.isEmpty(highlightId)) {
                        int idx = adapter.indexOf(highlightId);
                        if (idx >= 0) {
                            rv.scrollToPosition(idx);
                            adapter.highlight(highlightId);
                            highlightId = null;
                        }
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void updateStatus(String orderId, String newStatus) {
        db.collection("orders").document(orderId)
                .update(
                        "status", newStatus,
                        "updatedAt", FieldValue.serverTimestamp(),
                        newStatus.equals("confirmed") ? "confirmedAt" : "updatedAt",
                        FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> toast("Cập nhật: " + newStatus))
                .addOnFailureListener(e -> toast("Lỗi: " + e.getMessage()));
    }


    private void toast(String m) {
        if (getContext() != null) {
            Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
        }
    }
}
