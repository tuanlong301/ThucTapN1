package com.example.appbanhang.user.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.example.appbanhang.model.Product;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    public interface OnAddToCartListener { void onAdd(Product p); }
    private OnAddToCartListener onAddToCartListener;
    public void setOnAddToCartListener(OnAddToCartListener l) { this.onAddToCartListener = l; }

    private final Context context;
    private final List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder h, int pos) {
        Product p = productList.get(pos);

        h.txtName.setText(p.getName());
        h.txtDesc.setText(p.getDescription());

        if (p.getPrice() != null) {
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new Locale("vi","VN"));
            h.txtPrice.setText(nf.format(p.getPrice()) + " đ");
        } else {
            h.txtPrice.setText("-");
        }

        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(context)
                    .load(p.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(h.imgProduct);
        } else if (p.getImageResId() != 0) {
            h.imgProduct.setImageResource(p.getImageResId());
        } else {
            h.imgProduct.setImageResource(R.drawable.ic_launcher_foreground);
        }

        h.btnAdd.setOnClickListener(v -> {
            if (onAddToCartListener != null) onAddToCartListener.onAdd(p);

        });
    }

    @Override public int getItemCount() { return productList.size(); }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtDesc, txtPrice;
        Button btnAdd;
        ProductViewHolder(@NonNull View v) {
            super(v);
            imgProduct = v.findViewById(R.id.imgProduct);
            txtName    = v.findViewById(R.id.txtName);
            txtDesc    = v.findViewById(R.id.txtDesc);
            txtPrice   = v.findViewById(R.id.txtPrice);
            btnAdd     = v.findViewById(R.id.btnAdd);
        }
    }
}
