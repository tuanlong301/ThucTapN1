package com.example.appbanhang.user.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appbanhang.R;
import com.example.appbanhang.net.NetworkMonitor;
import com.example.appbanhang.user.CartActivity;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface OnQtyClick {
        void onPlus(int pos);
        void onMinus(int pos);
    }

    private final Context ctx;
    private final List<Object> data; // CartItem + NoteItem
    private final OnQtyClick cb;
    private final NumberFormat money = NumberFormat.getInstance(new Locale("vi", "VN"));
    private String note = "";

    public CartAdapter(Context ctx, List<Object> data, OnQtyClick cb) {
        setHasStableIds(true);
        this.ctx = ctx;
        this.data = data;
        this.cb = cb;
    }

    @Override
    public long getItemId(int position) {
        Object item = data.get(position);
        if (item instanceof CartActivity.CartItem) {
            return ((CartActivity.CartItem) item).id != null
                    ? ((CartActivity.CartItem) item).id.hashCode()
                    : position;
        }
        return position; // NoteItem
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) { // NoteItem
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_note, parent, false);
            return new VH(v);
        }
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public int getItemViewType(int position) {
        return (data.get(position) instanceof CartActivity.NoteItem) ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Object item = data.get(pos);

        if (item instanceof CartActivity.CartItem) {
            CartActivity.CartItem cartItem = (CartActivity.CartItem) item;

            h.tvName.setText(cartItem.name != null ? cartItem.name : "Không có tên");
            h.tvSub.setText("1 x " + (cartItem.name != null ? cartItem.name : "Không có tên"));

            long qty = cartItem.qty != null ? cartItem.qty : 0;
            h.tvQty.setText(String.valueOf(qty));

            double line = (cartItem.price != null ? cartItem.price : 0) * qty;
            h.tvLineTotal.setText(money.format(line) + " đ");

            if (cartItem.imageUrl != null && !cartItem.imageUrl.isEmpty()) {
                Glide.with(ctx).load(cartItem.imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(h.ivThumb);
            } else {
                h.ivThumb.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Cập nhật trạng thái nút theo mạng + qty
            updateButtonsEnabled(h, qty);

            h.btnPlus.setOnClickListener(v -> {
                if (!NetworkMonitor.get(v.getContext()).hasInternetNow()) {
                    showNoInternet(v.getContext());
                    return;
                }
                int adapterPos = h.getAbsoluteAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION && cb != null) cb.onPlus(adapterPos);
            });

            h.btnMinus.setOnClickListener(v -> {
                if (!NetworkMonitor.get(v.getContext()).hasInternetNow()) {
                    showNoInternet(v.getContext());
                    return;
                }
                int adapterPos = h.getAbsoluteAdapterPosition();
                if (adapterPos != RecyclerView.NO_POSITION && cb != null) cb.onMinus(adapterPos);
            });

        } else if (item instanceof CartActivity.NoteItem) {
            CartActivity.NoteItem noteItem = (CartActivity.NoteItem) item;
            h.etNote.setText(noteItem.getNote());
            h.etNote.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    noteItem.setNote(s.toString());
                    note = s.toString();
                }
            });
        }
    }

    @Override public int getItemCount() { return data != null ? data.size() : 0; }

    public String getNote() { return note; }

    private void updateButtonsEnabled(@NonNull VH h, long qty) {
        boolean online = NetworkMonitor.get(h.itemView.getContext()).hasInternetNow();
        h.btnPlus.setEnabled(online);
        h.btnMinus.setEnabled(online && qty > 0); // không cho trừ về 0
    }

    private void showNoInternet(Context ctx) {
        new AlertDialog.Builder(ctx)
                .setTitle("Mất kết nối")
                .setMessage("Không có internet. Vui lòng kiểm tra kết nối và thử lại.")
                .setPositiveButton("OK", null)
                .show();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvName, tvSub, tvQty, tvLineTotal;
        TextView btnMinus, btnPlus;
        EditText etNote;
        VH(@NonNull View v) {
            super(v);
            ivThumb = v.findViewById(R.id.imgThumb);
            tvName = v.findViewById(R.id.tvName);
            tvSub = v.findViewById(R.id.tvSub);
            tvQty = v.findViewById(R.id.tvQty);
            tvLineTotal = v.findViewById(R.id.tvLineTotal);
            btnMinus = v.findViewById(R.id.btnMinus);
            btnPlus = v.findViewById(R.id.btnPlus);
            etNote = v.findViewById(R.id.etNote);
        }
    }
}
