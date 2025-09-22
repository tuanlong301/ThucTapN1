package com.example.appbanhang.owner.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appbanhang.R;
import com.example.appbanhang.model.Product;
import com.example.appbanhang.utils.InvoiceUtilss;

import java.util.ArrayList;
import java.util.List;

public class OwnerProductAdapter extends RecyclerView.Adapter<OwnerProductAdapter.VH> {

    public interface OnAction {
        void onClick(Product p);
        void onEdit(Product p);
        void onDelete(Product p);
    }

    private final List<Product> data = new ArrayList<>();
    private final OnAction cb;

    public OwnerProductAdapter(OnAction cb) {
        this.cb = cb;
        setHasStableIds(true);
    }

    public void setData(List<Product> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @Override public int getItemCount() { return data.size(); }

    @Override public long getItemId(int position) {
        String id = data.get(position).getId();
        return id != null ? id.hashCode() : RecyclerView.NO_ID;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_owner_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Product p = data.get(pos);

        h.txtName.setText(p.getName());
        h.txtCategory.setText(p.getCategory());
        h.txtDesc.setText(p.getDescription());
        h.txtPrice.setText(InvoiceUtilss.formatVnd(p.getPrice()));

        Glide.with(h.itemView.getContext())
                .load(p.getImageUrl())
                .placeholder(R.drawable.r1)
                .error(R.drawable.r1)
                .centerCrop()
                .into(h.imgProduct);

        h.itemView.setOnClickListener(v -> cb.onClick(p));
        h.btnEdit.setOnClickListener(v -> cb.onEdit(p));
        h.btnDelete.setOnClickListener(v -> cb.onDelete(p));
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtCategory, txtDesc, txtPrice;
        Button btnEdit, btnDelete;

        VH(@NonNull View v) {
            super(v);
            imgProduct = v.findViewById(R.id.imgProduct);
            txtName    = v.findViewById(R.id.txtName);
            txtCategory= v.findViewById(R.id.txtCategory);
            txtDesc    = v.findViewById(R.id.txtDesc);
            txtPrice   = v.findViewById(R.id.txtPrice);
            btnEdit    = v.findViewById(R.id.btnEdit);
            btnDelete  = v.findViewById(R.id.btnDelete);
        }
    }
}
