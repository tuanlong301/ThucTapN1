package com.example.appbanhang.admin;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.*;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.admin.adapter.OrderAdapter;
import com.example.appbanhang.R;
import com.example.appbanhang.model.Order;
import com.example.appbanhang.utils.InvoiceUtilss;
import com.example.appbanhang.utils.InvoiceUtils;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class ConfirmedOrdersFragment extends Fragment {

    public static ConfirmedOrdersFragment newInstance() {
        ConfirmedOrdersFragment f = new ConfirmedOrdersFragment();
        f.setArguments(new Bundle());
        return f;
    }
    private TextView tvEmpty;
    private RecyclerView rv;
    private OrderAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration reg;
    @Nullable private String highlightId;

    @SuppressLint("MissingInflatedId")
    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle s) {
        View v = inflater.inflate(R.layout.fragment_orders, container, false);

        tvEmpty = v.findViewById(R.id.tvEmpty);
        rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));


        db = FirebaseFirestore.getInstance();

        adapter = new OrderAdapter(OrderAdapter.Mode.CONFIRMED, new OrderAdapter.OnAction() {
            @Override public void onConfirm(String orderId) { /* not used here */ }
            @Override public void onCancel(String orderId)  { /* optional */ }

            @Override public void onPay(String orderId) {
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
                // Kiểm tra paid trên server rồi mới cho in
                db.collection("orders").document(orderId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc == null || !doc.exists()) { toast("Không tìm thấy đơn"); return; }

                            String pStatus = doc.getString("paymentStatus");
                            if (pStatus == null || !pStatus.equalsIgnoreCase("paid")) {
                                new AlertDialog.Builder(requireContext())
                                        .setTitle("Chưa thanh toán")
                                        .setMessage("Vui lòng xác nhận 'Đã thanh toán' trước khi in hóa đơn.")
                                        .setPositiveButton("Cập nhật", (d,w) -> onPay(orderId))
                                        .setNegativeButton("Hủy", null)
                                        .show();
                                return;
                            }

                            // === ĐÃ THANH TOÁN → In PDF + set printed ===
                            String invoiceId = doc.getId();
                            String tableName = doc.getString("name");
                            String itemsRaw  = doc.getString("items");
                            String details   = parseItemsLine(itemsRaw);
                            String totalStr  = formatTotal(doc.get("total"));

                            // Gọi util in PDF của bạn (đã tồn tại trong project)
                            InvoiceUtils.exportInvoiceToPdf(
                                    requireContext(), invoiceId, tableName, details, totalStr
                            );

                            db.collection("orders").document(orderId)
                                    .update("printed", true,
                                            "printedAt", FieldValue.serverTimestamp(),
                                            "updatedAt", FieldValue.serverTimestamp())
                                    .addOnSuccessListener(vv -> toast("Đã in "))
                                    .addOnFailureListener(e -> toast("In xong nhưng chưa cập nhật printed: " + e.getMessage()));
                        })
                        .addOnFailureListener(e -> toast("Lỗi đọc đơn: " + e.getMessage()));
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
                        // ẨN đơn đã in
                        Boolean printed = d.getBoolean("printed");
                        if (printed != null && printed) continue;

                        Order o = d.toObject(Order.class);
                        if (o == null) o = new Order();
                        o.id = d.getId();
                        o.createdAt = d.getDate("timestamp");
                        list.add(o);
                    }
                    adapter.submit(list);
                    boolean empty = list == null || list.isEmpty();
                    tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rv.setVisibility(empty ? View.GONE : View.VISIBLE);


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
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        RadioGroup rg = new RadioGroup(requireContext());
        RadioButton rbPaid = new RadioButton(requireContext());
        rbPaid.setText("Đã thanh toán");
        RadioButton rbCancel = new RadioButton(requireContext());
        rbCancel.setText("Hủy đơn");
        rg.addView(rbPaid);
        rg.addView(rbCancel);

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
                        if (reason.isEmpty()) { toast("Vui lòng nhập lý do hủy"); return; }
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
                .update("paymentStatus", "paid",
                        "paidAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> toast("Đã xác nhận thanh toán"))
                .addOnFailureListener(e -> toast("Lỗi: " + e.getMessage()));
    }

    private void cancelOrder(String orderId, String reason) {
        db.collection("orders").document(orderId)
                .update("status", "canceled",
                        "cancelReason", reason,
                        "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> toast("Đã hủy đơn"))
                .addOnFailureListener(e -> toast("Lỗi: " + e.getMessage()));
    }

    private void toast(String m) { if (getContext()!=null) Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show(); }

    // ---- Helpers ----
    private String parseItemsLine(String raw) {
        if (raw == null || raw.isEmpty()) return "Món: (trống)";
        try {
            List<String> parts = new ArrayList<>();
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"qty\"\\s*:\\s*(\\d+)[^}]*\\}")
                    .matcher(raw);
            while (m.find()) parts.add(m.group(1) + " (" + m.group(2) + ")");
            return parts.isEmpty() ? raw : "Món: " + String.join(", ", parts);
        } catch (Exception e) {
            return raw;
        }
    }

    private String formatTotal(Object totalField) {
        try {
            if (totalField == null) return "0 đ";
            if (totalField instanceof Number) {
                long v = ((Number) totalField).longValue();
                return InvoiceUtilss.formatVnd(v);
            }
            return InvoiceUtilss.formatVnd(totalField.toString());
        } catch (Exception ignore) {
            return "0 đ";
        }
    }
}
