package com.example.appbanhang.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.appbanhang.common.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class AuthRepository {

    public interface LoginCallback {
        void onSuccess(String uid, @Nullable String email);
        void onError(String message);
    }

    public interface ProfileCallback {
        void onResult(String name, String role);
        void onNotFound(String message);
        void onError(String message);
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public void signIn(@NonNull String email, @NonNull String pass, @NonNull LoginCallback cb) {
        auth.signOut();
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> {
                    FirebaseUser user = res.getUser();
                    if (user == null) {
                        cb.onError("Không lấy được thông tin người dùng!");
                        return;
                    }
                    cb.onSuccess(user.getUid(), user.getEmail());
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthException) {
                        String code = ((FirebaseAuthException) e).getErrorCode();
                        switch (code) {
                            case "ERROR_INVALID_EMAIL":         cb.onError("Email không hợp lệ."); break;
                            case "ERROR_USER_NOT_FOUND":        cb.onError("Tài khoản không tồn tại."); break;
                            case "ERROR_WRONG_PASSWORD":        cb.onError("Mật khẩu không đúng."); break;
                            case "ERROR_OPERATION_NOT_ALLOWED": cb.onError("Provider Email/Password chưa bật."); break;
                            default:                            cb.onError("Đăng nhập thất bại: " + code);
                        }
                    } else {
                        cb.onError("Đăng nhập thất bại: " + e.getMessage());
                    }
                });
    }

    public void fetchProfile(@NonNull String uid, @Nullable String email, @NonNull ProfileCallback cb) {
        db.collection(Constants.COLL_ACC).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = safe(doc.getString("name"), "Khách");
                        String role = safe(doc.getString("role"), "user");

                        Map<String, Object> base = new HashMap<>();
                        base.put("uid", uid);
                        if (email != null) base.put("tk", email);
                        db.collection(Constants.COLL_ACC).document(uid).set(base, SetOptions.merge());

                        cb.onResult(name, role);

                    } else if (email != null && !email.isEmpty()) {
                        db.collection(Constants.COLL_ACC).whereEqualTo("tk", email).limit(1).get()
                                .addOnSuccessListener(q -> {
                                    if (!q.isEmpty()) {
                                        DocumentSnapshot d = q.getDocuments().get(0);
                                        String name = safe(d.getString("name"), "Khách");
                                        String role = safe(d.getString("role"), "user");

                                        Map<String, Object> data = d.getData() != null
                                                ? new HashMap<>(d.getData()) : new HashMap<>();
                                        data.put("uid", uid);
                                        data.put("tk", email);
                                        db.collection(Constants.COLL_ACC).document(uid).set(data, SetOptions.merge());

                                        cb.onResult(name, role);
                                    } else {
                                        cb.onNotFound("Tài khoản chưa được khởi tạo hồ sơ. Liên hệ admin!");
                                    }
                                })
                                .addOnFailureListener(e -> cb.onError("Lỗi tìm hồ sơ: " + e.getMessage()));
                    } else {
                        cb.onNotFound("Tài khoản chưa có hồ sơ. Liên hệ admin!");
                    }
                })
                .addOnFailureListener(e -> cb.onError("Lỗi đọc hồ sơ: " + e.getMessage()));
    }

    /** Lấy tên từ acc/{uid}, dùng cho CartActivity cache */
    public void fetchName(@NonNull String uid, @NonNull NameCallback ncb) {
        db.collection(Constants.COLL_ACC).document(uid).get()
                .addOnSuccessListener(doc -> {
                    String n = doc != null ? doc.getString("name") : null;
                    ncb.onResult(n != null && !n.trim().isEmpty() ? n.trim() : null);
                })
                .addOnFailureListener(e -> ncb.onResult(null));
    }

    /** Tìm name từ acc bằng uid hoặc fallback email, rồi trả về tên */
    public void resolveOrdererName(@NonNull String uid, @Nullable String email, @NonNull NameCallback ncb) {
        db.collection(Constants.COLL_ACC).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = safeName(doc.getString("name"));
                        ncb.onResult(name);
                    } else if (email != null && !email.isEmpty()) {
                        db.collection(Constants.COLL_ACC).whereEqualTo("tk", email).limit(1).get()
                                .addOnSuccessListener(q -> {
                                    if (!q.isEmpty()) {
                                        DocumentSnapshot d = q.getDocuments().get(0);
                                        String name = safeName(d.getString("name"));
                                        Map<String, Object> data = d.getData() != null ? new HashMap<>(d.getData()) : new HashMap<>();
                                        data.put("uid", uid);
                                        data.put("tk", email);
                                        db.collection(Constants.COLL_ACC).document(uid).set(data, SetOptions.merge());
                                        ncb.onResult(name);
                                    } else {
                                        ncb.onResult("Khách");
                                    }
                                })
                                .addOnFailureListener(e -> ncb.onResult("Khách"));
                    } else {
                        ncb.onResult("Khách");
                    }
                })
                .addOnFailureListener(e -> ncb.onResult("Khách"));
    }

    public void signOut() { auth.signOut(); }

    @Nullable
    public FirebaseUser getCurrentUser() { return auth.getCurrentUser(); }

    public void signInAnonymously(@NonNull Runnable onSuccess, @NonNull java.util.function.Consumer<String> onFail) {
        auth.signInAnonymously()
                .addOnSuccessListener(r -> onSuccess.run())
                .addOnFailureListener(e -> onFail.accept(e.getMessage()));
    }

    public interface NameCallback { void onResult(@Nullable String name); }

    private static String safe(String v, String def) {
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }
    private static String safeName(String n) { return (n == null || n.trim().isEmpty()) ? "Khách" : n.trim(); }
}
