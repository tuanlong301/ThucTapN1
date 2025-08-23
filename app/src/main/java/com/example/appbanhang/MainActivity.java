package com.example.appbanhang;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        db = FirebaseFirestore.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword); // Thêm TextView cho quên mật khẩu

        // Xử lý nút Đăng nhập
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra thông tin đăng nhập từ Firestore trong collection acc
            db.collection("acc")
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                boolean found = false;
                                for (DocumentSnapshot document : task.getResult()) {
                                    String tk = document.getString("tk");
                                    String mk = document.getString("mk");
                                    String role = document.getString("role");

                                    if (tk != null && tk.equals(email) && mk != null && mk.equals(password)) {
                                        found = true;
                                        Toast.makeText(MainActivity.this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                                        if ("user".equals(role)) {
                                            Intent intent = new Intent(MainActivity.this, MainMenu.class);
                                            startActivity(intent);
                                        } else if ("admin".equals(role)) {
                                            Intent intent = new Intent(MainActivity.this, MainMenu.class);
                                            startActivity(intent);
                                        }
                                        finish();
                                        break;
                                    }
                                }
                                if (!found) {
                                    Toast.makeText(MainActivity.this, "Email hoặc mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Lỗi: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // Xử lý quên mật khẩu
        tvForgotPassword.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Liên hệ admin để lấy mật khẩu", Toast.LENGTH_SHORT).show();
        });
    }
}