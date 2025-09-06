package com.example.appbanhang;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TablesFragment extends Fragment {

    public static TablesFragment newInstance() { return new TablesFragment(); }

    private RecyclerView rv;
    private TableAdapter adapter;
    private final List<Row> rows = new ArrayList<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle s) {
        // Dùng fragment_orders.xml có sẵn RecyclerView id=rvGeneric
        View v = inf.inflate(R.layout.fragment_orders, parent, false);
        rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new TableAdapter(rows, new TableAdapter.OnClick() {
            @Override public void onClick(int pos) { onTableClick(rows.get(pos)); }
        });
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // Refresh từ AdminMenu
        getParentFragmentManager().setFragmentResultListener(
                "force_refresh", this, (k, b) -> loadAll()
        );

        loadAll();
        return v;
    }

    private void loadAll() {
        if (getContext() == null) return;
        rows.clear();
        adapter.notifyDataSetChanged();

        db.collection("acc")
                .whereEqualTo("role", "user")
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    final List<Task<?>> waits = new ArrayList<>();

                    for (DocumentSnapshot d : snap.getDocuments()) {
                        final String uid = d.getString("uid");
                        final String name = d.getString("name");
                        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(name)) continue;

                        final Row row = new Row();
                        row.userId = uid;
                        row.title = name;
                        row.status = "Trống";
                        row.sub = null;

                        // 1) Lấy đơn theo userId (không orderBy để tránh index) -> lọc client
                        final Task<QuerySnapshot> tOrder = db.collection("orders")
                                .whereEqualTo("userId", uid)
                                .limit(5) // đủ để chọn cái mới nhất phía client
                                .get();

                        // 2) Gọi NV gần nhất
                        final Task<QuerySnapshot> tCall = db.collection("staff_calls")
                                .whereEqualTo("userId", uid)
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .limit(1)
                                .get();

                        // 3) Trạng thái thủ công
                        final Task<DocumentSnapshot> tManual = db.collection("table_states")
                                .document(uid).get();

                        Task<?> batch = com.google.android.gms.tasks.Tasks.whenAll(tOrder, tCall, tManual)
                                .addOnSuccessListener(ignored -> {
                                    // Ưu tiên: Đơn > Gọi NV > Manual > Trống
                                    QuerySnapshot os = tOrder.getResult();
                                    DocumentSnapshot newestOrder = pickNewestOrder(os);
                                    if (newestOrder != null) {
                                        String st = newestOrder.getString("status");
                                        if ("pending".equals(st) || "confirmed".equals(st)) {
                                            row.status = "Đang sử dụng";
                                            Date createdAt = newestOrder.getDate("createdAt");
                                            if (createdAt != null) {
                                                row.sub = "Đặt lúc " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(createdAt);
                                            } else {
                                                String ts = newestOrder.getString("timestamp");
                                                if (!TextUtils.isEmpty(ts)) row.sub = "Đặt lúc " + safeCutHHmm(ts);
                                            }
                                        }
                                    }

                                    if ("Trống".equals(row.status)) {
                                        QuerySnapshot cs = tCall.getResult();
                                        if (cs != null && !cs.isEmpty()) {
                                            DocumentSnapshot cd = cs.getDocuments().get(0);

                                        }
                                    }

                                    if ("Trống".equals(row.status)) {
                                        DocumentSnapshot ms = tManual.getResult();
                                        if (ms != null && ms.exists()) {
                                            String st = ms.getString("status"); // empty/occupied/need_help/reserved
                                            if ("occupied".equals(st)) {
                                                row.status = "Đang sử dụng";
                                            } else if ("need_help".equals(st)) {
                                                row.status = "Đang sử dụng & cần trợ giúp";
                                            } else if ("reserved".equals(st)) {
                                                row.status = "Đã đặt trước";
                                                Date rt = ms.getDate("reservedAt");
                                                if (rt != null) {
                                                    row.sub = "Vào lúc " + new SimpleDateFormat("HH:mm dd/MM", Locale.getDefault()).format(rt);
                                                }
                                            } else {
                                                row.status = "Trống";
                                            }
                                        }
                                    }

                                    rows.add(row);
                                    adapter.notifyItemInserted(rows.size() - 1);
                                });

                        waits.add(batch);
                    }

                    com.google.android.gms.tasks.Tasks.whenAll(waits)
                            .addOnSuccessListener(ignored -> {
                                Collections.sort(rows, new Comparator<Row>() {
                                    @Override public int compare(Row a, Row b) {
                                        return a.title.compareToIgnoreCase(b.title);
                                    }
                                });
                                adapter.notifyDataSetChanged();
                            });
                })
                .addOnFailureListener(e -> toast("Lỗi tải bàn: " + e.getMessage()));
    }

    /** Chọn đơn mới nhất: prefer createdAt(Date), fallback timestamp(String ISO) */
    @Nullable
    private DocumentSnapshot pickNewestOrder(@Nullable QuerySnapshot os) {
        if (os == null || os.isEmpty()) return null;
        List<DocumentSnapshot> list = new ArrayList<>(os.getDocuments());
        Collections.sort(list, new Comparator<DocumentSnapshot>() {
            @Override public int compare(DocumentSnapshot a, DocumentSnapshot b) {
                Date da = a.getDate("createdAt");
                Date db = b.getDate("createdAt");
                if (da != null && db != null) return db.compareTo(da); // desc
                if (da != null) return -1;
                if (db != null) return 1;
                String sa = a.getString("timestamp");
                String sb = b.getString("timestamp");
                if (sa == null) sa = "";
                if (sb == null) sb = "";
                // so sánh chuỗi ISO (yyyy-MM-ddTHH:mm:ssZ) — tạm coi chuỗi dài hơn và “mới hơn”
                return sb.compareTo(sa);
            }
        });
        return list.get(0);
    }

    private String safeCutHHmm(String iso) {
        try {
            if (iso != null && iso.length() >= 16) return iso.substring(11, 16);
        } catch (Exception ignore) {}
        return iso;
    }

    private void onTableClick(final Row row) {
        if (getContext() == null) return;

        final String[] choices = new String[] {
                "Trống",
                "Đang sử dụng",
                "Đang sử dụng & cần trợ giúp",
                "Đã đặt trước lúc…"
        };

        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle(row.title)
                .setItems(choices, (dlg, which) -> {
                    if (which == 0) saveManualState(row.userId, "empty", null);
                    else if (which == 1) saveManualState(row.userId, "occupied", null);
                    else if (which == 2) saveManualState(row.userId, "need_help", null);
                    else pickDateTime(row.userId);
                })
                .show();
    }

    private void pickDateTime(final String userId) {
        if (getContext() == null) return;
        final Calendar cal = Calendar.getInstance();

        DatePickerDialog dp = new DatePickerDialog(
                getContext(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(DatePicker view, int y, int m, int d) {
                        cal.set(Calendar.YEAR, y);
                        cal.set(Calendar.MONTH, m);
                        cal.set(Calendar.DAY_OF_MONTH, d);

                        TimePickerDialog tp = new TimePickerDialog(
                                getContext(),
                                new TimePickerDialog.OnTimeSetListener() {
                                    @Override public void onTimeSet(TimePicker view, int h, int min) {
                                        cal.set(Calendar.HOUR_OF_DAY, h);
                                        cal.set(Calendar.MINUTE, min);
                                        cal.set(Calendar.SECOND, 0);
                                        saveManualState(userId, "reserved", cal.getTime());
                                    }
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true
                        );
                        tp.show();
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dp.show();
    }

    private void saveManualState(String uid, String status, @Nullable Date reservedAt) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", status);
        m.put("updatedAt", new Date());
        if ("reserved".equals(status)) m.put("reservedAt", reservedAt);
        else m.put("reservedAt", null);

        db.collection("table_states").document(uid)
                .set(m)
                .addOnSuccessListener(v -> { toast("Đã cập nhật"); loadAll(); })
                .addOnFailureListener(e -> toast("Lỗi lưu: " + e.getMessage()));
    }

    private void toast(String s) {
        if (getContext() != null) Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show();
    }

    // ===== Model & Adapter =====

    public static class Row {
        public String userId;
        public String title;
        public String status;
        public String sub;
    }

    public static class TableAdapter extends RecyclerView.Adapter<TableAdapter.VH> {
        interface OnClick { void onClick(int pos); }
        private final List<Row> data;
        private final OnClick cb;
        public TableAdapter(List<Row> data, OnClick cb) { this.data = data; this.cb = cb; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_table, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Row r = data.get(pos);
            h.tvTitle.setText(r.title);

            String state = r.status != null ? r.status : "Trống";
            if (!TextUtils.isEmpty(r.sub)) state = state + " • " + r.sub;
            h.tvState.setText("Trạng thái: " + state);

            int color;
            if ("Trống".equals(r.status)) color = 0xFF6B7280;
            else if ("Đang sử dụng".equals(r.status)) color = 0xFF0E9F6E;
            else if ("Đang sử dụng & cần trợ giúp".equals(r.status)) color = 0xFFE11D48;
            else if ("Đã đặt trước".equals(r.status)) color = 0xFF2563EB;
            else color = 0xFF6B7280;
            h.tvState.setTextColor(color);

            h.itemView.setOnClickListener(v -> {
                if (cb != null) cb.onClick(h.getBindingAdapterPosition());
            });
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            final android.widget.TextView tvTitle, tvState;
            VH(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvTableName);
                tvState = v.findViewById(R.id.tvTableStatus);
            }
        }
    }
}
