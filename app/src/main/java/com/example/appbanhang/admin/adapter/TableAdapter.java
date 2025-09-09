package com.example.appbanhang.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;

import java.util.ArrayList;
import java.util.List;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.VH> {

    public interface OnTableClick {
        void onClick(TableRow row);
    }

    public static class TableRow {
        public String userId;
        public String name;   // "Bàn 1"

        public String status;
        // ví dụ: "Vào lúc 19:14 08/09" (có thể null)
        public String sub;
    }

    private final List<TableRow> data = new ArrayList<>();
    private OnTableClick onClick;

    public void setOnTableClick(OnTableClick cb) { this.onClick = cb; }

    public void submit(List<TableRow> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final TableRow r = data.get(pos);

        // Tên bàn
        h.tvTableName.setText(r.name != null ? r.name : "(?)");

        // Build dòng trạng thái 1 dòng
        String st = (r.status == null) ? "Không rõ" : r.status;
        String line = "Trạng thái: " + st;
        if (r.sub != null && !r.sub.trim().isEmpty()) {
            line += " • " + r.sub.trim();
        }
        h.tvStatus.setText(line);

        // Dọn/ẩn dòng phụ để không bị “bóng ma”
        if (h.tvSub != null) {
            h.tvSub.setText(null);
            h.tvSub.setVisibility(View.GONE);
        }

        // Màu theo trạng thái (map tên mới)
        h.tvStatus.setTextColor(colorForStatus(st));

        // Click
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(r);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    private int colorForStatus(String status) {
        if (status == null) return 0xFF6B7280; // xám

        String s = status.toLowerCase();

        if (s.contains("đang sử dụng") || s.contains("đang phục vụ")) {
            return 0xFF16A34A; // xanh lá
        }

        if (s.contains("đã đặt trước")) {
            return 0xFF3B82F6; // xanh dương
        }

        return 0xFF6B7280; // xám
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTableName, tvStatus, tvSub;
        VH(@NonNull View v) {
            super(v);
            tvTableName = v.findViewById(R.id.tvTableName);
            tvStatus    = v.findViewById(R.id.tvTableStatus);
            // Có thể không dùng nữa, nhưng vẫn bind để dọn state khi view tái sử dụng
            tvSub       = v.findViewById(R.id.tvTableSub);
        }
    }
}
