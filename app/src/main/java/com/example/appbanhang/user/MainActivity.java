package com.example.appbanhang.user;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.appbanhang.BaseActivity;
import com.example.appbanhang.R;
import com.example.appbanhang.admin.AdminMenu;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private static final String TAG = "MainActivity";

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();


        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> doLogin());
        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(this, "Liên hệ admin để đặt lại mật khẩu", Toast.LENGTH_SHORT).show()
        );
    }

    private void doLogin() {
        String email = etEmail.getText().toString().trim();
        String pass  = etPassword.getText().toString().trim();

        // 1) Validate input trước
        if (email.isEmpty() || pass.isEmpty()) {
            toast("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // 2) Check mạng trước khi gọi Firebase
        if (!requireOnline()) return;

        // 3) Disable nút rồi mới gọi Firebase
        btnLogin.setEnabled(false);

        // Clear phiên cũ nếu có
        auth.signOut();

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(res -> {
                    FirebaseUser user = res.getUser();
                    if (user == null) {
                        btnLogin.setEnabled(true);
                        toast("Không lấy được thông tin người dùng!");
                        return;
                    }
                    String uid  = user.getUid();
                    String mail = user.getEmail();
                    Log.d(TAG, "Login OK uid=" + uid + ", email=" + mail);
                    fetchProfileThenLaunch(uid, mail);
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    if (e instanceof FirebaseAuthException) {
                        String code = ((FirebaseAuthException) e).getErrorCode();
                        Log.e(TAG, "Auth error: " + code, e);
                        switch (code) {
                            case "ERROR_INVALID_EMAIL":         toast("Email không hợp lệ."); break;
                            case "ERROR_USER_NOT_FOUND":        toast("Tài khoản không tồn tại."); break;
                            case "ERROR_WRONG_PASSWORD":        toast("Mật khẩu không đúng."); break;
                            case "ERROR_OPERATION_NOT_ALLOWED": toast("Provider Email/Password chưa bật."); break;
                            default:                            toast("Đăng nhập thất bại: " + code);
                        }
                    } else {
                        toast("Đăng nhập thất bại: " + e.getMessage());
                    }
                });
    }

    /** Đọc acc/{uid}; nếu chưa có thì tìm theo tk=email và migrate sang acc/{uid}. Sau đó cache tên & điều hướng theo role. */
    private void fetchProfileThenLaunch(String uid, @Nullable String email) {
        if (!requireOnline()) {
            btnLogin.setEnabled(true);
            return;
        }

        db.collection("acc").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        // ✅ Hồ sơ đã có → đọc và merge
                        String name = safe(doc.getString("name"), "Khách");
                        String role = safe(doc.getString("role"), "user");

                        Map<String, Object> base = new HashMap<>();
                        base.put("uid", uid);
                        if (email != null) base.put("tk", email);
                        db.collection("acc").document(uid).set(base, SetOptions.merge());

                        cacheName(name);
                        launchByRole(role, name);

                    } else if (email != null && !email.isEmpty()) {
                        // ✅ Fallback: tìm hồ sơ theo email
                        db.collection("acc").whereEqualTo("tk", email).limit(1).get()
                                .addOnSuccessListener(q -> {
                                    if (!q.isEmpty()) {
                                        DocumentSnapshot d = q.getDocuments().get(0);
                                        String name = safe(d.getString("name"), "Khách");
                                        String role = safe(d.getString("role"), "user");

                                        // Gán UID mới vào doc cũ
                                        Map<String, Object> data = d.getData() != null
                                                ? new HashMap<>(d.getData())
                                                : new HashMap<>();
                                        data.put("uid", uid);
                                        data.put("tk", email);
                                        db.collection("acc").document(uid).set(data, SetOptions.merge());

                                        cacheName(name);
                                        launchByRole(role, name);
                                    } else {
                                        // ❌ Không tự tạo doc mới
                                        toast("Tài khoản chưa được khởi tạo hồ sơ. Liên hệ admin!");
                                        auth.signOut();
                                        btnLogin.setEnabled(true);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    btnLogin.setEnabled(true);
                                    toast("Lỗi tìm hồ sơ: " + e.getMessage());
                                });

                    } else {
                        // ❌ Không có email để đối chiếu
                        toast("Tài khoản chưa có hồ sơ. Liên hệ admin!");
                        auth.signOut();
                        btnLogin.setEnabled(true);
                    }
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    toast("Lỗi đọc hồ sơ: " + e.getMessage());
                });
    }


    private void cacheName(String name) {
        getSharedPreferences("app", MODE_PRIVATE)
                .edit()
                .putString("profile_name", name)
                .apply();
    }

    private void launchByRole(String roleRaw, String name) {
        btnLogin.setEnabled(true);
        String role = roleRaw == null ? "user" : roleRaw.trim().toLowerCase();
        toast("Xin chào " + (name != null ? name : ""));
        if ("admin".equals(role)) {
            startActivity(new Intent(this, AdminMenu.class));
        } else {
            startActivity(new Intent(this, MainMenu.class));
        }
        finish();
    }

    private String safe(String v, String def) {
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }
}
