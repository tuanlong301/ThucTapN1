package com.example.appbanhang.owner;

import android.app.AlertDialog;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Quản lý bàn / tài khoản:
 * - Hiển thị cả admin & user (admin/chuquan nổi bật, luôn đứng đầu danh sách)
 * - Thêm: tạo Auth user (tk/mk) qua secondary app, upsert acc/{uid}
 * - Sửa: đổi name, role, password (update cả Auth nếu đổi pass)
 * - Xoá: confirm rồi xoá doc acc (không xoá Auth)
 */
public class OwnerTablesFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvEmpty;
    private FloatingActionButton fab;

    private FirebaseFirestore db;
    private ListenerRegistration reg;

    private final TablesAdapter adapter = new TablesAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_tables, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);
        rv = v.findViewById(R.id.rvTables);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        fab = v.findViewById(R.id.fabAddTable);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        fab.setOnClickListener(view -> showAddDialog());
    }

    @Override
    public void onStart() {
        super.onStart();
        // KHÔNG lọc role → lấy hết rồi sắp xếp: admin/chuquan lên đầu, còn lại theo tên A→Z
        reg = db.collection("acc").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;

            List<TableDoc> list = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                list.add(new TableDoc(
                        d.getId(),
                        s(d.get("name")),
                        s(d.get("tk")),
                        s(d.get("mk")),
                        s(d.get("role")),
                        s(d.get("uid"))
                ));
            }

            // Sort: admin first, then name A→Z
            Collections.sort(list, new Comparator<TableDoc>() {
                @Override public int compare(TableDoc a, TableDoc b) {
                    boolean aa = isAdmin(a.role), bb = isAdmin(b.role);
                    if (aa != bb) return aa ? -1 : 1; // admin trước
                    String an = a.name == null ? "" : a.name;
                    String bn = b.name == null ? "" : b.name;
                    return an.compareToIgnoreCase(bn);
                }
            });

            adapter.submit(list);

            boolean empty = list.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            rv.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    /* ======================= Thêm ======================= */

    private void showAddDialog() {
        View form = LayoutInflater.from(getContext()).inflate(R.layout.dialog_three_fields, null, false);
        // Nếu bạn chưa có layout dialog_three_fields, dùng form tạo tay:
        if (form == null) {
            form = buildThreeFieldsForm("Tên bàn", "Tài khoản (email)", "Mật khẩu");
        }
        final EditText edtName, edtTk, edtMk;
        if (form.findViewById(R.id.edt1) != null) {
            edtName = form.findViewById(R.id.edt1);
            edtTk   = form.findViewById(R.id.edt2);
            edtMk   = form.findViewById(R.id.edt3);
            edtName.setHint("Tên bàn ");
            edtTk.setHint("Tài khoản (email)"); edtTk.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            edtMk.setHint("Mật khẩu"); edtMk.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                                     // edtMk.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        } else {

            final View f = form;
            edtName = f.findViewById(1);
            edtTk   = f.findViewById(2);
            edtMk   = f.findViewById(3);
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm tài khoản/bàn")
                .setView(form)
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Lưu", (d, w) -> {
                    String name = edtName.getText().toString().trim();
                    String tk   = edtTk.getText().toString().trim();
                    String mk   = edtMk.getText().toString().trim();
                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(tk) || TextUtils.isEmpty(mk)) {
                        toast("Nhập đủ tên, tài khoản, mật khẩu"); return;
                    }
                    // Kiểm tra trùng tk trong acc
                    db.collection("acc").whereEqualTo("tk", tk).limit(1).get()
                            .addOnSuccessListener(q -> {
                                if (!q.isEmpty()) { toast("Tài khoản đã tồn tại trong acc"); }
                                else { createAuthAndSave(name, tk, mk); }
                            })
                            .addOnFailureListener(err -> toast("Lỗi kiểm tra tk: " + err.getMessage()));
                })
                .show();
    }


    private View buildThreeFieldsForm(String h1, String h2, String h3) {
        androidx.appcompat.widget.LinearLayoutCompat root =
                new androidx.appcompat.widget.LinearLayoutCompat(requireContext());
        root.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad,pad,pad,pad);

        EditText e1 = new EditText(requireContext()); e1.setId(1); e1.setHint(h1); root.addView(e1);
        EditText e2 = new EditText(requireContext()); e2.setId(2); e2.setHint(h2); e2.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS); root.addView(e2);
        EditText e3 = new EditText(requireContext()); e3.setId(3); e3.setHint(h3); e3.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); root.addView(e3);
        return root;
    }

    private FirebaseApp getOrInitSecondaryApp() {
        FirebaseApp main = FirebaseApp.getInstance();
        FirebaseOptions opts = main.getOptions();
        try { return FirebaseApp.getInstance("auth-helper"); }
        catch (IllegalStateException ignore) { return FirebaseApp.initializeApp(requireContext(), opts, "auth-helper"); }
    }

    private void createAuthAndSave(String name, String email, String password) {
        final FirebaseApp secondary = getOrInitSecondaryApp();
        final FirebaseAuth auth = FirebaseAuth.getInstance(secondary);

        // 1) Tạo user
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(res -> {
                    String uid = res.getUser() != null ? res.getUser().getUid() : null;
                    upsertAccThenCleanup(uid, name, email, password, auth, secondary, /*isRestore*/false);
                })
                .addOnFailureListener(e -> {
                    // 2) Email đã tồn tại trong Auth -> đăng nhập bằng pass người dùng vừa nhập
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        auth.signInWithEmailAndPassword(email, password)
                                .addOnSuccessListener(res -> {
                                    String uid = res.getUser() != null ? res.getUser().getUid() : null;
                                    // đảm bảo password trong Auth = password mới (nếu trước đó khác)
                                    if (res.getUser() != null) {
                                        res.getUser().updatePassword(password)
                                                .addOnCompleteListener(x ->
                                                        upsertAccThenCleanup(uid, name, email, password, auth, secondary, /*isRestore*/true));
                                    } else {
                                        upsertAccThenCleanup(uid, name, email, password, auth, secondary, /*isRestore*/true);
                                    }
                                })
                                .addOnFailureListener(err -> {
                                    // Không biết mật khẩu hiện tại -> gửi email reset
                                    auth.sendPasswordResetEmail(email)
                                            .addOnSuccessListener(v -> toast("Email đã tồn tại. Đã gửi link đặt lại mật khẩu."))
                                            .addOnFailureListener(err2 -> toast("Email đã tồn tại trong Authentication"));
                                    cleanupSecondary(auth, secondary);
                                });
                    } else {
                        toast("Lỗi tạo Auth user: " + e.getMessage());
                        cleanupSecondary(auth, secondary);
                    }
                });
    }


    private void dedupeAccAfterUpsert(String uid, String tk) {
        db.collection("acc").whereEqualTo("tk", tk).get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        if (!d.getId().equals(uid)) db.collection("acc").document(d.getId()).delete();
                    }
                });
    }

    private void upsertAccThenCleanup(
            String uid, String name, String email, String password,
            FirebaseAuth auth, FirebaseApp secondary, boolean isRestore
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("tk", email);
        data.put("mk", password);   // plaintext theo yêu cầu
        data.put("role", "user");
        if (uid != null) data.put("uid", uid);

        if (uid != null) {
            db.collection("acc").document(uid).set(data)
                    .addOnSuccessListener(v -> {
                        toast(isRestore ? "Đã khôi phục tài khoản" : "Đã thêm");
                        dedupeAccAfterUpsert(uid, email);
                    })
                    .addOnFailureListener(err -> toast("Lỗi ghi acc: " + err.getMessage()))
                    .addOnCompleteListener(done -> cleanupSecondary(auth, secondary));
        } else {
            // fallback hiếm gặp
            db.collection("acc").add(data)
                    .addOnSuccessListener(v -> toast(isRestore ? "Đã khôi phục (không có uid)" : "Đã thêm"))
                    .addOnCompleteListener(done -> cleanupSecondary(auth, secondary));
        }
    }


    private void cleanupSecondary(FirebaseAuth auth, FirebaseApp app) {
        try { auth.signOut(); } catch (Exception ignore) {}
        try { app.delete();  } catch (Exception ignore) {}
    }

    /* ======================= Sửa / Xoá ======================= */

    private void showEditDialog(TableDoc t) {
        // form: name, role, password (email không cho đổi ở đây để đơn giản & tránh updateAuth email)
        View form = buildThreeFieldsForm("Tên", "Quyền (user/admin)", "Mật khẩu mới");
        final EditText edtName = form.findViewById(1);
        final EditText edtRole = form.findViewById(2);
        final EditText edtMk   = form.findViewById(3);
        edtName.setText(t.name);
        edtRole.setText(TextUtils.isEmpty(t.role) ? "user" : t.role);
        edtMk.setText(t.mk); // plaintext (theo yêu cầu)

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
                        toast("Nhập đủ tên/role/mật khẩu"); return;
                    }

                    Map<String,Object> up = new HashMap<>();
                    up.put("name", newName);
                    up.put("role", newRole);
                    boolean passChanged = !newPass.equals(t.mk);
                    if (passChanged) up.put("mk", newPass);

                    db.collection("acc").document(t.id).update(up)
                            .addOnSuccessListener(v -> {
                                if (passChanged) {
                                    updateAuthPassword(t.tk, t.mk, newPass);
                                } else {
                                    toast("Đã lưu");
                                }
                            })
                            .addOnFailureListener(err -> toast("Lỗi lưu: " + err.getMessage()));
                })
                .show();
    }

    /** Đăng nhập secondary bằng (email, oldPass) rồi updatePassword(newPass) */
    private void updateAuthPassword(String email, String oldPass, String newPass) {
        final FirebaseApp secondary = getOrInitSecondaryApp();
        final FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondary);

        secondaryAuth.signInWithEmailAndPassword(email, oldPass)
                .addOnSuccessListener(res -> {
                    if (res.getUser() == null) { toast("Không lấy được user để đổi mật khẩu"); cleanupSecondary(secondaryAuth, secondary); return; }
                    res.getUser().updatePassword(newPass)
                            .addOnSuccessListener(v -> toast("Đã đổi mật khẩu"))
                            .addOnFailureListener(e -> toast("Lỗi đổi mật khẩu Auth: " + e.getMessage()))
                            .addOnCompleteListener(done -> cleanupSecondary(secondaryAuth, secondary));
                })
                .addOnFailureListener(e -> {
                    toast("Không đăng nhập được user để đổi mật khẩu");
                    cleanupSecondary(secondaryAuth, secondary);
                });
    }

    private void confirmDelete(TableDoc t) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xoá tài khoản?")
                .setMessage("Xoá \"" + t.name + "\" (" + t.tk + ")? Thao tác này không thể hoàn tác.")
                .setNegativeButton("Huỷ", null)
                .setPositiveButton("Xoá", (d, w) -> {
                    db.collection("acc").document(t.id).delete()
                            .addOnSuccessListener(v -> toast("Đã xoá: " + t.name))
                            .addOnFailureListener(err -> toast("Lỗi xoá: " + err.getMessage()));
                })
                .show();
    }

    /* ======================= Adapter ======================= */

    private static class TableDoc {
        final String id, name, tk, mk, role, uid;
        TableDoc(String id, String name, String tk, String mk, String role, String uid) {
            this.id = id; this.name = name; this.tk = tk; this.mk = mk; this.role = role; this.uid = uid;
        }
    }

    private class TableVH extends RecyclerView.ViewHolder {
        final TextView tvName, tvStatus;
        final View btnEdit, btnDelete;
        TableVH(@NonNull View v) {
            super(v);
            tvName   = v.findViewById(R.id.tvTableName);
            tvStatus = v.findViewById(R.id.tvTableStatus);
            btnEdit  = v.findViewById(R.id.btnEdit);
            btnDelete= v.findViewById(R.id.btnDelete);
        }
    }

    private class TablesAdapter extends RecyclerView.Adapter<TableVH> {
        private final List<TableDoc> data = new ArrayList<>();
        void submit(List<TableDoc> list) { data.clear(); if (list != null) data.addAll(list); notifyDataSetChanged(); }

        @NonNull @Override public TableVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table_manage, parent, false);
            return new TableVH(v);
        }

        @Override public void onBindViewHolder(@NonNull TableVH h, int position) {
            TableDoc t = data.get(position);
            h.tvName.setText(t.name);

            String role = TextUtils.isEmpty(t.role) ? "user" : t.role;
            h.tvStatus.setText(t.tk + " • " + role);

            // Nổi bật admin/chuquan
            h.tvStatus.setTextColor(isAdmin(role) ? Color.parseColor("#E91E63") : Color.parseColor("#6B7280"));

            h.btnEdit.setOnClickListener(v -> showEditDialog(t));
            h.btnDelete.setOnClickListener(v -> confirmDelete(t));
        }

        @Override public int getItemCount() { return data.size(); }
    }

    /* ======================= Utils ======================= */

    private void toast(String msg) { if (getContext()!=null) Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show(); }
    private static String s(Object v) { return v == null ? "" : String.valueOf(v); }
    private static boolean isAdmin(String role) {
        return "admin".equalsIgnoreCase(role) || "king".equalsIgnoreCase(role);
    }
}
