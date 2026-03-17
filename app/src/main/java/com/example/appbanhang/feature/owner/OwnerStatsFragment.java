package com.example.appbanhang.feature.owner;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * View-only: observe OwnerStatsViewModel.
 */
public class OwnerStatsFragment extends Fragment {

    private Button btnToday, btnThisWeek, btnThisMonth, btnFromDate, btnToDate, btnApply;
    private TextView tvRevenue, tvOrderCount, tvAvg;
    private RecyclerView rvTopItems;
    private OwnerStatsViewModel vm;
    private OrdersAdapter ordersAdapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        btnToday = v.findViewById(R.id.btnToday);
        btnThisWeek = v.findViewById(R.id.btnThisWeek);
        btnThisMonth = v.findViewById(R.id.btnThisMonth);
        btnFromDate = v.findViewById(R.id.btnFromDate);
        btnToDate = v.findViewById(R.id.btnToDate);
        btnApply = v.findViewById(R.id.btnApply);
        tvRevenue = v.findViewById(R.id.tvRevenue);
        tvOrderCount = v.findViewById(R.id.tvOrderCount);
        tvAvg = v.findViewById(R.id.tvAvg);
        rvTopItems = v.findViewById(R.id.rvTopItems);

        rvTopItems.setLayoutManager(new LinearLayoutManager(getContext()));
        ordersAdapter = new OrdersAdapter();
        rvTopItems.setAdapter(ordersAdapter);

        vm = new ViewModelProvider(this).get(OwnerStatsViewModel.class);

        // Observe stats
        vm.getStats().observe(getViewLifecycleOwner(), stats -> {
            tvRevenue.setText(OwnerStatsViewModel.formatVND(stats.revenue));
            tvOrderCount.setText(String.valueOf(stats.orderCount));
            tvAvg.setText(OwnerStatsViewModel.formatVND(stats.avg));
            ordersAdapter.submit(stats.rows);
        });

        // Date buttons text
        updateDateLabels();

        // Clicks
        btnToday.setOnClickListener(x -> { vm.setToday(); updateDateLabels(); vm.applyFilter(); });
        btnThisWeek.setOnClickListener(x -> { vm.setThisWeek(); updateDateLabels(); vm.applyFilter(); });
        btnThisMonth.setOnClickListener(x -> { vm.setThisMonth(); updateDateLabels(); vm.applyFilter(); });
        btnFromDate.setOnClickListener(x -> showDatePicker(true));
        btnToDate.setOnClickListener(x -> showDatePicker(false));
        btnApply.setOnClickListener(x -> vm.applyFilter());

        vm.applyFilter();
    }

    private void updateDateLabels() {
        btnFromDate.setText(android.text.format.DateFormat.format("dd/MM/yyyy", vm.getFromCal()));
        btnToDate.setText(android.text.format.DateFormat.format("dd/MM/yyyy", vm.getToCal()));
    }

    private void showDatePicker(boolean isFrom) {
        Calendar c = isFrom ? vm.getFromCal() : vm.getToCal();
        new DatePickerDialog(requireContext(), (dp, y, m, d) -> {
            if (isFrom) vm.setFromDate(y, m, d); else vm.setToDate(y, m, d);
            updateDateLabels();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ========== Inner Adapter (pure UI) ==========

    private static class OrderVH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvSub, tvTime, tvMoney;
        OrderVH(@NonNull View root, TextView t1, TextView s1, TextView t2, TextView m2) {
            super(root); tvTitle = t1; tvSub = s1; tvTime = t2; tvMoney = m2;
        }
    }

    private class OrdersAdapter extends RecyclerView.Adapter<OrderVH> {
        private final List<OwnerStatsViewModel.OrderRow> data = new ArrayList<>();

        void submit(List<OwnerStatsViewModel.OrderRow> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public OrderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout root = new LinearLayout(parent.getContext());
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setPadding(24, 18, 24, 18);
            root.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout left = new LinearLayout(parent.getContext());
            left.setOrientation(LinearLayout.VERTICAL);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView title = new TextView(parent.getContext());
            title.setTextSize(16);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
            TextView sub = new TextView(parent.getContext());
            sub.setTextSize(14);
            left.addView(title); left.addView(sub);

            LinearLayout right = new LinearLayout(parent.getContext());
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);
            right.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView time = new TextView(parent.getContext());
            time.setTextSize(13);
            TextView money = new TextView(parent.getContext());
            money.setTextSize(15);
            money.setTypeface(money.getTypeface(), android.graphics.Typeface.BOLD);
            right.addView(time); right.addView(money);

            root.addView(left); root.addView(right);
            return new OrderVH(root, title, sub, time, money);
        }

        @Override public void onBindViewHolder(@NonNull OrderVH h, int position) {
            OwnerStatsViewModel.OrderRow r = data.get(position);
            h.tvTitle.setText(r.titleLeft);
            h.tvSub.setText(r.subLeft.isEmpty() ? "(Không có món)" : r.subLeft);
            h.tvTime.setText(OwnerStatsViewModel.fmtTime(r.timeMillis));
            h.tvMoney.setText(OwnerStatsViewModel.formatVND(r.total));
        }

        @Override public int getItemCount() { return data.size(); }
    }
}
