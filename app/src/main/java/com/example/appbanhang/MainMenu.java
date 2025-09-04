package com.example.appbanhang;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainMenu extends AppCompatActivity {

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

        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ProductAdapter(this, productList);
        rvProducts.setAdapter(adapter);

        // Khi bấm nút + trên từng item
        adapter.setOnAddToCartListener(this::addToCart);

        // Nút mở giỏ hàng
        findViewById(R.id.btnCart).setOnClickListener(v -> {
            startActivity(new android.content.Intent(MainMenu.this, CartActivity.class));
        });

        db = FirebaseFirestore.getInstance();

        // Đăng nhập ẩn danh rồi mới wire sự kiện và load
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> {
                    wireCategoryClicks();
                    wireCategoryClicks1();
                    wireCategoryClicks2();
                    wireCategoryClicks3();

                    listenCartCount();  // <- badge realtime
                    loadAll();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Auth FAIL: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (uid == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        if (p.getId() == null) {
            Toast.makeText(this, "Thiếu ID sản phẩm", Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("name", p.getName());
        data.put("imageUrl", p.getImageUrl());
        data.put("price", p.getPrice());
        data.put("qty", FieldValue.increment(1));   // tăng 1

        db.collection("carts").document(uid)
                .collection("items").document(p.getId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Đã thêm: " + p.getName(), Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Thêm giỏ hàng lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
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
}
