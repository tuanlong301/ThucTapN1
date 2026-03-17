package com.example.appbanhang.feature.user;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.common.base.BaseActivity;
import com.example.appbanhang.R;
import com.example.appbanhang.common.utils.Constants;
import com.example.appbanhang.feature.user.adapter.ProductAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;

/**
 * View-only: observe LiveData từ MenuViewModel.
 * Chỉ import FirebaseAuth để lấy uid. Toàn bộ Firestore logic nằm trong ViewModel/Repository.
 */
public class MenuActivity extends BaseActivity {

    private RecyclerView rvProducts;
    private ProductAdapter adapter;
    private TextView tvCartBadge;
    private MenuViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCartBadge = findViewById(R.id.tvCartBadge);
        rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 3));
        adapter = new ProductAdapter(this, new ArrayList<>());
        rvProducts.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(MenuViewModel.class);

        // ========== Search ==========
        EditText etSearch = findViewById(R.id.etSearch);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    vm.search(s.toString());
                }
            });
        }

        // Observe products
        vm.getProducts().observe(this, products -> {
            adapter.updateData(products);
            adapter.setOnAddToCartListener(p -> {
                String uid = getUid();
                if (uid == null || !requireOnline()) return;
                vm.addToCart(uid, p);
            });
        });

        // Observe cart badge — with bounce animation
        vm.getCartCount().observe(this, count -> {
            if (tvCartBadge == null) return;
            if (count <= 0) {
                tvCartBadge.setVisibility(View.GONE);
            } else {
                tvCartBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                tvCartBadge.setVisibility(View.VISIBLE);
                // Bounce animation like Shopee
                tvCartBadge.setScaleX(0.5f);
                tvCartBadge.setScaleY(0.5f);
                tvCartBadge.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(300)
                        .setInterpolator(new OvershootInterpolator(2f))
                        .start();
            }
        });

        // Observe toasts
        vm.getToast().observe(this, event -> {
            String msg = event.getContentIfNotHandled();
            if (msg == null) return;
            if ("__MAX_QTY__".equals(msg)) {
                new AlertDialog.Builder(this)
                        .setTitle("⚠️ Giới hạn số lượng")
                        .setMessage("Số lượng tối đa là " + Constants.MAX_QTY + " cho mỗi món.\nNếu muốn đặt nhiều hơn, vui lòng liên hệ nhân viên.")
                        .setPositiveButton("Đã hiểu", null).show();
            } else {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe call staff
        vm.getCallStaffResult().observe(this, event -> {
            MenuViewModel.CallStaffResult r = event.getContentIfNotHandled();
            if (r == null) return;
            switch (r) {
                case SUCCESS:
                    new AlertDialog.Builder(this)
                            .setTitle("✅ Đã gửi yêu cầu")
                            .setMessage("Đã thông báo cho nhân viên.\nVui lòng đợi trong giây lát.")
                            .setPositiveButton("OK", null).show();
                    break;
                case SPAM:
                    new AlertDialog.Builder(this)
                            .setTitle("⏳ Vui lòng đợi")
                            .setMessage("Bạn đã gọi nhân viên gần đây.\nVui lòng chờ tối đa 2 phút trước khi gọi lại.")
                            .setPositiveButton("Đã hiểu", null).show();
                    break;
                case OFFLINE:
                    new AlertDialog.Builder(this)
                            .setTitle("📡 Mất kết nối")
                            .setMessage("Không thể gửi yêu cầu khi mất mạng.\nVui lòng kết nối lại internet.")
                            .setPositiveButton("OK", null).show();
                    break;
                case ERROR: break; // toast already shown
            }
        });

        // Button clicks
        findViewById(R.id.btnCallStaff).setOnClickListener(v -> {
            String uid = getUid();
            if (uid != null) vm.callStaff(uid, requireOnline());
        });

        findViewById(R.id.btnCart).setOnClickListener(v ->
                startActivity(new android.content.Intent(this, CartActivity.class)));

        // Category clicks
        setupCategoryClick(R.id.btnBestSeller, Constants.CAT_BESTSELLER);
        setupCategoryClick(R.id.btnkm, Constants.CAT_KM);
        setupCategoryClick(R.id.btnCM, Constants.CAT_CM);
        setupCategoryClick(R.id.btnNuoc, Constants.CAT_NUOC);

        // Init data
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            vm.signInAnonymously(() -> initData());
        } else {
            initData();
        }
    }

    private void initData() {
        String uid = getUid();
        if (uid != null) vm.startListenCartCount(uid);
        vm.loadAll();
    }

    @Override
    protected void onResume() {
        super.onResume();
        vm.loadAll(); // Tải lại khi quay về (owner có thể đã thêm món mới)
    }

    @Override
    protected void onStop() {
        super.onStop();
        vm.stopListenCartCount();
    }

    private void setupCategoryClick(int viewId, String category) {
        View btn = findViewById(viewId);
        if (btn != null) btn.setOnClickListener(v -> vm.loadByCategory(category));
    }

    private String getUid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }
}
