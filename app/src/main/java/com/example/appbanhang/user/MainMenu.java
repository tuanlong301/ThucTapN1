package com.example.appbanhang.user;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;


import com.example.appbanhang.BaseActivity;
import com.example.appbanhang.user.adapter.ProductAdapter;
import com.example.appbanhang.R;
import com.example.appbanhang.model.Product;

import com.google.firebase.firestore.FieldValue;

import com.google.firebase.firestore.Query;


import java.util.ArrayList;
import java.util.List;

public class MainMenu extends BaseActivity {
    private static final long MAX_QTY = 9;
    private RecyclerView rvProducts;
    private final List<Product> productList = new ArrayList<>();
    private ProductAdapter adapter;
    private FirebaseFirestore db;

    private int cartCount = 0;
    private TextView tvCartBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCartBadge = findViewById(R.id.tvCartBadge);
        updateCartBadge();
        findViewById(R.id.btnCallStaff).setOnClickListener(v -> callStaff());

        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ProductAdapter(this, productList);
        rvProducts.setAdapter(adapter);
        adapter.setOnAddToCartListener(this::addToCart);
        findViewById(R.id.btnCart).setOnClickListener(v ->
                startActivity(new android.content.Intent(MainMenu.this, CartActivity.class)));

        db = FirebaseFirestore.getInstance();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            // Chỉ dùng ẩn danh khi thật sự chưa đăng nhập
            auth.signInAnonymously()
                    .addOnSuccessListener(r -> initUIData())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Auth FAIL: " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            initUIData();    // đã đăng nhập -> chạy luôn
        }
    }

    // gom phần gán click + lắng nghe + load dữ liệu vào đây
    private void initUIData() {
        wireCategoryClicks();
        wireCategoryClicks1();
        wireCategoryClicks2();
        wireCategoryClicks3();

        listenCartCount();
        loadAll();
    }


    private void wireCategoryClicks() {
        View best = findViewById(R.id.btnBestSeller);
        if (best != null) best.setOnClickListener(v -> loadByCategory("bestseller"));

    }

    private void wireCategoryClicks1() {
        View best = findViewById(R.id.btnkm);
        if (best != null) best.setOnClickListener(v -> loadByCategory("km"));

    }
    private void wireCategoryClicks2() {
        View best = findViewById(R.id.btnCM);
        if (best != null) best.setOnClickListener(v -> loadByCategory("cm"));

    }
    private void wireCategoryClicks3() {
        View best = findViewById(R.id.btnNuoc);
        if (best != null) best.setOnClickListener(v -> loadByCategory("nuoc"));

    }

    /** Hiển thị tất cả món */
    private void loadAll() {
        db.collection("food_001")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    productList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) {
                            p.setId(doc.getId());              // <- LẤY ID tài liệu
                            productList.add(p);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Loaded " + productList.size() + " items (all)", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "READ FAIL", e);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Lọc theo category */
    private void loadByCategory(String cat) {
        db.collection("food_001")
                .whereEqualTo("category", cat)
                .get()
                .addOnSuccessListener(snap -> {
                    productList.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Product p = d.toObject(Product.class);
                        if (p != null) {
                            p.setId(d.getId());               // <- nhớ set id
                            productList.add(p);
                        }
                    }
                    // sort theo tên ở client
                    java.util.Collections.sort(productList, (a, b) -> {
                        if (a.getName() == null) return -1;
                        if (b.getName() == null) return 1;
                        return a.getName().compareToIgnoreCase(b.getName());
                    });
                    adapter.notifyDataSetChanged();
                });
    }

    /** Ghi/ tăng số lượng 1 món vào giỏ hàng Firestore */

    private void addToCart(Product p) {
        if (!requireOnline()) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;
        if (p == null || p.getId() == null) return;

        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference docRef = db.collection("carts")
                .document(uid)
                .collection("items")
                .document(p.getId());

        db.runTransaction((Transaction.Function<Boolean>) transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            Long curr = snap.exists() ? snap.getLong("qty") : 0L;
            if (curr == null) curr = 0L;

            // Đủ 5 rồi -> không cho cộng tiếp
            if (curr >= MAX_QTY) {
                return Boolean.FALSE; // báo về là đã chạm trần
            }

            long next = Math.min(curr + 1, MAX_QTY);

            if (snap.exists()) {
                transaction.update(docRef, "qty", next);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("name", p.getName());
                data.put("imageUrl", p.getImageUrl());
                data.put("price", p.getPrice());
                data.put("qty", next);
                transaction.set(docRef, data, SetOptions.merge());
            }
            return Boolean.TRUE; // đã cập nhật
        }).addOnSuccessListener(changed -> {
            if (Boolean.TRUE.equals(changed)) {
                Toast.makeText(this, "Đã thêm món: " + p.getName(), Toast.LENGTH_SHORT).show();
            } else {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Giới hạn số lượng")
                        .setMessage("Nếu muốn đặt nhiều hơn vui lòng liên hệ nhân viên  ")
                        .setPositiveButton("OK", null)
                        .show();
            }
        }).addOnFailureListener(e -> {
            // im lặng hoặc Log.e("CART", "addToCart failed", e);
        });
    }

    /** Lắng nghe tổng số lượng trong giỏ để cập nhật badge */
    private void listenCartCount() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("carts").document(uid).collection("items")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    int sum = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Number q = (Number) d.get("qty");
                        sum += (q == null) ? 0 : q.intValue();
                    }
                    cartCount = sum;
                    updateCartBadge();
                });
    }

    private void updateCartBadge() {
        if (tvCartBadge == null) return;
        if (cartCount <= 0) {
            tvCartBadge.setVisibility(View.GONE);
        } else {
            tvCartBadge.setText(cartCount > 99 ? "99+" : String.valueOf(cartCount));
            tvCartBadge.setVisibility(View.VISIBLE);
        }
    }
    /**Goi nhan vien */

    private void callStaff() {
        if (!requireOnline()) {
            // 🚨 Không cho queue Firestore khi offline
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Mất kết nối")
                    .setMessage("Không thể gửi yêu cầu khi mất mạng. Vui lòng kết nối lại internet.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1) Chống spam 2 phút
        db.collection("staff_calls")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    long now = System.currentTimeMillis();
                    if (!snap.isEmpty()) {
                        java.util.Date t = snap.getDocuments().get(0).getDate("createdAt");
                        if (t != null && (now - t.getTime()) < 2 * 60 * 1000) {
                            new androidx.appcompat.app.AlertDialog.Builder(this)
                                    .setTitle("Vui lòng đợi")
                                    .setMessage("Bạn đã gọi nhân viên gần đây. Vui lòng chờ tối đa 2 phút trước khi gọi lại.")
                                    .setPositiveButton("OK", null)
                                    .show();
                            return;
                        }
                    }

                    // 2) Lấy tên bàn từ collection acc
                    db.collection("acc")
                            .whereEqualTo("uid", uid)   // tìm doc có field uid = current uid
                            .limit(1)
                            .get()
                            .addOnSuccessListener(accSnap -> {
                                String tableName = "Khách";
                                if (!accSnap.isEmpty()) {
                                    tableName = accSnap.getDocuments().get(0).getString("name");
                                    if (tableName == null || tableName.trim().isEmpty()) {
                                        tableName = "Khách";
                                    }
                                }

                                // 3) Tạo request gọi NV
                                Map<String, Object> call = new HashMap<>();
                                call.put("userId", uid);
                                call.put("name", tableName);   // <-- lưu đúng field name
                                call.put("createdAt", FieldValue.serverTimestamp());
                                call.put("status", "queued");

                                db.collection("staff_calls")
                                        .add(call)
                                        .addOnSuccessListener(ref ->
                                                new androidx.appcompat.app.AlertDialog.Builder(this)
                                                        .setTitle("Đã gửi yêu cầu")
                                                        .setMessage("Đã thông báo cho nhân viên. Vui lòng đợi.")
                                                        .setPositiveButton("OK", null)
                                                        .show())
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "Lỗi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_LONG).show());
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi lấy thông tin bàn: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi kiểm tra lịch sử gọi: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

}


