package com.example.appbanhang.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.appbanhang.common.utils.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffCallRepository {

    public interface SimpleCallback { void onSuccess(); void onError(String msg); }
    public interface ResultCallback<T> { void onSuccess(T result); void onError(String msg); }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Realtime listener cho staff calls đang queued (admin nhận) */
    @NonNull
    public ListenerRegistration listenQueuedCalls(@NonNull ResultCallback<List<DocumentSnapshot>> cb) {
        return db.collection(Constants.COLL_STAFF_CALLS)
                .whereEqualTo("status", Constants.CALL_QUEUED)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    cb.onSuccess(snap.getDocuments());
                });
    }

    /** Admin xác nhận đã nhận call */
    public void acknowledgeCall(@NonNull String callId, @Nullable String adminUid, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_STAFF_CALLS).document(callId)
                .update("status", Constants.CALL_HANDLED,
                        "ackBy", adminUid,
                        "acknowledgedAt", FieldValue.serverTimestamp())
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(e.getMessage()));
    }

    /** Kiểm tra spam (2 phút) trước khi gọi */
    public void checkSpam(@NonNull String uid, @NonNull ResultCallback<Boolean> cb) {
        db.collection(Constants.COLL_STAFF_CALLS)
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        Date t = snap.getDocuments().get(0).getDate("createdAt");
                        if (t != null && (System.currentTimeMillis() - t.getTime()) < Constants.CALL_SPAM_MILLIS) {
                            cb.onSuccess(false); // spam
                            return;
                        }
                    }
                    cb.onSuccess(true); // OK
                })
                .addOnFailureListener(e -> cb.onError("Lỗi kiểm tra lịch sử gọi: " + e.getMessage()));
    }

    /** Lấy tên bàn rồi tạo staff call */
    public void createCall(@NonNull String uid, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_ACC).whereEqualTo("uid", uid).limit(1).get()
                .addOnSuccessListener(accSnap -> {
                    String tableName = "Khách";
                    if (!accSnap.isEmpty()) {
                        tableName = accSnap.getDocuments().get(0).getString("name");
                        if (tableName == null || tableName.trim().isEmpty()) tableName = "Khách";
                    }
                    Map<String, Object> call = new HashMap<>();
                    call.put("userId", uid);
                    call.put("name", tableName);
                    call.put("createdAt", FieldValue.serverTimestamp());
                    call.put("status", Constants.CALL_QUEUED);

                    db.collection(Constants.COLL_STAFF_CALLS).add(call)
                            .addOnSuccessListener(ref -> cb.onSuccess())
                            .addOnFailureListener(e -> cb.onError("Lỗi gửi yêu cầu: " + e.getMessage()));
                })
                .addOnFailureListener(e -> cb.onError("Lỗi lấy thông tin bàn: " + e.getMessage()));
    }
}
