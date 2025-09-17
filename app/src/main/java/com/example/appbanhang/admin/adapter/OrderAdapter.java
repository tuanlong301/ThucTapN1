package com.example.appbanhang.admin.adapter;

import com.example.appbanhang.R;
import com.example.appbanhang.model.Order;
import com.example.appbanhang.utils.InvoiceUtilss;

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
        this.mode = mode;
        this.cb = cb;
        setHasStableIds(true);
    }

    public void submit(List<Order> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    public int indexOf(String id) {
        if (id == null) return -1;
        for (int i = 0; i < data.size(); i++) {
            if (id.equals(data.get(i).id)) return i;
        }
        return -1;
    }

    public void highlight(String id) {
        this.highlightId = id;
        notifyDataSetChanged();
    }

    @Override public long getItemId(int position) {
        Order o = data.get(position);
        return (o != null && o.id != null) ? o.id.hashCode() : position;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Order o = data.get(pos);

        // Header
        String title = "Bàn số: " + (o.name != null ? o.name : "");
        h.tvTableNumber.setText(title);

        String timeStr = (o.timestampStr != null && !o.timestampStr.isEmpty())
                ? o.timestampStr
                : (o.createdAt != null ? o.createdAt.toString() : "");
        h.tvTimestamp.setText("Đặt lúc: " + timeStr);

        // Body
        h.tvOrderDetails.setText(parseItemsLine(o.items));

        if (o.notes != null && !o.notes.trim().isEmpty()) {
            h.tvNotes.setVisibility(View.VISIBLE);
            h.tvNotes.setText("Ghi chú: " + o.notes);
        } else {
            h.tvNotes.setVisibility(View.GONE);
        }

        // Tổng tiền (dùng util của bạn – đã có overload String/Number)
        h.tvTotal.setText("Tổng: " + InvoiceUtilss.formatVnd(o.total));

        // Payment line (phương thức + trạng thái tiền)
        if (h.tvPayment != null) {
            String pm = (o.paymentMethod == null ? "?" : o.paymentMethod);
            String pst;
            if ("paid".equalsIgnoreCase(o.paymentStatus)) pst = "Đã thanh toán";
            else if ("awaiting_transfer".equalsIgnoreCase(o.paymentStatus)) pst = "Chờ chuyển khoản";
            else if ("unpaid".equalsIgnoreCase(o.paymentStatus)) pst = "Chưa thu";
            else pst = (o.paymentStatus == null ? "Chưa thanh toán" : o.paymentStatus);
            h.tvPayment.setText(pm + " · " + pst);
        }

        // Buttons
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
            h.btnPrint.setVisibility(View.VISIBLE);

            boolean isPaid = "paid".equalsIgnoreCase(o.paymentStatus);

            // In HÓA ĐƠN: chỉ khi đã paid; nếu chưa, đẩy sang cập nhật thanh toán
            h.btnPrint.setEnabled(isPaid);
            h.btnPrint.setAlpha(isPaid ? 1f : 0.5f);
            h.btnPrint.setOnClickListener(v -> {
                if (isPaid) {
                    cb.onPrint(o.id);   // fragment in + set printed=true
                } else {
                    cb.onPay(o.id);     // yêu cầu cập nhật thanh toán trước
                }
            });

            if (!isPaid) {
                h.btnPayment.setVisibility(View.VISIBLE);
                h.btnPayment.setText("Cập nhật thanh toán");
                h.btnPayment.setOnClickListener(v -> cb.onPay(o.id));
            } else {
                h.btnPayment.setVisibility(View.GONE);
            }
        }

        h.itemView.setAlpha((highlightId != null && highlightId.equals(o.id)) ? 1f : 0.98f);
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTableNumber, tvTimestamp, tvOrderDetails, tvNotes, tvTotal;
        TextView tvPayment;
        Button btnConfirm, btnCancel, btnPayment, btnPrint;

        VH(@NonNull View v) {
            super(v);
            tvTableNumber  = v.findViewById(R.id.tvTableNumber);
            tvTimestamp    = v.findViewById(R.id.tvTimestamp);
            tvOrderDetails = v.findViewById(R.id.tvOrderDetails);
            tvNotes        = v.findViewById(R.id.tvNotes);
            tvTotal        = v.findViewById(R.id.tvTotal);
            tvPayment      = v.findViewById(R.id.tvPayment);

            btnConfirm     = v.findViewById(R.id.btnConfirm);
            btnCancel      = v.findViewById(R.id.BtnCancel);
            btnPayment     = v.findViewById(R.id.btnPayment);
            btnPrint       = v.findViewById(R.id.btnPrint);
        }
    }

    // Parse chuỗi items -> "Món: A (2), B (1)"
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
