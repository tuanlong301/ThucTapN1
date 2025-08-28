package com.example.appbanhang;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class PendingOrdersFragment extends Fragment {

    public static PendingOrdersFragment newInstance() {
        PendingOrdersFragment f = new PendingOrdersFragment();
        f.setArguments(new Bundle()); // sẵn để đặt highlight_id
        return f;
    }

    private RecyclerView rv;
    private OrderAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration reg;
    @Nullable private String highlightId;

    @SuppressLint("MissingInflatedId")
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_orders, parent, false);
        rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(OrderAdapter.Mode.PENDING, new OrderAdapter.OnAction() {
            @Override public void onConfirm(String orderId) { updateStatus(orderId, "confirmed"); }
            @Override public void onCancel(String orderId)  { updateStatus(orderId, "canceled"); }
            @Override public void onPay(String orderId)     { /* not used here */ }
            @Override public void onPrint(String orderId)   { /* not used here */ }
        });
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // nhận highlight_id từ AdminAdapter
        Bundle args = getArguments();
        if (args != null) highlightId = args.getString("highlight_id");

        // nhận sự kiện Refresh từ AdminMenu (tuỳ chọn)
        getParentFragmentManager().setFragmentResultListener(
                "force_refresh", this, (k, b) -> {
                    rv.scrollToPosition(0);
                    adapter.highlight(null);
                }
        );

        return v;
    }

    @Override public void onStart() {
        super.onStart();
        reg = db.collection("orders")
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    List<OrderAdapter.Order> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        OrderAdapter.Order o = new OrderAdapter.Order();
                        o.id = d.getId();
                        o.name = d.getString("name");
                        o.items = d.getString("items");
                        o.total = d.getString("total");
                        o.notes = d.getString("notes");
                        o.paymentMethod = d.getString("paymentMethod");
                        o.status = d.getString("status");
                        o.createdAt = d.getDate("createdAt");
                        o.timestampStr = d.getString("timestampStr");
                        o.tableNumber = d.getString("tableNumber");
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

    @Override public void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void updateStatus(String orderId, String newStatus) {
        db.collection("orders").document(orderId)
                .update("status", newStatus, "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> toast("Cập nhật: " + newStatus))
                .addOnFailureListener(e -> toast("Lỗi: " + e.getMessage()));
    }

    private void toast(String m) { if (getContext()!=null) Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show(); }
}
