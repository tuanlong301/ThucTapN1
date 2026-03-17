package com.example.appbanhang.feature.owner;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.example.appbanhang.data.model.Product;
import com.example.appbanhang.feature.owner.adapter.OwnerProductAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashMap;
import java.util.Map;

/**
 * View-only: observe OwnerProductsViewModel.
 */
public class OwnerProductsFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvEmpty;
    private OwnerProductAdapter adapter;
    private OwnerProductsViewModel vm;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_owner_products, container, false);

        rv = v.findViewById(R.id.rvProducts);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        FloatingActionButton fabAdd = v.findViewById(R.id.fabAddProduct);

        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new OwnerProductAdapter(new OwnerProductAdapter.OnAction() {
            @Override public void onClick(Product p) {}
            @Override public void onEdit(Product p) { showProductDialog(p); }
            @Override public void onDelete(Product p) { confirmDelete(p); }
        });
        rv.setAdapter(adapter);

        fabAdd.setOnClickListener(v1 -> showProductDialog(null));
        setupSearch(v);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        vm = new ViewModelProvider(this).get(OwnerProductsViewModel.class);

        vm.getFilteredProducts().observe(getViewLifecycleOwner(), list -> {
            adapter.setData(list);
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });

        vm.getToast().observe(getViewLifecycleOwner(), event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null && getContext() != null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        vm.startListening();
    }

    @Override public void onDestroyView() { super.onDestroyView(); vm.stopListening(); }

    // ========== Search ==========

    private void setupSearch(View root) {
        View possibleSearchView = root.findViewById(R.id.btnSearch);
        if (possibleSearchView instanceof SearchView) {
            SearchView sv = (SearchView) possibleSearchView;
            sv.setIconifiedByDefault(false);
            sv.setQueryHint("Tìm món theo tên/loại...");
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String q) { vm.setSearchQuery(q); return true; }
                @Override public boolean onQueryTextChange(String q) { vm.setSearchQuery(q); return true; }
            });
        }

        View possibleEt = root.findViewById(R.id.etSearch);
        if (possibleEt instanceof EditText) {
            ((EditText) possibleEt).addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { vm.setSearchQuery(s != null ? s.toString() : ""); }
            });
        }
    }

    // ========== Dialogs ==========

    private void showProductDialog(@Nullable Product editProduct) {
        boolean isEdit = editProduct != null;
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        scroll.addView(layout);

        EditText etName = addField(layout, "Tên món", InputType.TYPE_CLASS_TEXT);
        EditText etPrice = addField(layout, "Giá (đ)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText etCategory = addField(layout, "Loại (category)", InputType.TYPE_CLASS_TEXT);
        EditText etDesc = addField(layout, "Mô tả", InputType.TYPE_CLASS_TEXT);
        EditText etImage = addField(layout, "Image URL (http...)", InputType.TYPE_CLASS_TEXT);

        if (isEdit) {
            etName.setText(nullToEmpty(editProduct.getName()));
            etPrice.setText(editProduct.getPrice() != null ? String.valueOf(editProduct.getPrice()) : "");
            etCategory.setText(nullToEmpty(editProduct.getCategory()));
            etDesc.setText(nullToEmpty(editProduct.getDescription()));
            etImage.setText(nullToEmpty(editProduct.getImageUrl()));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? "Sửa món" : "Thêm món")
                .setView(scroll)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            if (name.isEmpty()) { etName.setError("Nhập tên món"); return; }

            double price = 0;
            if (!priceStr.isEmpty()) {
                try { price = Double.parseDouble(priceStr); }
                catch (NumberFormatException ex) { etPrice.setError("Giá không hợp lệ"); return; }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("price", price);
            data.put("category", etCategory.getText().toString().trim());
            data.put("description", etDesc.getText().toString().trim());
            data.put("imageUrl", etImage.getText().toString().trim());

            if (isEdit) vm.updateProduct(editProduct.getId(), data);
            else vm.addProduct(data);
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void confirmDelete(@NonNull Product p) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa món")
                .setMessage("Bạn chắc muốn xóa \"" + nullToEmpty(p.getName()) + "\"?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> vm.deleteProduct(p.getId()))
                .show();
    }

    private EditText addField(LinearLayout parent, String hint, int inputType) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setInputType(inputType);
        parent.addView(et);
        return et;
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
