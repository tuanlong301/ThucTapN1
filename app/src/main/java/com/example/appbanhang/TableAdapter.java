package com.example.appbanhang;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class TableAdapter extends RecyclerView.Adapter<TableAdapter.VH> {

    public interface OnTableClick {
        void onClick(TableRow row);
    }

    public static class TableRow {
        public String userId;
        public String name;      // "Bàn 1"
        public String status;    // "Trống" | "Đang phục vụ" | "Đang gọi NV" | "Đặt trước"
        public String sub;       // "Đặt lúc 14:57" hoặc "Gọi lúc 14:10"...
    }

    private final List<TableRow> data = new ArrayList<>();
    private OnTableClick onClick;

    public void setOnTableClick(OnTableClick cb) { this.onClick = cb; }

    public void submit(List<TableRow> list) {
        data.clear();
        data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_table, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        final TableRow r = data.get(pos);

        h.tvTableName.setText(r.name != null ? r.name : "(?)");
        h.tvStatus.setText("Trạng thái: " + (r.status != null ? r.status : "Không rõ"));
        h.tvSub.setText(r.sub != null ? r.sub : "");

        // Màu chữ theo trạng thái
        int c;
        if ("Đang phục vụ".equalsIgnoreCase(r.status)) c = 0xFF16A34A;         // xanh lá
        else if ("Đang gọi NV".equalsIgnoreCase(r.status)) c = 0xFFEAB308;    // vàng
        else if ("Đặt trước".equalsIgnoreCase(r.status)) c = 0xFF3B82F6;      // xanh dương
        else c = 0xFF6B7280;                                                  // xám
        h.tvStatus.setTextColor(c);

        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(r);
        });
    }

    @Override public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTableName, tvStatus, tvSub;
        VH(@NonNull View v) {
            super(v);
            tvTableName = v.findViewById(R.id.tvTableName);
            tvStatus    = v.findViewById(R.id.tvTableStatus);
            tvSub       = v.findViewById(R.id.tvTableSub);
        }
    }
}
