package com.example.appbanhang.feature.admin;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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
import com.example.appbanhang.common.utils.Constants;
import com.example.appbanhang.feature.admin.adapter.TableAdapter;

import java.util.Calendar;
import java.util.Date;

/**
 * View-only: observe TablesViewModel.
 */
public class TablesFragment extends Fragment {

    private TablesViewModel vm;
    private TableAdapter tableAdapter;

    public static TablesFragment newInstance() { return new TablesFragment(); }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        tableAdapter = new TableAdapter();
        tableAdapter.setOnTableClick(this::showTableDialog);
        rv.setAdapter(tableAdapter);

        vm = new ViewModelProvider(this).get(TablesViewModel.class);

        vm.getTables().observe(getViewLifecycleOwner(), tableAdapter::submit);

        vm.getToast().observe(getViewLifecycleOwner(), event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null && getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        vm.loadTables();
    }

    private void showTableDialog(TableAdapter.TableRow row) {
        String[] options = {"Đang sử dụng", "Trống", "Đặt trước"};
        new AlertDialog.Builder(requireContext())
                .setTitle(row.name)
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0: vm.saveState(row.userId, Constants.TABLE_OCCUPIED, null); break;
                        case 1: vm.saveState(row.userId, Constants.TABLE_AVAILABLE, null); break;
                        case 2: pickDateTimeForReservation(row.userId); break;
                    }
                }).show();
    }

    private void pickDateTimeForReservation(String uid) {
        Calendar now = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (dp, y, m, d) ->
                new TimePickerDialog(requireContext(), (tp, h, min) -> {
                    Calendar cal = Calendar.getInstance();
                    cal.set(y, m, d, h, min, 0);
                    vm.saveState(uid, "reserved", cal.getTime());
                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show(),
                now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
    }
}
