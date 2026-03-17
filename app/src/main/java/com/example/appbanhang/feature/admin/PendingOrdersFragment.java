package com.example.appbanhang.feature.admin;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.example.appbanhang.feature.admin.adapter.OrderAdapter;

/**
 * View-only: observe PendingOrdersViewModel.
 */
public class PendingOrdersFragment extends Fragment {

    private PendingOrdersViewModel vm;
    private OrderAdapter adapter;
    private MediaPlayer tingPlayer;

    public static PendingOrdersFragment newInstance() { return new PendingOrdersFragment(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(OrderAdapter.Mode.PENDING, new OrderAdapter.OnAction() {
            @Override public void onConfirm(String id) { vm.confirmOrder(id); }
            @Override public void onCancel(String id) { showCancelDialog(id); }
            @Override public void onPay(String id) {}
            @Override public void onPrint(String id) {}
        });
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(PendingOrdersViewModel.class);

        // Observe orders
        vm.getOrders().observe(getViewLifecycleOwner(), orders -> {
            adapter.submit(orders);
            // Highlight from args
            Bundle args = getArguments();
            if (args != null) {
                String hlId = args.getString("highlight_id");
                if (hlId != null && !hlId.isEmpty()) {
                    int idx = adapter.indexOf(hlId);
                    if (idx >= 0) { rv.scrollToPosition(idx); adapter.highlight(hlId); }
                    args.remove("highlight_id");
                }
            }
        });

        // Observe new order sound
        vm.getHasNewOrder().observe(getViewLifecycleOwner(), hasNew -> {
            if (hasNew) { playTing(); vm.resetNewOrderFlag(); }
        });

        // Observe toasts
        vm.getToast().observe(getViewLifecycleOwner(), event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null && getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        vm.startListening();
    }

    private void showCancelDialog(String orderId) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        EditText etReason = new EditText(requireContext());
        etReason.setHint("Lý do hủy");
        etReason.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(etReason);

        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy đơn hàng")
                .setView(layout)
                .setNegativeButton("Đóng", null)
                .setPositiveButton("Hủy đơn", (d, w) -> {
                    String reason = etReason.getText().toString().trim();
                    if (reason.isEmpty()) reason = "Không rõ";
                    vm.cancelOrder(orderId, reason);
                })
                .show();
    }

    private void playTing() {
        try {
            if (tingPlayer != null) { tingPlayer.release(); tingPlayer = null; }
            tingPlayer = MediaPlayer.create(getContext(), R.raw.ting);
            if (tingPlayer != null) {
                tingPlayer.setOnCompletionListener(MediaPlayer::release);
                tingPlayer.start();
            }
        } catch (Exception ignore) {}
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        vm.stopListening();
        if (tingPlayer != null) { tingPlayer.release(); tingPlayer = null; }
    }
}
