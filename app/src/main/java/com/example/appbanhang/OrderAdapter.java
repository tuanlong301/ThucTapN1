package com.example.appbanhang;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.VH> {

    public enum Mode { PENDING, CONFIRMED }

    public interface OnAction {
        void onConfirm(String orderId);
        void onCancel(String orderId);
        void onPay(String orderId);
        void onPrint(String orderId);
    }

    private final List<Order> data = new ArrayList<>();
    private final Mode mode;
    private final OnAction cb;
    private String highlightId;

    public OrderAdapter(Mode mode, OnAction cb) {
        this.mode = mode; this.cb = cb;
    }

    public void submit(List<Order> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    public int indexOf(String id) {
        for (int i = 0; i < data.size(); i++) if (id.equals(data.get(i).id)) return i;
        return -1;
    }

    public void highlight(String id) { this.highlightId = id; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Order o = data.get(pos);

        String title = "Bàn số: " + (o.tableNumber != null ? o.tableNumber :
                (o.name != null ? o.name : ""));
        h.tvTableNumber.setText(title);

        String timeStr = (o.timestampStr != null && !o.timestampStr.isEmpty())
                ? o.timestampStr
                : (o.createdAt != null ? o.createdAt.toString() : "");
        h.tvTimestamp.setText("Đặt lúc: " + timeStr);

        h.tvOrderDetails.setText(parseItemsLine(o.items));

        if (o.notes != null && !o.notes.trim().isEmpty()) {
            h.tvNotes.setVisibility(View.VISIBLE);
            h.tvNotes.setText("Ghi chú: " + o.notes);
        } else {
            h.tvNotes.setVisibility(View.GONE);
        }

        h.tvTotal.setText("Tổng: " + (o.total != null ? o.total : "0") + " đ");

        if (mode == Mode.PENDING) {
            h.btnConfirm.setVisibility(View.VISIBLE);
            h.btnCancel.setVisibility(View.VISIBLE);
            h.btnPayment.setVisibility(View.GONE);
            h.btnPrint.setVisibility(View.GONE);

            h.btnConfirm.setOnClickListener(v -> cb.onConfirm(o.id));
            h.btnCancel.setOnClickListener(v -> cb.onCancel(o.id));
        } else { // CONFIRMED
            h.btnConfirm.setVisibility(View.GONE);
            h.btnCancel.setVisibility(View.GONE);
            h.btnPayment.setVisibility(View.VISIBLE);
            h.btnPrint.setVisibility(View.VISIBLE);

            h.btnPayment.setOnClickListener(v -> cb.onPay(o.id));
            h.btnPrint.setOnClickListener(v -> cb.onPrint(o.id));
        }

        h.itemView.setAlpha((highlightId != null && highlightId.equals(o.id)) ? 1f : 0.98f);
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTableNumber, tvTimestamp, tvOrderDetails, tvNotes, tvTotal;
        Button btnConfirm, btnCancel, btnPayment, btnPrint;
        VH(@NonNull View v) {
            super(v);
            tvTableNumber  = v.findViewById(R.id.tvTableNumber);
            tvTimestamp    = v.findViewById(R.id.tvTimestamp);
            tvOrderDetails = v.findViewById(R.id.tvOrderDetails);
            tvNotes        = v.findViewById(R.id.tvNotes);
            tvTotal        = v.findViewById(R.id.tvTotal);
            btnConfirm     = v.findViewById(R.id.btnConfirm);
            btnCancel      = v.findViewById(R.id.BtnCancel); // id của bạn là BtnCancel (B hoa)
            btnPayment     = v.findViewById(R.id.btnPayment);
            btnPrint       = v.findViewById(R.id.btnPrint);
        }
    }

    // ===== Model khớp dữ liệu Firestore =====
    public static class Order {
        public String id;
        public String userId;
        public String name;         // tên người đặt/ bàn
        public String items;        // bạn đang lưu dạng chuỗi
        public String total;
        public String notes;
        public String paymentMethod;
        public String status;
        public java.util.Date createdAt;
        public String timestampStr;
        public String tableNumber;  // nếu có
        public Order() {}
    }

    private String parseItemsLine(String raw) {
        if (raw == null || raw.isEmpty()) return "Món: (trống)";
        try {
            List<String> parts = new ArrayList<>();
            Matcher m = Pattern
                    .compile("\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"[^}]*\"qty\"\\s*:\\s*(\\d+)[^}]*\\}")
                    .matcher(raw);
            while (m.find()) parts.add(m.group(1) + " (" + m.group(2) + ")");
            return parts.isEmpty() ? raw : "Món: " + String.join(", ", parts);
        } catch (Exception e) {
            return raw;
        }
    }
}
