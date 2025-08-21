package com.example.appbanhang;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter hiển thị giỏ hàng: mỗi item là CartItem với các trường từ Firestore.
 * Không chơi notifyDataSetChanged bừa — chỉ refresh đúng dòng thay đổi để mượt.
 */
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface OnQtyClick {
        void onPlus(int pos);
        void onMinus(int pos);
    }

    private final Context ctx;
    private final List<CartActivity.CartItem> data;
    private final OnQtyClick cb;
    private final NumberFormat money = NumberFormat.getInstance(new Locale("vi", "VN"));

    public CartAdapter(Context ctx, List<CartActivity.CartItem> data, OnQtyClick cb) {
        setHasStableIds(true);
        this.ctx = ctx;
        this.data = data;
        this.cb = cb;
    }

    @Override
    public long getItemId(int position) {
        CartActivity.CartItem item = data.get(position);
        return item.id != null ? item.id.hashCode() : position;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CartActivity.CartItem item = data.get(pos);
        if (item == null) return; // Tránh crash nếu item null

        // Tên và biến thể
        h.tvName.setText(item.name != null ? item.name : "Không có tên");
        h.tvSub.setText("1 x " + (item.name != null ? item.name : "Không có tên")); // Sửa thành tvSub

        // Số lượng và thành tiền
        long qty = item.qty != null ? item.qty : 0;
        h.tvQty.setText(String.valueOf(qty));
        double line = (item.price != null ? item.price : 0) * qty;
        h.tvLineTotal.setText(money.format(line) + " đ");

        // Ảnh
        if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
            Glide.with(ctx)
                    .load(item.imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(h.ivThumb);
        } else {
            h.ivThumb.setImageResource(R.drawable.ic_launcher_foreground);
        }

        // Cộng
        h.btnPlus.setOnClickListener(v -> {
            int adapterPos = h.getAbsoluteAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION && cb != null) {
                cb.onPlus(adapterPos);
            }
        });

        // Trừ
        h.btnMinus.setOnClickListener(v -> {
            int adapterPos = h.getAbsoluteAdapterPosition();
            if (adapterPos != RecyclerView.NO_POSITION && cb != null) {
                cb.onMinus(adapterPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName, tvSub, tvQty, tvLineTotal;
        TextView btnMinus, btnPlus; // Thay ImageButton thành TextView

        VH(@NonNull View v) {
            super(v);
            ivThumb = v.findViewById(R.id.imgThumb);
            tvName = v.findViewById(R.id.tvName);
            tvSub = v.findViewById(R.id.tvSub);
            tvQty = v.findViewById(R.id.tvQty);
            tvLineTotal = v.findViewById(R.id.tvLineTotal);
            btnMinus = v.findViewById(R.id.btnMinus);
            btnPlus = v.findViewById(R.id.btnPlus);


            v.setFocusable(true);
            btnMinus.setContentDescription("Giảm số lượng");
            btnPlus.setContentDescription("Tăng số lượng");
        }
    }
}