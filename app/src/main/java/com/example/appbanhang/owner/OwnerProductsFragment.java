package com.example.appbanhang.owner;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.example.appbanhang.model.Product;
import com.example.appbanhang.owner.adapter.OwnerProductAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Quản lý món: Hiển thị + Thêm/Sửa/Xóa + Tìm kiếm (name + category) */
public class OwnerProductsFragment extends Fragment {

    // Views
    private RecyclerView rv;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;

    // Optional search widgets (nếu có trong layout)
    private SearchView searchView;    // @id/searchView
    private EditText etSearch;        // @id/etSearch

    // Data
    private final List<Product> all = new ArrayList<>();   // toàn bộ từ Firestore
    private String currentQuery = "";                      // query đang lọc

    // Adapter + Firestore
    private OwnerProductAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration registration;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_owner_products, container, false);

        rv = v.findViewById(R.id.rvProducts);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        fabAdd = v.findViewById(R.id.fabAddProduct);


        View possibleSearchView = v.findViewById(R.id.btnSearch);
        if (possibleSearchView instanceof SearchView) {
            searchView = (SearchView) possibleSearchView;
        }
        View possibleEt = v.findViewById(R.id.etSearch);
        if (possibleEt instanceof EditText) {
            etSearch = (EditText) possibleEt;
        }

        GridLayoutManager glm = new GridLayoutManager(getContext(), 3);
        rv.setLayoutManager(glm);

        adapter = new OwnerProductAdapter(new OwnerProductAdapter.OnAction() {
            @Override public void onClick(Product p) {
                // Optional: mở chi tiết nếu bạn muốn
            }

            @Override public void onEdit(Product p) {
                showProductDialog(p);
            }

            @Override public void onDelete(Product p) {
                confirmDelete(p);
            }
        });
        rv.setAdapter(adapter);

        // FAB thêm mới
        fabAdd.setOnClickListener(v1 -> showProductDialog(null));

        // Search handler (nếu có)
        setupSearchHandlers();

        db = FirebaseFirestore.getInstance();
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        // Lắng nghe realtime collection "food_001"
        registration = db.collection("food_001")
                .addSnapshotListener((snap, e) -> {
                    if (!isAdded()) return;
                    all.clear();

                    if (e != null || snap == null) {
                        renderFiltered();
                        return;
                    }
                    for (DocumentSnapshot d : snap) {
                        Product p = d.toObject(Product.class);
                        if (p != null) {
                            p.setId(d.getId());
                            all.add(p);
                        }
                    }
                    renderFiltered();
                });
    }

    @Override public void onStop() {
        super.onStop();
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    // ============== Search ==============

    private void setupSearchHandlers() {
        if (searchView != null) {
            searchView.setIconifiedByDefault(false);
            searchView.setQueryHint("Tìm món theo tên/loại...");
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override public boolean onQueryTextSubmit(String query) {
                    currentQuery = safeLower(query);
                    renderFiltered();
                    return true;
                }
                @Override public boolean onQueryTextChange(String newText) {
                    currentQuery = safeLower(newText);
                    renderFiltered();
                    return true;
                }
            });
        }

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    currentQuery = safeLower(s == null ? "" : s.toString());
                    renderFiltered();
                }
            });
        }
    }

    private void renderFiltered() {
        List<Product> filtered = new ArrayList<>();
        if (TextUtils.isEmpty(currentQuery)) {
            filtered.addAll(all);
        } else {
            for (Product p : all) {
                String name = safeLower(p.getName());
                String cat  = safeLower(p.getCategory());
                if ((name != null && name.contains(currentQuery)) ||
                        (cat  != null && cat.contains(currentQuery))) {
                    filtered.add(p);
                }
            }
        }
        adapter.setData(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private static String safeLower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT).trim();
    }

    // ============== Dialog Add/Edit ==============

    /** Dialog thêm (editProduct == null) hoặc sửa (editProduct != null). */
    private void showProductDialog(@Nullable Product editProduct) {
        boolean isEdit = (editProduct != null);

        // Tạo layout dọc đơn giản cho dialog (khỏi cần XML)
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        final EditText etName = new EditText(requireContext());
        etName.setHint("Tên món");
        layout.addView(etName);

        final EditText etPrice = new EditText(requireContext());
        etPrice.setHint("Giá (đ)");
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(etPrice);

        final EditText etCategory = new EditText(requireContext());
        etCategory.setHint("Loại (category)");
        layout.addView(etCategory);

        final EditText etDesc = new EditText(requireContext());
        etDesc.setHint("Mô tả");
        etDesc.setMaxLines(4);
        layout.addView(etDesc);

        final EditText etImage = new EditText(requireContext());
        etImage.setHint("Image URL (http...)");
        layout.addView(etImage);

        if (isEdit) {
            etName.setText(nullToEmpty(editProduct.getName()));
            etPrice.setText(editProduct.getPrice() == null ? "" : String.valueOf(editProduct.getPrice()));
            etCategory.setText(nullToEmpty(editProduct.getCategory()));
            etDesc.setText(nullToEmpty(editProduct.getDescription()));
            etImage.setText(nullToEmpty(editProduct.getImageUrl()));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(isEdit ? "Sửa món" : "Thêm món")
                .setView(layout)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(dlg -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();
            String category = etCategory.getText().toString().trim();
            String desc = etDesc.getText().toString().trim();
            String imageUrl = etImage.getText().toString().trim();

            if (name.isEmpty()) {
                etName.setError("Nhập tên món");
                etName.requestFocus();
                return;
            }

            double price = 0;
            if (!priceStr.isEmpty()) {
                try { price = Double.parseDouble(priceStr); }
                catch (NumberFormatException ex) {
                    etPrice.setError("Giá không hợp lệ");
                    etPrice.requestFocus();
                    return;
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("price", price);
            data.put("category", category);
            data.put("description", desc);
            data.put("imageUrl", imageUrl);

            if (isEdit) {
                FirebaseFirestore.getInstance()
                        .collection("food_001")
                        .document(editProduct.getId())
                        .update(data)
                        .addOnSuccessListener(x -> {
                            Toast.makeText(getContext(), "Đã lưu", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } else {
                FirebaseFirestore.getInstance()
                        .collection("food_001")
                        .add(data)
                        .addOnSuccessListener(x -> {
                            Toast.makeText(getContext(), "Đã thêm", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }));
        dialog.show();
    }

    // ============== Delete ==============

    private void confirmDelete(@NonNull Product p) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa món")
                .setMessage("Bạn chắc muốn xóa \"" + nullToEmpty(p.getName()) + "\"?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) ->
                        FirebaseFirestore.getInstance()
                                .collection("food_001")
                                .document(p.getId())
                                .delete()
                                .addOnSuccessListener(x -> Toast.makeText(getContext(), "Đã xóa", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show())
                ).show();
    }

    private static String nullToEmpty(String s) { return s == null ? "" : s; }
}
