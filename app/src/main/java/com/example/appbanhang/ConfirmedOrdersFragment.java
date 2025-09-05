package com.example.appbanhang;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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

        db = FirebaseFirestore.getInstance();

        adapter = new OrderAdapter(OrderAdapter.Mode.CONFIRMED, new OrderAdapter.OnAction() {
            @Override public void onConfirm(String orderId) { /* not used here */ }
            @Override public void onCancel(String orderId)  { /* tuỳ policy nếu muốn huỷ ở tab này */ }

            @Override public void onPay(String orderId) {
                // Đọc doc để kiểm tra tình trạng hiện tại
                db.collection("orders").document(orderId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc == null || !doc.exists()) { toast("Không tìm thấy đơn"); return; }
                            String pStatus = doc.getString("paymentStatus");
                            if ("paid".equalsIgnoreCase(pStatus)) {
                                toast("Đơn đã thanh toán");
                                return;
                            }
                            showUpdatePaymentDialog(orderId);
                        })
                        .addOnFailureListener(e -> toast("Lỗi đọc đơn: " + e.getMessage()));
            }

            @Override public void onPrint(String orderId) {
                toast("In hoá đơn " + orderId + " (đang phát triển)");
            }
        });
        rv.setAdapter(adapter);

        // optional highlight
        Bundle args = getArguments();
        if (args != null) highlightId = args.getString("highlight_id");

        // optional refresh từ AdminMenu
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
                        // map timestamp -> createdAt để hiển thị fallback
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

    // ---------- Dialog cập nhật thanh toán ----------
    private void showUpdatePaymentDialog(String orderId) {
        // layout vertical
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        // radio group
        RadioGroup rg = new RadioGroup(requireContext());
        RadioButton rbPaid = new RadioButton(requireContext());
        rbPaid.setText("Đã thanh toán");
        RadioButton rbCancel = new RadioButton(requireContext());
        rbCancel.setText("Hủy đơn");
        rg.addView(rbPaid);
        rg.addView(rbCancel);

        // lý do huỷ (ẩn mặc định)
        final EditText edtReason = new EditText(requireContext());
        edtReason.setHint("Lý do hủy (bắt buộc nếu chọn Hủy đơn)");
        edtReason.setVisibility(View.GONE);

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            edtReason.setVisibility(checkedId == rbCancel.getId() ? View.VISIBLE : View.GONE);
        });

        root.addView(rg);
        root.addView(edtReason);

        new AlertDialog.Builder(requireContext())
                .setTitle("Cập nhật thanh toán")
                .setView(root)
                .setPositiveButton("Xác nhận", (d, w) -> {
                    if (rg.getCheckedRadioButtonId() == rbPaid.getId()) {
                        markPaid(orderId);
                    } else if (rg.getCheckedRadioButtonId() == rbCancel.getId()) {
                        String reason = edtReason.getText() == null ? "" : edtReason.getText().toString().trim();
                        if (reason.isEmpty()) {
                            toast("Vui lòng nhập lý do hủy"); return;
                        }
                        cancelOrder(orderId, reason);
                    } else {
                        toast("Vui lòng chọn hành động");
                    }
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void markPaid(String orderId) {
        db.collection("orders").document(orderId)
                .update(
                        "paymentStatus", "paid",
                        "paidAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> toast("Đã xác nhận thanh toán"))
                .addOnFailureListener(e -> toast("Lỗi: " + e.getMessage()));
    }

    private void cancelOrder(String orderId, String reason) {
        db.collection("orders").document(orderId)
                .update(
                        "status", "canceled",
                        "cancelReason", reason,
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> toast("Đã hủy đơn"))
                .addOnFailureListener(e -> toast("Lỗi: " + e.getMessage()));
    }

    private void toast(String m) { if (getContext()!=null) Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show(); }
}
