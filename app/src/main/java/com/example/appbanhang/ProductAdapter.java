package com.example.appbanhang;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    // ===== Callback báo về Activity khi bấm Add =====
    public interface OnAddToCartListener {
        void onAdd(Product p);
    }
    private OnAddToCartListener onAddToCartListener;
    public void setOnAddToCartListener(OnAddToCartListener l) {
        this.onAddToCartListener = l;
    }

    private final Context context;
    private final List<Product> productList;

    public ProductAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.txtName.setText(product.getName());
        holder.txtDesc.setText(product.getDescription());

        // Giá: Number -> "53.000 đ"
        if (product.getPrice() != null) {
            java.text.NumberFormat nf =
                    java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
            holder.txtPrice.setText(nf.format(product.getPrice()) + " đ");
        } else {
            holder.txtPrice.setText("-");
        }

        // Ảnh: ưu tiên imageUrl, fallback imageResId, cuối cùng placeholder
        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(context)
                    .load(product.getImageUrl())
                    // Nếu muốn không crop, đổi .centerCrop() -> .fitCenter() và đảm bảo XML dùng adjustViewBounds
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(holder.imgProduct);
        } else if (product.getImageResId() != 0) {
            holder.imgProduct.setImageResource(product.getImageResId());
        } else {
            holder.imgProduct.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // Nút thêm giỏ
        holder.btnAdd.setOnClickListener(v -> {
            // Toast nhẹ
            android.widget.Toast.makeText(context, "Đã thêm: " + product.getName(),
                    android.widget.Toast.LENGTH_SHORT).show();

            // Gọi callback về MainMenu để tăng badge
            if (onAddToCartListener != null) {
                onAddToCartListener.onAdd(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView txtName, txtDesc, txtPrice;
        Button btnAdd;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            txtName    = itemView.findViewById(R.id.txtName);
            txtDesc    = itemView.findViewById(R.id.txtDesc);
            txtPrice   = itemView.findViewById(R.id.txtPrice);
            btnAdd     = itemView.findViewById(R.id.btnAdd);
        }
    }
}
