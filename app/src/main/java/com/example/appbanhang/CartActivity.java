package com.example.appbanhang;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private static final String TAG = "CartActivity";
    private RecyclerView rv;
    private View emptyView, footerBar;
    private TextView tvTotal;
    private Button btnOrder;
    private final List<CartItem> items = new ArrayList<>();
    private CartAdapter cartAdapter;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_cart);

        rv = findViewById(R.id.rvCart);
        emptyView = findViewById(R.id.viewEmpty);
        footerBar = findViewById(R.id.footerBar);
        tvTotal = findViewById(R.id.tvTotal);
        btnOrder = findViewById(R.id.btnCheckout);

        rv.setLayoutManager(new LinearLayoutManager(this));

        cartAdapter = new CartAdapter(this, items, new CartAdapter.OnQtyClick() {
            @Override
            public void onPlus(int pos) {
                if (pos >= 0 && pos < items.size()) {
                    changeQty(items.get(pos), 1);
                }
            }

            @Override
            public void onMinus(int pos) {
                if (pos >= 0 && pos < items.size()) {
                    CartItem item = items.get(pos);
                    if (item.qty != null && item.qty <= 1) {
                        deleteItem(item);
                    } else {
                        changeQty(item, -1);
                    }
                }
            }
        });
        rv.setAdapter(cartAdapter);

        db = FirebaseFirestore.getInstance();
        loadCart();

        // Xử lý nút Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Xử lý nút Đặt hàng
        btnOrder.setOnClickListener(v -> {
            Toast.makeText(CartActivity.this, "Đặt hàng thành công!", Toast.LENGTH_SHORT).show();
            clearCart();
        });
    }

    private void loadCart() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            Toast.makeText(this, "Không thể tải giỏ hàng: Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish(); // Đóng activity nếu không có uid
            return;
        }
        Log.d(TAG, "Loading cart for uid: " + uid);
        db.collection("carts").document(uid).collection("items")
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();
                    if (snap.isEmpty()) {
                        Log.d(TAG, "Cart is empty");
                    } else {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            CartItem it = d.toObject(CartItem.class);
                            if (it != null) {
                                it.id = d.getId();
                                items.add(it);
                                Log.d(TAG, "Loaded item: " + it.name + ", qty: " + it.qty);
                            }
                        }
                    }
                    cartAdapter.notifyDataSetChanged();
                    updateUIState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading cart: " + e.getMessage());
                    Toast.makeText(this, "Lỗi tải giỏ hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateUIState() {
        if (items.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            footerBar.setVisibility(View.GONE);
            btnOrder.setEnabled(false);
            btnOrder.setAlpha(0.5f);
        } else {
            emptyView.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            footerBar.setVisibility(View.VISIBLE);
            btnOrder.setEnabled(true);
            btnOrder.setAlpha(1.0f);

            double sum = 0;
            for (CartItem c : items) {
                double price = (c.price == null ? 0 : c.price);
                long qty = (c.qty == null ? 0 : c.qty);
                sum += price * qty;
            }
            NumberFormat f = NumberFormat.getInstance(new Locale("vi", "VN"));
            tvTotal.setText(f.format(sum) + " đ");
        }
    }

    private void changeQty(CartItem item, int delta) {
        if (item == null || item.id == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("carts").document(uid).collection("items")
                .document(item.id)
                .update("qty", FieldValue.increment(delta))
                .addOnSuccessListener(v -> loadCart())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating qty: " + e.getMessage());
                    Toast.makeText(this, "Lỗi cập nhật số lượng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteItem(CartItem item) {
        if (item == null || item.id == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("carts").document(uid).collection("items")
                .document(item.id)
                .delete()
                .addOnSuccessListener(v -> loadCart())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting item: " + e.getMessage());
                    Toast.makeText(this, "Lỗi xóa sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearCart() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("carts").document(uid).collection("items")
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        d.getReference().delete();
                    }
                    loadCart();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error clearing cart: " + e.getMessage());
                    Toast.makeText(this, "Lỗi xóa giỏ hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Model đọc từ Firestore */
    public static class CartItem {
        public String id, name, imageUrl;
        public Double price;
        public Long qty;

        public CartItem() {}
    }
}