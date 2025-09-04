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

public class ConfirmedOrdersFragment extends Fragment {

    public static ConfirmedOrdersFragment newInstance() {
        ConfirmedOrdersFragment f = new ConfirmedOrdersFragment();
        f.setArguments(new Bundle());
        return f;
    }

    private RecyclerView rv;
    private OrderAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration reg;
    @Nullable private String highlightId;

    @SuppressLint("MissingInflatedId")
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        View v = inflater.inflate(R.layout.fragment_orders, container, false);

        rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(OrderAdapter.Mode.CONFIRMED, new OrderAdapter.OnAction() {
            @Override public void onConfirm(String orderId) { /* not used in confirmed */ }
            @Override public void onCancel(String orderId)  { /* tuỳ bạn có cho huỷ sau xác nhận không */ }
            @Override public void onPay(String orderId)     { markPaid(orderId); }
            @Override public void onPrint(String orderId)   { toast("In hoá đơn: " + orderId); /* hook in/ share PDF */ }
        });
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        Bundle args = getArguments();
        if (args != null) highlightId = args.getString("highlight_id");

        getParentFragmentManager().setFragmentResultListener(
                "force_refresh", this, (k, b) -> { rv.scrollToPosition(0); adapter.highlight(null); }
        );
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        reg = db.collection("orders")
                .whereEqualTo("status", "confirmed")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { toast("Lỗi tải đơn: " + e.getMessage()); e.printStackTrace(); return; }
                    if (snap == null) return;

                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Order o = d.toObject(Order.class);
                        if (o == null) o = new Order();
                        o.id = d.getId();
                        o.createdAt = d.getDate("timestamp");
                        list.add(o);
                    }
                    adapter.submit(list);

                    if (!TextUtils.isEmpty(highlightId)) {
                        int idx = adapter.indexOf(highlightId);
                        if (idx >= 0) { rv.scrollToPosition(idx); adapter.highlight(highlightId); highlightId = null; }
                    }
                });
    }

    @Override public void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void markPaid(String orderId) {
        db.collection("orders").document(orderId)
                .update(
                        "status", "paid",
                        "paidAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> toast("Đã thanh toán"))
                .addOnFailureListener(e -> toast("Lỗi thanh toán: " + e.getMessage()));
    }

    private void toast(String m) { if (getContext()!=null) Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show(); }
}
