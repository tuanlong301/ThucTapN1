package com.example.appbanhang;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.*;

public class TablesFragment extends Fragment {

    private RecyclerView rv;
    private TableAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration reg;

    public static Fragment newInstance() { return new TablesFragment(); }

    @Nullable
    @Override public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup parent, @Nullable Bundle s) {
        View v = inf.inflate(R.layout.fragment_orders, parent, false);
        rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new GridLayoutManager(getContext(), 3)); // 3 cột như ảnh
        adapter = new TableAdapter();
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // sync với nút Refresh của AdminMenu
        getParentFragmentManager().setFragmentResultListener(
                "force_refresh", this, (k, b) -> loadOnce()
        );
        return v;
    }

    @Override public void onStart() {
        super.onStart();
        // Realtime danh sách bàn
        reg = db.collection("acc")
                .whereEqualTo("role", "user")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    buildRows(snap.getDocuments());
                });
    }

    @Override public void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    private void loadOnce() {
        db.collection("acc").whereEqualTo("role", "user")
                .get().addOnSuccessListener(snap -> buildRows(snap.getDocuments()))
                .addOnFailureListener(e -> toast("Lỗi tải bàn: " + e.getMessage()));
    }

    private void buildRows(List<DocumentSnapshot> accDocs) {
        if (accDocs == null) return;
        List<TableAdapter.TableRow> rows = new ArrayList<>();
        List<com.google.android.gms.tasks.Task<Void>> pending = new ArrayList<>();

        for (DocumentSnapshot acc : accDocs) {
            final String uid  = acc.getId();
            final String name = acc.getString("name");

            TableAdapter.TableRow row = new TableAdapter.TableRow();
            row.userId = uid;
            row.name   = (name != null ? name : "(?)");
            row.status = "Trống";
            row.sub    = "";

            // query đơn mở
            com.google.android.gms.tasks.Task<QuerySnapshot> tOrder =
                    db.collection("orders")
                            .whereEqualTo("userId", uid)
                            .whereIn("status", Arrays.asList("pending","confirmed"))
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(1).get();

            // query gọi NV đang chờ
            com.google.android.gms.tasks.Task<QuerySnapshot> tCall =
                    db.collection("staff_calls")
                            .whereEqualTo("userId", uid)
                            .whereEqualTo("status", "queued")
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(1).get();

            com.google.android.gms.tasks.Task<Void> c =
                    Tasks.whenAllSuccess(tOrder, tCall).onSuccessTask(x -> {
                        QuerySnapshot os = tOrder.getResult();
                        if (os != null && !os.isEmpty()) {
                            row.status = "Đang phục vụ";
                            Date ct = os.getDocuments().get(0).getDate("createdAt");
                            if (ct != null) {
                                String hhmm = new SimpleDateFormat("Đặt lúc HH:mm", Locale.getDefault()).format(ct);
                                row.sub = hhmm;
                            }
                        }
                        QuerySnapshot cs = tCall.getResult();
                        if (cs != null && !cs.isEmpty()) {
                            if (!"Đang phục vụ".equals(row.status)) row.status = "Đang gọi NV";
                            if (row.sub == null || row.sub.isEmpty()) {
                                Date ct = cs.getDocuments().get(0).getDate("createdAt");
                                if (ct != null) {
                                    String hhmm = new SimpleDateFormat("Gọi lúc HH:mm", Locale.getDefault()).format(ct);
                                    row.sub = hhmm;
                                } else row.sub = "Có yêu cầu hỗ trợ";
                            }
                        }
                        return Tasks.forResult(null);
                    }).continueWithTask(t -> { rows.add(row); return Tasks.forResult(null); });

            pending.add(c);
        }

        Tasks.whenAllComplete(pending).addOnSuccessListener(v -> {
            Collections.sort(rows, (a,b) -> {
                if (a.name == null) return -1;
                if (b.name == null) return 1;
                return a.name.compareToIgnoreCase(b.name);
            });
            adapter.submit(rows);
        });
    }

    private void toast(String m) { if (getContext()!=null) Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show(); }
}
