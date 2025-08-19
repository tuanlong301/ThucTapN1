package com.example.appbanhang;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.View;              // <-- thêm
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

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
        setContentView(R.layout.activity_main); // layout có rvProducts + các nút category

        tvCartBadge = findViewById(R.id.tvCartBadge);   // <- quan trọng
        updateCartBadge();
        // RecyclerView
        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ProductAdapter(this, productList);
        rvProducts.setAdapter(adapter);

        adapter.setOnAddToCartListener(p -> {
            cartCount++;
            updateCartBadge();
        });
        // Firestore
        db = FirebaseFirestore.getInstance();

        // Đăng nhập ẩn danh rồi gắn click + load mặc định
        FirebaseAuth.getInstance().signInAnonymously()
                .addOnSuccessListener(r -> {
                    Log.d("AUTH", "Anonymous sign-in OK: " + r.getUser().getUid());
                    wireCategoryClicks();   // gắn sự kiện các nút
                    loadAll();              // mặc định hiển thị tất cả
                })
                .addOnFailureListener(e -> {
                    Log.e("AUTH", "Anonymous sign-in FAIL", e);
                    Toast.makeText(this, "Auth FAIL: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Gắn click cho các nút category bạn đã code cứng trong XML */
    private void wireCategoryClicks() {

        if (findViewById(R.id.btnBestSeller) != null)
            findViewById(R.id.btnBestSeller).setOnClickListener(v -> loadByCategory("bestseller"));

//        if (findViewById(R.id.btnPromo) != null)
//            findViewById(R.id.btnPromo).setOnClickListener(v -> loadByCategory("promo"));
//
//        if (findViewById(R.id.btnPasta) != null)
//            findViewById(R.id.btnPasta).setOnClickListener(v -> loadByCategory("pasta"));
//
//        if (findViewById(R.id.btnDrink) != null)
//            findViewById(R.id.btnDrink).setOnClickListener(v -> loadByCategory("drink"));
//
//        // Nếu muốn thêm nút “Tất cả” thì đặt id btnAll trong XML
//        if (findViewById(R.id.btnAll) != null)
//            findViewById(R.id.btnAll).setOnClickListener(v -> loadAll());
    }

    /** Tải tất cả sản phẩm */
    private void loadAll() {
        db.collection("food_001")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    productList.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) productList.add(p);
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Loaded " + productList.size() + " items (all)", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "READ FAIL", e);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    /** Lọc theo category (field 'category' trong từng document) */
    private void loadByCategory(String cat) {
        db.collection("food_001")
                .whereEqualTo("category", cat)
                .get()
                .addOnSuccessListener(snap -> {
                    productList.clear();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Product p = d.toObject(Product.class);
                        if (p != null) productList.add(p);
                    }
                    // Sort tại client theo name
                    java.util.Collections.sort(productList, (a, b) -> {
                        if (a.getName() == null) return -1;
                        if (b.getName() == null) return 1;
                        return a.getName().compareToIgnoreCase(b.getName());
                    });
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateCartBadge() {
        if (tvCartBadge == null) return;
        if (cartCount <= 0) {
            tvCartBadge.setVisibility(View.GONE);
        } else {
            // Hiển thị 99+
            tvCartBadge.setText(cartCount > 99 ? "99+" : String.valueOf(cartCount));
            tvCartBadge.setVisibility(View.VISIBLE);
        }
    }
}
