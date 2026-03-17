package com.example.appbanhang.feature.user;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.common.base.BaseActivity;
import com.example.appbanhang.R;
import com.example.appbanhang.common.network.NetworkMonitor;
import com.example.appbanhang.common.ui.Dialogs;
import com.example.appbanhang.common.utils.Constants;
import com.example.appbanhang.feature.user.adapter.CartAdapter;

/**
 * View-only: observe LiveData từ CartViewModel.
 */
public class CartActivity extends BaseActivity {

    private RecyclerView rv;
    private View emptyView, footerBar;
    private TextView tvTotal;
    private Button btnOrder;
    private CartAdapter cartAdapter;
    private CartViewModel vm;

    private final NetworkMonitor.Listener netListener = ok -> { if (ok) vm.loadCart(); };

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_cart);

        rv = findViewById(R.id.rvCart);
        emptyView = findViewById(R.id.viewEmpty);
        footerBar = findViewById(R.id.footerBar);
        tvTotal = findViewById(R.id.tvTotal);
        btnOrder = findViewById(R.id.btnCheckout);

        rv.setLayoutManager(new LinearLayoutManager(this));

        vm = new ViewModelProvider(this).get(CartViewModel.class);

        // Observe cart items
        vm.getCartItems().observe(this, items -> {
            cartAdapter = new CartAdapter(this, items, new CartAdapter.OnQtyClick() {
                @Override public void onPlus(int pos) {
                    if (!requireOnline()) return;
                    vm.changeQty(pos, +1);
                }
                @Override public void onMinus(int pos) {
                    if (!requireOnline()) return;
                    vm.changeQty(pos, -1);
                }
            });
            rv.setAdapter(cartAdapter);
        });

        // Observe total
        vm.getTotalText().observe(this, tvTotal::setText);

        // Observe empty state
        vm.getCartEmpty().observe(this, empty -> {
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            rv.setVisibility(empty ? View.GONE : View.VISIBLE);
            footerBar.setVisibility(empty ? View.GONE : View.VISIBLE);
            btnOrder.setEnabled(!empty);
            btnOrder.setAlpha(empty ? 0.5f : 1f);
        });

        // Observe messages — professional error dialogs
        vm.getMessage().observe(this, event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) {
                Dialogs.error(this, msg);
            }
        });

        // Observe order success
        vm.getOrderSuccess().observe(this, event -> {
            Boolean ok = event.getContentIfNotHandled();
            if (ok != null && ok) {
                Dialogs.success(this, "Đặt hàng thành công!\nĐơn hàng đang được xử lý.",
                        (d, w) -> vm.clearCart());
            }
        });

        // Buttons
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnOrder.setOnClickListener(v -> onCheckout());

        // Load
        vm.loadCart();
        vm.refreshCachedName(name ->
                getSharedPreferences("app", MODE_PRIVATE).edit().putString("profile_name", name).apply());
    }

    @Override protected void onStart() { super.onStart(); NetworkMonitor.get(this).addListener(netListener); }
    @Override protected void onStop() { super.onStop(); NetworkMonitor.get(this).removeListener(netListener); }

    private void onCheckout() {
        if (!requireOnline()) return;

        String note = cartAdapter != null ? cartAdapter.getNote() : "";
        RadioGroup rg = findViewById(R.id.rgPaymentMethod);
        if (rg == null) {
            Dialogs.error(this, "Không thể tìm thấy phương thức thanh toán.\nVui lòng khởi động lại ứng dụng.");
            return;
        }

        int checkedId = rg.getCheckedRadioButtonId();
        String paymentMethod = (checkedId == R.id.rbCash) ? Constants.PM_CASH
                : (checkedId == R.id.rbTransfer) ? Constants.PM_TRANSFER : null;

        if (paymentMethod == null) {
            Dialogs.info(this, "💳 Chưa chọn thanh toán",
                    "Vui lòng chọn phương thức thanh toán trước khi đặt hàng.");
            return;
        }

        String cachedName = getSharedPreferences("app", MODE_PRIVATE).getString("profile_name", null);

        // Xác nhận trước khi đặt
        Dialogs.confirm(this,
                "🛒 Xác nhận đặt hàng",
                "Bạn chắc chắn muốn đặt đơn này?",
                (d, w) -> vm.checkout(note, paymentMethod, cachedName));
    }

    public void togglePaymentMethod(View v) {
        View panel = findViewById(R.id.layoutPaymentMethod);
        if (panel == null) return;
        panel.setVisibility(panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }
}
