package com.example.appbanhang.data.repository;

import androidx.annotation.NonNull;

import com.example.appbanhang.common.utils.Constants;
import com.example.appbanhang.data.model.CartItem;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartRepository {

    private static final String SUB_ITEMS = "items";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface SimpleCallback { void onSuccess(); void onError(String msg); }
    public interface ResultCallback<T> { void onSuccess(T result); void onError(String msg); }

    /**
     * Thêm 1 món vào cart. Trả Boolean: TRUE = đã thêm, FALSE = chạm trần qty.
     */
    public void addToCart(@NonNull String uid, @NonNull String productId,
                          @NonNull String name, String imageUrl, Double price,
                          @NonNull ResultCallback<Boolean> cb) {
        DocumentReference docRef = db.collection(Constants.COLL_CARTS).document(uid)
                .collection(SUB_ITEMS).document(productId);

        db.runTransaction((Transaction.Function<Boolean>) transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            Long curr = snap.exists() ? snap.getLong("qty") : 0L;
            if (curr == null) curr = 0L;
            if (curr >= Constants.MAX_QTY) return Boolean.FALSE;

            long next = Math.min(curr + 1, Constants.MAX_QTY);
            if (snap.exists()) {
                transaction.update(docRef, "qty", next);
            } else {
                Map<String, Object> data = new HashMap<>();
                data.put("name", name);
                data.put("imageUrl", imageUrl);
                data.put("price", price);
                data.put("qty", next);
                transaction.set(docRef, data, SetOptions.merge());
            }
            return Boolean.TRUE;
        }).addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /** Tải tất cả cart items cho 1 uid */
    public void loadCart(@NonNull String uid, @NonNull ResultCallback<List<CartItem>> cb) {
        db.collection(Constants.COLL_CARTS).document(uid).collection(SUB_ITEMS)
                .get()
                .addOnSuccessListener(snap -> {
                    List<CartItem> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        CartItem it = d.toObject(CartItem.class);
                        if (it != null) { it.id = d.getId(); list.add(it); }
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(e -> cb.onError("Lỗi tải giỏ hàng: " + e.getMessage()));
    }

    /** Realtime listener cho tổng qty (badge) */
    @NonNull
    public ListenerRegistration listenCartCount(@NonNull String uid, @NonNull ResultCallback<Integer> cb) {
        return db.collection(Constants.COLL_CARTS).document(uid).collection(SUB_ITEMS)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) { cb.onSuccess(0); return; }
                    int sum = 0;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Number q = (Number) d.get("qty");
                        sum += (q == null) ? 0 : q.intValue();
                    }
                    cb.onSuccess(sum);
                });
    }

    public void changeQty(@NonNull String uid, @NonNull String itemId, int delta, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_CARTS).document(uid).collection(SUB_ITEMS)
                .document(itemId)
                .update("qty", FieldValue.increment(delta))
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi cập nhật số lượng: " + e.getMessage()));
    }

    public void deleteItem(@NonNull String uid, @NonNull String itemId, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_CARTS).document(uid).collection(SUB_ITEMS)
                .document(itemId).delete()
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi xóa sản phẩm: " + e.getMessage()));
    }

    public void clearCart(@NonNull String uid, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_CARTS).document(uid).collection(SUB_ITEMS)
                .get()
                .addOnSuccessListener(snap -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : snap.getDocuments()) batch.delete(d.getReference());
                    batch.commit()
                            .addOnSuccessListener(v -> cb.onSuccess())
                            .addOnFailureListener(e -> cb.onError("Lỗi xóa giỏ hàng: " + e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onError("Lỗi xóa giỏ hàng: " + e.getMessage()));
    }
}
