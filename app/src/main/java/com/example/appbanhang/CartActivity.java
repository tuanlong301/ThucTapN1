package com.example.appbanhang;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.firestore.SetOptions;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartActivity extends AppCompatActivity {

    private static final String TAG = "CartActivity";

    private RecyclerView rv;
    private View emptyView, footerBar;
    private TextView tvTotal;
    private Button btnOrder;
    private LinearLayout layoutPaymentMethod;

    private final List<Object> items = new ArrayList<>();
    private CartAdapter cartAdapter;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_cart);

        // Views
        rv = findViewById(R.id.rvCart);
        emptyView = findViewById(R.id.viewEmpty);
        footerBar = findViewById(R.id.footerBar);
        tvTotal = findViewById(R.id.tvTotal);
        btnOrder = findViewById(R.id.btnCheckout);
        layoutPaymentMethod = findViewById(R.id.layoutPaymentMethod);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Recycler
        rv.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(this, items, new CartAdapter.OnQtyClick() {
            @Override public void onPlus(int pos) {
                if (pos >= 0 && pos < items.size() - 1) changeQty((CartItem) items.get(pos), 1);
            }
            @Override public void onMinus(int pos) {
                if (pos >= 0 && pos < items.size() - 1) {
                    CartItem it = (CartItem) items.get(pos);
                    if (it.qty != null && it.qty <= 1) deleteItem(it); else changeQty(it, -1);
                }
            }
        });
        rv.setAdapter(cartAdapter);

        // Events
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnOrder.setOnClickListener(v -> onCheckout());

        // Load data
        loadCart();
        // Optional: làm mới cache tên trong nền để lần sau luôn sẵn sàng
        refreshCachedNameAsync();
    }

    /** Nhấn Đặt hàng */
    private void onCheckout() {
        // Ghi chú ở cuối list (CartAdapter cung cấp)
        String note = cartAdapter.getNote();

        RadioGroup rgPaymentMethod = findViewById(R.id.rgPaymentMethod);
        if (rgPaymentMethod == null) {
            toast("Lỗi: Không tìm thấy phương thức thanh toán!");
            return;
        }
        int checkedId = rgPaymentMethod.getCheckedRadioButtonId();
        String paymentMethod = (checkedId == R.id.rbCash) ? "Tiền mặt"
                : (checkedId == R.id.rbTransfer) ? "Chuyển khoản" : null;

        if (items.isEmpty() || items.size() == 1) { toast("Giỏ hàng trống!"); return; }
        if (paymentMethod == null) { toast("Vui lòng chọn phương thức thanh toán!"); return; }
        if (auth.getCurrentUser() == null) { toast("Chưa đăng nhập!"); return; }

        // Lấy tên từ cache (ưu tiên để không bị "Khách")
        String cachedName = getSharedPreferences("app", MODE_PRIVATE).getString("profile_name", null);
        if (cachedName != null && !cachedName.trim().isEmpty()) {
            saveOrder(note, paymentMethod, cachedName.trim());
            // làm mới cache trong nền cho lần sau
            refreshCachedNameAsync();
            return;
        }

        // Chưa có cache → đọc theo UID (fallback email nếu cần)
        String uid = auth.getCurrentUser().getUid();
        fetchOrdererNameAndSave(uid, note, paymentMethod);
    }

    /** Tải giỏ hàng theo uid hiện tại */
    private void loadCart() {
        if (auth.getCurrentUser() == null) {
            toast("Không thể tải giỏ hàng: Chưa đăng nhập");
            finish();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        Log.d(TAG, "Loading cart for uid: " + uid);

        db.collection("carts").document(uid).collection("items")
                .get()
                .addOnSuccessListener(snap -> {
                    items.clear();
                    if (!snap.isEmpty()) {
                        for (DocumentSnapshot d : snap.getDocuments()) {
                            CartItem it = d.toObject(CartItem.class);
                            if (it != null) {
                                it.id = d.getId();
                                items.add(it);
                            }
                        }
                    }
                    items.add(new NoteItem("")); // Hàng ghi chú ở cuối
                    cartAdapter.notifyDataSetChanged();
                    updateUIState();
                })
                .addOnFailureListener(e -> toast("Lỗi tải giỏ hàng: " + e.getMessage()));
    }

    /** Hiển thị/ẩn footer + tổng tiền */
    private void updateUIState() {
        if (items.isEmpty() || (items.size() == 1 && items.get(0) instanceof NoteItem)) {
            emptyView.setVisibility(View.VISIBLE);
            rv.setVisibility(View.GONE);
            footerBar.setVisibility(View.GONE);
            btnOrder.setEnabled(false);
            btnOrder.setAlpha(0.5f);
            return;
        }

        emptyView.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);
        footerBar.setVisibility(View.VISIBLE);
        btnOrder.setEnabled(true);
        btnOrder.setAlpha(1f);

        double sum = 0;
        for (Object obj : items) {
            if (obj instanceof CartItem) {
                CartItem c = (CartItem) obj;
                sum += (c.price != null ? c.price : 0) * (c.qty != null ? c.qty : 0);
            }
        }
        NumberFormat f = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvTotal.setText(f.format(sum) + " đ");
    }

    /** Lưu đơn */
    private void saveOrder(String note, String paymentMethod, String name) {
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> order = new HashMap<>();
        order.put("userId", uid);
        order.put("name", name);
        order.put("items", getItemsListString()); // đang lưu chuỗi theo format cũ
        order.put("total", String.valueOf(calculateTotal()));
        order.put("notes", note);
        order.put("paymentMethod", paymentMethod);
        order.put("status", "pending");
        order.put("timestamp", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

        db.collection("orders")
                .add(order)
                .addOnSuccessListener(r -> {
                    toast("Đặt hàng thành công!");
                    clearCart();
                })
                .addOnFailureListener(e -> toast("Lỗi đặt hàng: " + e.getMessage()));
    }

    /** Lấy tên theo acc/{uid}; nếu thiếu thì fallback tk=email và migrate; cache lại để lần sau dùng ngay */
    private void fetchOrdererNameAndSave(String uid, String note, String paymentMethod) {
        db.collection("acc").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = safeName(doc.getString("name"));
                        cacheName(name);
                        saveOrder(note, paymentMethod, name);
                    } else {
                        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : null;
                        if (email == null || email.isEmpty()) {
                            saveOrder(note, paymentMethod, "Khách");
                            return;
                        }
                        db.collection("acc").whereEqualTo("tk", email).limit(1).get()
                                .addOnSuccessListener(q -> {
                                    if (!q.isEmpty()) {
                                        DocumentSnapshot d = q.getDocuments().get(0);
                                        String name = safeName(d.getString("name"));

                                        // migrate sang acc/{uid} cho chuẩn
                                        Map<String, Object> data = d.getData() != null ? new HashMap<>(d.getData()) : new HashMap<>();
                                        data.put("uid", uid);
                                        data.put("tk", email);
                                        db.collection("acc").document(uid).set(data, SetOptions.merge());

                                        cacheName(name);
                                        saveOrder(note, paymentMethod, name);
                                    } else {
                                        saveOrder(note, paymentMethod, "Khách");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    toast("Lỗi tìm hồ sơ: " + e.getMessage());
                                    saveOrder(note, paymentMethod, "Khách");
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    toast("Lỗi đọc hồ sơ: " + e.getMessage());
                    saveOrder(note, paymentMethod, "Khách");
                });
    }

    /** Làm mới cache tên trong nền (không chặn luồng) */
    private void refreshCachedNameAsync() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("acc").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String n = doc != null ? doc.getString("name") : null;
                    if (n != null && !n.trim().isEmpty()) cacheName(n.trim());
                });
    }

    private void cacheName(String name) {
        getSharedPreferences("app", MODE_PRIVATE)
                .edit()
                .putString("profile_name", name)
                .apply();
    }

    private String safeName(String n) { return (n == null || n.trim().isEmpty()) ? "Khách" : n.trim(); }

    /** Tạo chuỗi items theo format hiện tại */
    private String getItemsListString() {
        StringBuilder sb = new StringBuilder("[ ");
        for (Object obj : items) {
            if (obj instanceof CartItem) {
                CartItem c = (CartItem) obj;
                sb.append("{ \"name\": \"").append(c.name)
                        .append("\", \"qty\": ").append(c.qty)
                        .append(", \"price\": ").append(c.price)
                        .append(", \"notes\": \"\" }, ");
            }
        }
        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        sb.append(" ]");
        return sb.toString();
    }

    private double calculateTotal() {
        double sum = 0;
        for (Object obj : items) {
            if (obj instanceof CartItem) {
                CartItem c = (CartItem) obj;
                sum += (c.price != null ? c.price : 0) * (c.qty != null ? c.qty : 0);
            }
        }
        return sum;
    }

    /** Dùng trong XML: android:onClick="togglePaymentMethod" */
    public void togglePaymentMethod(View v) {
        View panel = findViewById(R.id.layoutPaymentMethod);
        if (panel == null) {
            toast("Thiếu layoutPaymentMethod trong layout!");
            return;
        }
        panel.setVisibility(panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void changeQty(CartItem item, int delta) {
        if (item == null || item.id == null || auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("carts").document(uid).collection("items")
                .document(item.id)
                .update("qty", FieldValue.increment(delta))
                .addOnSuccessListener(v -> loadCart())
                .addOnFailureListener(e -> toast("Lỗi cập nhật số lượng: " + e.getMessage()));
    }

    private void deleteItem(CartItem item) {
        if (item == null || item.id == null || auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("carts").document(uid).collection("items")
                .document(item.id)
                .delete()
                .addOnSuccessListener(v -> loadCart())
                .addOnFailureListener(e -> toast("Lỗi xóa sản phẩm: " + e.getMessage()));
    }

    private void clearCart() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("carts").document(uid).collection("items")
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) d.getReference().delete();
                    loadCart();
                })
                .addOnFailureListener(e -> toast("Lỗi xóa giỏ hàng: " + e.getMessage()));
    }

    // ====== Models ======
    public static class CartItem {
        public String id, name, imageUrl;
        public Double price;
        public Long qty;
        public CartItem() {}
    }

    public static class NoteItem {
        public String note;
        public NoteItem(String note) { this.note = note; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    // ====== Utils ======
    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}
