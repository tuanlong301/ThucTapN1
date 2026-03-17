package com.example.appbanhang.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.appbanhang.data.model.Product;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductRepository {

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private static final String COLLECTION = "food_001";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void loadAll(@NonNull Callback<List<Product>> cb) {
        db.collection(COLLECTION)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Product> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Product p = doc.toObject(Product.class);
                        if (p != null) { p.setId(doc.getId()); list.add(p); }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError("Lỗi tải dữ liệu: " + e.getMessage()));
    }

    public void loadByCategory(@NonNull String category, @NonNull Callback<List<Product>> cb) {
        db.collection(COLLECTION)
                .whereEqualTo("category", category)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Product> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Product p = d.toObject(Product.class);
                        if (p != null) { p.setId(d.getId()); list.add(p); }
                    }
                    Collections.sort(list, (a, b) -> {
                        if (a.getName() == null) return -1;
                        if (b.getName() == null) return 1;
                        return a.getName().compareToIgnoreCase(b.getName());
                    });
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /** Realtime listener trả về toàn bộ products khi có thay đổi */
    @NonNull
    public ListenerRegistration listenAll(@NonNull Callback<List<Product>> cb) {
        return db.collection(COLLECTION).addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) {
                cb.onSuccess(new ArrayList<>());
                return;
            }
            List<Product> list = new ArrayList<>();
            for (DocumentSnapshot d : snap) {
                Product p = d.toObject(Product.class);
                if (p != null) { p.setId(d.getId()); list.add(p); }
            }
            cb.onSuccess(list);
        });
    }

    public void addProduct(@NonNull Map<String, Object> data, @NonNull Callback<Void> cb) {
        db.collection(COLLECTION).add(data)
                .addOnSuccessListener(ref -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError("Lỗi: " + e.getMessage()));
    }

    public void updateProduct(@NonNull String id, @NonNull Map<String, Object> data, @NonNull Callback<Void> cb) {
        db.collection(COLLECTION).document(id).update(data)
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError("Lỗi: " + e.getMessage()));
    }

    public void deleteProduct(@NonNull String id, @NonNull Callback<Void> cb) {
        db.collection(COLLECTION).document(id).delete()
                .addOnSuccessListener(v -> cb.onSuccess(null))
                .addOnFailureListener(e -> cb.onError("Lỗi: " + e.getMessage()));
    }
}
