package com.example.appbanhang.user.adapter;

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

    private static final int MAX_QTY = 9;

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
            CartActivity.CartItem ci = (CartActivity.CartItem) item;
            return ci.id != null ? ci.id.hashCode() : position;
        }
        return position; // NoteItem
    }

    @Override
    public int getItemViewType(int position) {
        return (data.get(position) instanceof CartActivity.NoteItem) ? 1 : 0;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 1) { // NoteItem
            View v = LayoutInflater.from(ctx).inflate(R.layout.item_note, parent, false);
            return new VH(v);
        }
        View v = LayoutInflater.from(ctx).inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Object item = data.get(pos);

        if (item instanceof CartActivity.CartItem) {
            CartActivity.CartItem ci = (CartActivity.CartItem) item;

            // Tên, phụ đề
            h.tvName.setText(ci.name != null ? ci.name : "Không có tên");
            h.tvSub.setText("1 x " + (ci.name != null ? ci.name : "Không có tên"));

            // Số lượng & thành tiền
            long qty = ci.qty != null ? ci.qty : 0;
            h.tvQty.setText(String.valueOf(qty));
            double line = (ci.price != null ? ci.price : 0) * qty;
            h.tvLineTotal.setText(money.format(line) + " đ");

            // Ảnh
            if (ci.imageUrl != null && !ci.imageUrl.isEmpty()) {
                Glide.with(ctx).load(ci.imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_launcher_foreground)
                        .error(R.drawable.ic_launcher_foreground)
                        .into(h.ivThumb);
            } else {
                h.ivThumb.setImageResource(R.drawable.ic_launcher_foreground);
            }

            // Bật/tắt nút theo mạng & qty
            updateButtonsEnabled(h, qty);

            // Nút "+"
            h.btnPlus.setOnClickListener(v -> {
                if (!NetworkMonitor.get(v.getContext()).hasInternetNow()) return; // im lặng
                int adapterPos = h.getBindingAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION || cb == null) return;

                Object obj = data.get(adapterPos);
                if (!(obj instanceof CartActivity.CartItem)) return;

                CartActivity.CartItem curr = (CartActivity.CartItem) obj;
                long qNow = curr.qty != null ? curr.qty : 0;

                if (qNow >= MAX_QTY) return; // chạm trần → im lặng

                cb.onPlus(adapterPos);
            });

            // Nút "−"
            h.btnMinus.setOnClickListener(v -> {
                if (!NetworkMonitor.get(v.getContext()).hasInternetNow()) return;
                int adapterPos = h.getBindingAdapterPosition();
                if (adapterPos == RecyclerView.NO_POSITION || cb == null) return;
                cb.onMinus(adapterPos);
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

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public String getNote() {
        return note;
    }

    private void updateButtonsEnabled(@NonNull VH h, long qty) {
        boolean online = NetworkMonitor.get(h.itemView.getContext()).hasInternetNow();
        h.btnPlus.setEnabled(online && qty < MAX_QTY);
        h.btnMinus.setEnabled(online && qty >= 1);
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
