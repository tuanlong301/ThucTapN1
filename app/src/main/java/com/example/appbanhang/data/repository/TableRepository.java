package com.example.appbanhang.data.repository;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.appbanhang.common.utils.Constants;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TableRepository {

    public interface SimpleCallback { void onSuccess(); void onError(String msg); }
    public interface ResultCallback<T> { void onSuccess(T result); void onError(String msg); }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // ========== Admin Tables (table_states) ==========

    public static class TableRow {
        public String userId, name, status;
        @Nullable public String sub;
    }

    /** Load tất cả bàn (acc role=user) kèm trạng thái từ table_states */
    public void loadTablesWithState(@NonNull ResultCallback<List<TableRow>> cb) {
        db.collection(Constants.COLL_ACC)
                .whereEqualTo("role", Constants.ROLE_USER)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    final List<Task<?>> pending = new ArrayList<>();
                    final List<TableRow> rows = new ArrayList<>();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String uid = d.getString("uid");
                        String name = d.getString("name");
                        if (uid == null || uid.isEmpty() || name == null || name.isEmpty()) continue;

                        TableRow row = new TableRow();
                        row.userId = uid;
                        row.name = name;
                        row.status = "Trống";

                        Task<DocumentSnapshot> t = db.collection(Constants.COLL_TABLE_STATES).document(uid).get()
                                .addOnSuccessListener(ms -> {
                                    if (ms != null && ms.exists()) {
                                        String st = ms.getString("status");
                                        if (Constants.TABLE_OCCUPIED.equals(st)) {
                                            row.status = "Đang sử dụng";
                                        } else if (Constants.TABLE_RESERVED.equals(st)) {
                                            row.status = "Đã đặt trước";
                                            Date rt = ms.getDate("reservedAt");
                                            if (rt != null) {
                                                row.sub = "Vào lúc " + new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(rt);
                                            }
                                        }
                                    }
                                    rows.add(row);
                                });
                        pending.add(t);
                    }

                    Tasks.whenAll(pending).addOnSuccessListener(ignored -> {
                        Collections.sort(rows, (a, b) -> a.name.compareToIgnoreCase(b.name));
                        cb.onSuccess(rows);
                    });
                })
                .addOnFailureListener(e -> cb.onError("Lỗi tải bàn: " + e.getMessage()));
    }

    public void saveManualState(@NonNull String uid, @NonNull String status, @Nullable Date reservedAt, @NonNull SimpleCallback cb) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", status);
        m.put("updatedAt", new Date());
        m.put("reservedAt", Constants.TABLE_RESERVED.equals(status) ? reservedAt : null);

        db.collection(Constants.COLL_TABLE_STATES).document(uid).set(m)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi lưu: " + e.getMessage()));
    }

    // ========== Owner Tables (Accounts) ==========

    public static class AccountDoc {
        public final String id, name, tk, mk, role, uid;
        public AccountDoc(String id, String name, String tk, String mk, String role, String uid) {
            this.id = id; this.name = name; this.tk = tk; this.mk = mk; this.role = role; this.uid = uid;
        }
    }

    @NonNull
    public ListenerRegistration listenAccounts(@NonNull ResultCallback<List<AccountDoc>> cb) {
        return db.collection(Constants.COLL_ACC).addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            List<AccountDoc> list = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                list.add(new AccountDoc(d.getId(), s(d.get("name")), s(d.get("tk")),
                        s(d.get("mk")), s(d.get("role")), s(d.get("uid"))));
            }
            Collections.sort(list, (a, b) -> {
                boolean aa = Constants.isAdmin(a.role), bb = Constants.isAdmin(b.role);
                if (aa != bb) return aa ? -1 : 1;
                return (a.name == null ? "" : a.name).compareToIgnoreCase(b.name == null ? "" : b.name);
            });
            cb.onSuccess(list);
        });
    }

    public void createAuthAndSave(@NonNull Context ctx, @NonNull String name,
                                   @NonNull String email, @NonNull String password,
                                   @NonNull SimpleCallback cb) {
        // Kiểm tra trùng tk trước
        db.collection(Constants.COLL_ACC).whereEqualTo("tk", email).limit(1).get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) { cb.onError("Tài khoản đã tồn tại trong acc"); return; }
                    doCreateAuth(ctx, name, email, password, cb);
                })
                .addOnFailureListener(e -> cb.onError("Lỗi kiểm tra tk: " + e.getMessage()));
    }

    private void doCreateAuth(Context ctx, String name, String email, String password, SimpleCallback cb) {
        FirebaseApp secondary = getOrInitSecondaryApp(ctx);
        FirebaseAuth auth = FirebaseAuth.getInstance(secondary);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(res -> {
                    String uid = res.getUser() != null ? res.getUser().getUid() : null;
                    upsertAcc(uid, name, email, password, false, auth, secondary, cb);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener(res -> {
                                    String uid = res.getUser() != null ? res.getUser().getUid() : null;
                                    if (res.getUser() != null) {
                                        res.getUser().updatePassword(password)
                                                .addOnCompleteListener(x -> upsertAcc(uid, name, email, password, true, auth, secondary, cb));
                                    } else {
                                        upsertAcc(uid, name, email, password, true, auth, secondary, cb);
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    cb.onError("Email đã tồn tại trong Authentication");
                                    cleanup(auth, secondary);
                                });
                    } else {
                        cb.onError("Lỗi tạo Auth user: " + e.getMessage());
                        cleanup(auth, secondary);
                    }
                });
    }

    private void upsertAcc(String uid, String name, String email, String password,
                            boolean isRestore, FirebaseAuth auth, FirebaseApp secondary, SimpleCallback cb) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name); data.put("tk", email); data.put("role", Constants.ROLE_USER);
        if (uid != null) data.put("uid", uid);

        if (uid != null) {
            db.collection(Constants.COLL_ACC).document(uid).set(data)
                    .addOnSuccessListener(v -> {
                        dedupeAcc(uid, email);
                        cb.onSuccess();
                    })
                    .addOnFailureListener(e -> cb.onError("Lỗi ghi acc: " + e.getMessage()))
                    .addOnCompleteListener(done -> cleanup(auth, secondary));
        } else {
            db.collection(Constants.COLL_ACC).add(data)
                    .addOnSuccessListener(v -> cb.onSuccess())
                    .addOnCompleteListener(done -> cleanup(auth, secondary));
        }
    }

    private void dedupeAcc(String uid, String tk) {
        db.collection(Constants.COLL_ACC).whereEqualTo("tk", tk).get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if (!d.getId().equals(uid)) db.collection(Constants.COLL_ACC).document(d.getId()).delete();
                    }
                });
    }

    public void updateAccount(@NonNull String id, @NonNull Map<String, Object> data, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_ACC).document(id).update(data)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi lưu: " + e.getMessage()));
    }

    public void deleteAccount(@NonNull String id, @NonNull SimpleCallback cb) {
        db.collection(Constants.COLL_ACC).document(id).delete()
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError("Lỗi xoá: " + e.getMessage()));
    }

    public void updateAuthPassword(@NonNull Context ctx, @NonNull String email,
                                    @NonNull String oldPass, @NonNull String newPass,
                                    @NonNull SimpleCallback cb) {
        FirebaseApp secondary = getOrInitSecondaryApp(ctx);
        FirebaseAuth auth = FirebaseAuth.getInstance(secondary);
        auth.signInWithEmailAndPassword(email, oldPass)
                .addOnSuccessListener(res -> {
                    if (res.getUser() == null) { cb.onError("Không lấy được user"); cleanup(auth, secondary); return; }
                    res.getUser().updatePassword(newPass)
                            .addOnSuccessListener(v -> cb.onSuccess())
                            .addOnFailureListener(e -> cb.onError("Lỗi đổi mật khẩu Auth: " + e.getMessage()))
                            .addOnCompleteListener(done -> cleanup(auth, secondary));
                })
                .addOnFailureListener(e -> {
                    cb.onError("Không đăng nhập được user để đổi mật khẩu");
                    cleanup(auth, secondary);
                });
    }

    // ========== Helpers ==========

    private FirebaseApp getOrInitSecondaryApp(Context ctx) {
        FirebaseOptions opts = FirebaseApp.getInstance().getOptions();
        try { return FirebaseApp.getInstance("auth-helper"); }
        catch (IllegalStateException e) { return FirebaseApp.initializeApp(ctx, opts, "auth-helper"); }
    }

    private void cleanup(FirebaseAuth auth, FirebaseApp app) {
        try { auth.signOut(); } catch (Exception ignore) {}
        try { app.delete(); } catch (Exception ignore) {}
    }

    private static String s(Object v) { return v == null ? "" : String.valueOf(v); }
}
