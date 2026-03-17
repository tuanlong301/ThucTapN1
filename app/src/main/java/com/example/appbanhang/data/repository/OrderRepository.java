package com.example.appbanhang.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.appbanhang.common.utils.Constants;
import com.example.appbanhang.data.model.Order;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class OrderRepository {

    public interface SimpleCallback { void onSuccess(); void onError(String msg); }
    public interface ResultCallback<T> { void onSuccess(T result); void onError(String msg); }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Tạo đơn hàng mới */
    public void createOrder(@NonNull Map<String, Object> order, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_ORDERS).add(order)
                .addOnSuccessListener(ref -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi đặt hàng: " + e.getMessage()));
    }

    /** Realtime listener cho pending orders */
    @NonNull
    public ListenerRegistration listenPending(@NonNull ResultCallback<List<Order>> cb) {
        return db.collection(Constants.COLL_ORDERS)
                .whereEqualTo("status", Constants.STATUS_PENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError("Lỗi tải đơn: " + e.getMessage()); return; }
                    if (snap == null) { cb.onSuccess(new ArrayList<>()); return; }
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Order o = d.toObject(Order.class);
                        if (o == null) o = new Order();
                        o.id = d.getId();
                        o.createdAt = d.getDate("timestamp");
                        list.add(o);
                    }
                    cb.onSuccess(list);
                });
    }

    /** Realtime listener cho confirmed orders (ẩn printed) */
    @NonNull
    public ListenerRegistration listenConfirmed(@NonNull ResultCallback<List<Order>> cb) {
        return db.collection(Constants.COLL_ORDERS)
                .whereEqualTo("status", Constants.STATUS_CONFIRMED)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { cb.onError("Lỗi tải đơn: " + e.getMessage()); return; }
                    if (snap == null) { cb.onSuccess(new ArrayList<>()); return; }
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Boolean printed = d.getBoolean("printed");
                        if (printed != null && printed) continue;
                        Order o = d.toObject(Order.class);
                        if (o == null) o = new Order();
                        o.id = d.getId();
                        o.createdAt = d.getDate("timestamp");
                        list.add(o);
                    }
                    cb.onSuccess(list);
                });
    }

    public void updateStatus(@NonNull String orderId, @NonNull String newStatus, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_ORDERS).document(orderId)
                .update("status", newStatus,
                        "confirmedAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi: " + e.getMessage()));
    }

    public void markPaid(@NonNull String orderId, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_ORDERS).document(orderId)
                .update("paymentStatus", Constants.PAY_PAID,
                        "paidAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi: " + e.getMessage()));
    }

    public void cancelOrder(@NonNull String orderId, @NonNull String reason, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_ORDERS).document(orderId)
                .update("status", Constants.STATUS_CANCELED,
                        "cancelReason", reason,
                        "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi: " + e.getMessage()));
    }

    public void markPrinted(@NonNull String orderId, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_ORDERS).document(orderId)
                .update("printed", true,
                        "printedAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    public void getOrder(@NonNull String orderId, @NonNull ResultCallback<DocumentSnapshot> cb) {
        db.collection(Constants.COLL_ORDERS).document(orderId).get()
                .addOnSuccessListener(cb::onSuccess)
                .addOnFailureListener(e -> cb.onError("Lỗi đọc đơn: " + e.getMessage()));
    }

    /** Query orders cho thống kê owner: confirmed + paid trong khoảng thời gian */
    public void queryPaidOrders(@NonNull Date from, @NonNull Date to, @NonNull ResultCallback<List<DocumentSnapshot>> cb) {
        db.collection(Constants.COLL_ORDERS)
                .whereEqualTo("status", Constants.STATUS_CONFIRMED)
                .whereEqualTo("paymentStatus", Constants.PAY_PAID)
                .whereGreaterThanOrEqualTo("timestamp", from)
                .whereLessThanOrEqualTo("timestamp", to)
                .get()
                .addOnSuccessListener(snap -> cb.onSuccess(snap.getDocuments()))
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }
}
