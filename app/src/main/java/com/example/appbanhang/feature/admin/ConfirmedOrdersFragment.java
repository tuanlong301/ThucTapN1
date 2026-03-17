package com.example.appbanhang.feature.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.example.appbanhang.common.utils.CurrencyUtils;
import com.example.appbanhang.common.utils.InvoiceUtils;
import com.example.appbanhang.data.model.Order;
import com.example.appbanhang.data.repository.OrderRepository;
import com.example.appbanhang.feature.admin.adapter.OrderAdapter;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * View-only: observe ConfirmedOrdersViewModel.
 */
public class ConfirmedOrdersFragment extends Fragment {

    private ConfirmedOrdersViewModel vm;
    private OrderAdapter adapter;

    public static ConfirmedOrdersFragment newInstance() { return new ConfirmedOrdersFragment(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(OrderAdapter.Mode.CONFIRMED, new OrderAdapter.OnAction() {
            @Override public void onConfirm(String id) {}
            @Override public void onCancel(String id) {}
            @Override public void onPay(String id) { showPayDialog(id); }
            @Override public void onPrint(String id) { doPrint(id); }
        });
        rv.setAdapter(adapter);

        vm = new ViewModelProvider(this).get(ConfirmedOrdersViewModel.class);

        vm.getOrders().observe(getViewLifecycleOwner(), adapter::submit);

        vm.getToast().observe(getViewLifecycleOwner(), event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null && getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        vm.startListening();
    }

    private void showPayDialog(String orderId) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận thanh toán")
                .setMessage("Đánh dấu đơn này đã thanh toán?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Đã thu tiền", (d, w) -> vm.markPaid(orderId))
                .show();
    }

    private void doPrint(String orderId) {
        vm.getOrder(orderId, new OrderRepository.ResultCallback<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot doc) {
                if (!isAdded() || getContext() == null) return;
                Order o = doc.toObject(Order.class);
                if (o == null) return;
                o.id = doc.getId();

                try {
                    String details = o.items != null ? o.items : "";
                    String total = o.total != null ? o.total : "0";
                    String tableName = o.name != null ? o.name : "";
                    InvoiceUtils.exportInvoiceToPdf(getContext(), o.id, tableName, details, total);
                    vm.markPrinted(orderId);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Lỗi in: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onError(String msg) {
                if (getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override public void onDestroyView() { super.onDestroyView(); vm.stopListening(); }
}
