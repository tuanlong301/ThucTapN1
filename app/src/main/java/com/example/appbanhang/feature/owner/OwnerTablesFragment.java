package com.example.appbanhang.feature.owner;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.example.appbanhang.data.repository.TableRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * View-only: observe OwnerTablesViewModel.
 */
public class OwnerTablesFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvEmpty;
    private OwnerTablesViewModel vm;
    private final AccountsAdapter accountsAdapter = new AccountsAdapter();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_tables, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        rv = v.findViewById(R.id.rvTables);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        FloatingActionButton fab = v.findViewById(R.id.fabAddTable);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        rv.setAdapter(accountsAdapter);

        vm = new ViewModelProvider(this).get(OwnerTablesViewModel.class);

        vm.getAccounts().observe(getViewLifecycleOwner(), list -> {
            accountsAdapter.submit(list);
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            rv.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
        });

        vm.getToast().observe(getViewLifecycleOwner(), event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null && getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        fab.setOnClickListener(view -> showAddDialog());
        vm.startListening();
    }

    @Override public void onDestroyView() { super.onDestroyView(); vm.stopListening(); }

    // ========== Dialogs ==========

    private void showAddDialog() {
        View form = buildForm("Tên bàn", "Tài khoản (email)", "Mật khẩu");
        EditText edtName = form.findViewById(1), edtTk = form.findViewById(2), edtMk = form.findViewById(3);

        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm tài khoản/bàn")
                .setView(form)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = edtName.getText().toString().trim();
                    String tk = edtTk.getText().toString().trim();
                    String mk = edtMk.getText().toString().trim();
                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(tk) || TextUtils.isEmpty(mk)) {
                        Toast.makeText(getContext(), "Nhập đủ tên, tài khoản, mật khẩu", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    vm.addAccount(requireContext(), name, tk, mk);
                }).show();
    }

    private void showEditDialog(TableRepository.AccountDoc t) {
        View form = buildForm("Tên", "Quyền (user/admin)", "Mật khẩu mới");
        EditText edtName = form.findViewById(1), edtRole = form.findViewById(2), edtMk = form.findViewById(3);
        edtName.setText(t.name);
        edtRole.setText(TextUtils.isEmpty(t.role) ? "user" : t.role);
        edtMk.setText(t.mk);

        new AlertDialog.Builder(requireContext())
                .setTitle("Sửa tài khoản")
                .setView(form)
                .setNegativeButton("Đóng", null)
                .setNeutralButton("Xoá", (d, w) -> confirmDelete(t))
                .setPositiveButton("Lưu", (d, w) -> {
                    String newName = edtName.getText().toString().trim();
                    String newRole = edtRole.getText().toString().trim();
                    String newPass = edtMk.getText().toString().trim();
                    if (TextUtils.isEmpty(newName) || TextUtils.isEmpty(newRole) || TextUtils.isEmpty(newPass)) {
                        Toast.makeText(getContext(), "Nhập đủ tên/role/mật khẩu", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> up = new HashMap<>();
                    up.put("name", newName);
                    up.put("role", newRole);
                    boolean passChanged = !newPass.equals(t.mk);
                    if (passChanged) up.put("mk", newPass);

                    vm.updateAccount(t.id, up);
                    if (passChanged) vm.updateAuthPassword(requireContext(), t.tk, t.mk, newPass);
                }).show();
    }

    private void confirmDelete(TableRepository.AccountDoc t) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xoá tài khoản?")
                .setMessage("Xoá \"" + t.name + "\" (" + t.tk + ")? Thao tác này không thể hoàn tác.")
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá", (d, w) -> vm.deleteAccount(t.id))
                .show();
    }

    private View buildForm(String h1, String h2, String h3) {
        androidx.appcompat.widget.LinearLayoutCompat root =
                new androidx.appcompat.widget.LinearLayoutCompat(requireContext());
        root.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        EditText e1 = new EditText(requireContext()); e1.setId(1); e1.setHint(h1); root.addView(e1);
        EditText e2 = new EditText(requireContext()); e2.setId(2); e2.setHint(h2); e2.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); root.addView(e2);
        EditText e3 = new EditText(requireContext()); e3.setId(3); e3.setHint(h3); e3.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); root.addView(e3);
        return root;
    }

    // ========== Inner Adapter ==========

    private class AccountVH extends RecyclerView.ViewHolder {
        final TextView tvName, tvStatus;
        final View btnEdit, btnDelete;
        AccountVH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvTableName);
            tvStatus = v.findViewById(R.id.tvTableStatus);
            btnEdit = v.findViewById(R.id.btnEdit);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }

    private class AccountsAdapter extends RecyclerView.Adapter<AccountVH> {
        private final List<TableRepository.AccountDoc> data = new ArrayList<>();

        void submit(List<TableRepository.AccountDoc> list) {
            data.clear();
            if (list != null) data.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public AccountVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table_manage, parent, false);
            return new AccountVH(v);
        }

        @Override public void onBindViewHolder(@NonNull AccountVH h, int position) {
            TableRepository.AccountDoc t = data.get(position);
            h.tvName.setText(t.name);
            String role = TextUtils.isEmpty(t.role) ? "user" : t.role;
            h.tvStatus.setText(t.tk + " • " + role);
            h.tvStatus.setTextColor(isAdmin(role) ? Color.parseColor("#E91E63") : Color.parseColor("#6B7280"));
            h.btnEdit.setOnClickListener(v -> showEditDialog(t));
            h.btnDelete.setOnClickListener(v -> confirmDelete(t));
        }

        @Override public int getItemCount() { return data.size(); }
    }

    private static boolean isAdmin(String role) {
        return "admin".equalsIgnoreCase(role) || "king".equalsIgnoreCase(role);
    }
}
