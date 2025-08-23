package com.example.appbanhang;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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
    private LinearLayout layoutPaymentMethod;
    private final List<Object> items = new ArrayList<>(); // Sử dụng Object để chứa cả CartItem và ghi chú
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
        layoutPaymentMethod = findViewById(R.id.layoutPaymentMethod);

        rv.setLayoutManager(new LinearLayoutManager(this));

        cartAdapter = new CartAdapter(this, items, new CartAdapter.OnQtyClick() {
            @Override
            public void onPlus(int pos) {
                if (pos >= 0 && pos < items.size() - 1) { // -1 để tránh ghi chú
                    changeQty((CartItem) items.get(pos), 1);
                }
            }

            @Override
            public void onMinus(int pos) {
                if (pos >= 0 && pos < items.size() - 1) { // -1 để tránh ghi chú
                    CartItem item = (CartItem) items.get(pos);
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
            String note = cartAdapter.getNote(); // Lấy ghi chú từ adapter
            int selectedPaymentId = ((RadioGroup) findViewById(R.id.rgPaymentMethod)).getCheckedRadioButtonId();
            String paymentMethod = (selectedPaymentId == R.id.rbCash) ? "Tiền mặt" :
                    (selectedPaymentId == R.id.rbTransfer) ? "Chuyển khoản" : "Chưa chọn";

            if (items.isEmpty() || items.size() == 1) { // Kiểm tra nếu chỉ có ghi chú hoặc rỗng
                Toast.makeText(CartActivity.this, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedPaymentId == -1) {
                Toast.makeText(CartActivity.this, "Vui lòng chọn phương thức thanh toán!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(CartActivity.this, "Đặt hàng thành công!\nGhi chú: " + note + "\nPhương thức: " + paymentMethod, Toast.LENGTH_LONG).show();
            clearCart();
        });
    }

    public void togglePaymentMethod(View v) {
        boolean isVisible = layoutPaymentMethod.getVisibility() == View.VISIBLE;
        layoutPaymentMethod.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    private void loadCart() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            Toast.makeText(this, "Không thể tải giỏ hàng: Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Log.d(TAG, "Loading cart for uid: " + uid);
        db.collection("carts").document(uid).collection("items")
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();
                    if (snap.isEmpty()) {
                        Log.d(TAG, "Cart is empty");
                        items.add(new NoteItem("")); // Thêm ghi chú rỗng nếu giỏ rỗng
                    } else {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            CartItem it = d.toObject(CartItem.class);
                            if (it != null) {
                                it.id = d.getId();
                                items.add(it);
                                Log.d(TAG, "Loaded item: " + it.name + ", qty: " + it.qty);
                            }
                        }
                        items.add(new NoteItem("")); // Thêm ghi chú cuối danh sách
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
        if (items.isEmpty() || (items.size() == 1 && items.get(0) instanceof NoteItem)) {
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
            for (Object obj : items) {
                if (obj instanceof CartItem) {
                    CartItem c = (CartItem) obj;
                    double price = (c.price == null ? 0 : c.price);
                    long qty = (c.qty == null ? 0 : c.qty);
                    sum += price * qty;
                }
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

    /** Model cho ghi chú */
    public static class NoteItem {
        public String note;

        public NoteItem(String note) {
            this.note = note;
        }

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}