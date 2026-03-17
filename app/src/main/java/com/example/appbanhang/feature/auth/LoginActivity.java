package com.example.appbanhang.feature.auth;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.appbanhang.R;
import com.example.appbanhang.common.base.BaseActivity;
import com.example.appbanhang.common.utils.Constants;
import com.example.appbanhang.feature.admin.AdminMenuActivity;
import com.example.appbanhang.feature.owner.OwnerMenuActivity;
import com.example.appbanhang.feature.user.MenuActivity;

/**
 * View-only: observer LiveData từ LoginViewModel.
 * Kế thừa BaseActivity để có network monitoring + fullscreen.
 */
public class LoginActivity extends BaseActivity {

    private EditText edEmail, edPass;
    private Button btnLogin;
    private LoginViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        edEmail = findViewById(R.id.etEmail);
        edPass = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        vm = new ViewModelProvider(this).get(LoginViewModel.class);

        // Observe loading
        vm.getLoading().observe(this, loading -> btnLogin.setEnabled(!loading));

        // Observe result
        vm.getResult().observe(this, event -> {
            LoginViewModel.LoginResult r = event.getContentIfNotHandled();
            if (r == null) return;

            if (r.success) {
                SharedPreferences sp = getSharedPreferences("app", MODE_PRIVATE);
                sp.edit().putString("profile_name", r.name)
                         .putString("profile_role", r.role).apply();

                Class<?> target;
                if (Constants.ROLE_ADMIN.equalsIgnoreCase(r.role)) {
                    target = AdminMenuActivity.class;
                } else if (Constants.ROLE_OWNER.equalsIgnoreCase(r.role)) {
                    target = OwnerMenuActivity.class;
                } else {
                    target = MenuActivity.class;
                }
                startActivity(new Intent(this, target));
                finish();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Lỗi đăng nhập")
                        .setMessage(r.error)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });

        btnLogin.setOnClickListener(v -> {
            String email = edEmail.getText().toString().trim();
            String pass = edPass.getText().toString().trim();
            vm.login(email, pass);
        });
    }
}
