package com.example.appbanhang.feature.admin.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        public String sub;    // "Vào lúc 19:14 08/09" (có thể null)
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

        h.tvTableName.setText(r.name != null ? r.name : "(?)");

        String st = (r.status == null) ? "Không rõ" : r.status;

        // Status chip text + color
        String chipText;
        int chipBgRes;
        int chipTextColor;
        int stripColor;

        String sLower = st.toLowerCase();
        if (sLower.contains("đang sử dụng") || sLower.contains("đang phục vụ")) {
            chipText = "Đang phục vụ";
            chipBgRes = R.drawable.bg_chip_occupied;
            chipTextColor = ContextCompat.getColor(h.itemView.getContext(), R.color.danger);
            stripColor = ContextCompat.getColor(h.itemView.getContext(), R.color.table_occupied);
        } else if (sLower.contains("đã đặt trước")) {
            chipText = "Đã đặt trước";
            chipBgRes = R.drawable.bg_chip_reserved;
            chipTextColor = ContextCompat.getColor(h.itemView.getContext(), R.color.warning);
            stripColor = ContextCompat.getColor(h.itemView.getContext(), R.color.table_reserved);
        } else {
            chipText = "Trống";
            chipBgRes = R.drawable.bg_chip_available;
            chipTextColor = ContextCompat.getColor(h.itemView.getContext(), R.color.success);
            stripColor = ContextCompat.getColor(h.itemView.getContext(), R.color.table_available);
        }

        h.tvStatus.setText(chipText);
        h.tvStatus.setTextColor(chipTextColor);
        h.tvStatus.setBackgroundResource(chipBgRes);

        // Color strip on left
        if (h.viewStrip != null) {
            h.viewStrip.setBackgroundColor(stripColor);
        }

        // Sub text
        if (h.tvSub != null) {
            if (r.sub != null && !r.sub.trim().isEmpty()) {
                h.tvSub.setText(r.sub.trim());
                h.tvSub.setVisibility(View.VISIBLE);
            } else {
                h.tvSub.setText(null);
                h.tvSub.setVisibility(View.GONE);
            }
        }

        // Click
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(r);
        });
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTableName, tvStatus, tvSub;
        View viewStrip;
        VH(@NonNull View v) {
            super(v);
            tvTableName = v.findViewById(R.id.tvTableName);
            tvStatus    = v.findViewById(R.id.tvTableStatus);
            tvSub       = v.findViewById(R.id.tvTableSub);
            viewStrip   = v.findViewById(R.id.viewStatusStrip);
        }
    }
}
