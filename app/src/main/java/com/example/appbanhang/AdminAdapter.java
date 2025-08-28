package com.example.appbanhang;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * ViewPager2 adapter cho Admin:
 * 0: PendingOrdersFragment (Đơn hàng)
 * 1: ConfirmedOrdersFragment (Đơn hàng đã xác nhận)
 * 2: TablesFragment (Quản lý bàn)
 *
 * highlightId: orderId vừa tạo để tab 0 highlight/scroll tới.
 */
public class AdminAdapter extends FragmentStateAdapter {

    @Nullable
    private final String highlightId;

    // Gọi từ AdminMenu: new AdminAdapter(this, highlightId);
    public AdminAdapter(@NonNull FragmentActivity fa, @Nullable String highlightId) {
        super(fa);
        this.highlightId = highlightId;
    }

    // Nếu bạn chưa cần highlightId, vẫn có thể giữ thêm 1 constructor rút gọn:
    public AdminAdapter(@NonNull FragmentActivity fa) {
        this(fa, null);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: { // Đơn hàng (pending)
                PendingOrdersFragment f = PendingOrdersFragment.newInstance();
                if (highlightId != null && !highlightId.isEmpty()) {
                    Bundle args = f.getArguments() != null ? f.getArguments() : new Bundle();
                    args.putString("highlight_id", highlightId);
                    f.setArguments(args);
                }
                return f;
            }
            case 1: // Đơn hàng đã xác nhận
                return ConfirmedOrdersFragment.newInstance();
            case 2: // Quản lý bàn
                return TablesFragment.newInstance();
            default:
                return PendingOrdersFragment.newInstance();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
