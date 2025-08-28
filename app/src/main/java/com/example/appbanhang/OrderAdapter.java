package com.example.appbanhang;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Map;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private Context context;
    private List<Order> orders;
    private boolean isPendingTab;

    public OrderAdapter(Context context, boolean isPendingTab) {
        this.context = context;
        this.isPendingTab = isPendingTab;
    }

    public void setOrders(List<Order> orders) {
        this.orders = orders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.tvTableNumber.setText("Bàn: " + order.getTableNumber());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        holder.tvTimestamp.setText("Thời gian: " + sdf.format(order.getTimestamp()));

        // Hiển thị chi tiết món
        StringBuilder details = new StringBuilder();
        for (Map<String, Object> item : order.getItems()) {
            details.append(item.get("name")).append(" (x").append(item.get("qty")).append(") - ")
                    .append(item.get("price")).append("đ, ");
        }
        if (details.length() > 0) details.setLength(details.length() - 2); // Xóa dấu phẩy cuối
        holder.tvOrderDetails.setText(details.toString());

        if (order.getNotes() != null && !order.getNotes().isEmpty()) {
            holder.tvNotes.setText("Ghi chú: " + order.getNotes());
            holder.tvNotes.setVisibility(View.VISIBLE);
        } else {
            holder.tvNotes.setVisibility(View.GONE);
        }
        holder.tvTotal.setText("Tổng: " + (int) order.getTotal() + "đ");

        // Hiển thị nút theo tab
        if (isPendingTab) {
            holder.btnConfirm.setVisibility(View.VISIBLE);
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnPayment.setVisibility(View.GONE);
            holder.btnPrint.setVisibility(View.GONE);
        } else {
            holder.btnConfirm.setVisibility(View.GONE);
            holder.btnCancel.setVisibility(View.GONE);
            holder.btnPayment.setVisibility(View.VISIBLE);
            holder.btnPrint.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return orders != null ? orders.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTableNumber, tvTimestamp, tvOrderDetails, tvNotes, tvTotal;
        Button btnConfirm, btnCancel, btnPayment, btnPrint;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTableNumber = itemView.findViewById(R.id.tvTableNumber);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvOrderDetails = itemView.findViewById(R.id.tvOrderDetails);
            tvNotes = itemView.findViewById(R.id.tvNotes);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnCancel = itemView.findViewById(R.id.BtnCancel);
            btnPayment = itemView.findViewById(R.id.btnPayment);
            btnPrint = itemView.findViewById(R.id.btnPrint);
        }
    }
}