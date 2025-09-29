package com.example.appbanhang.admin;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.example.appbanhang.admin.adapter.OrderAdapter;
import com.example.appbanhang.model.Order;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class PendingOrdersFragment extends Fragment {

    public static PendingOrdersFragment newInstance() {
        PendingOrdersFragment f = new PendingOrdersFragment();
        f.setArguments(new Bundle());
        return f;
    }

    private TextView tvEmpty;
    private RecyclerView rv;
    private OrderAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration reg;
    @Nullable private String highlightId;

    // ====== thêm: âm báo ting ======
    private SoundPool soundPool;
    private int tingId = 0;
    private boolean tingReady = false;
    private boolean initialLoaded = false; // bỏ qua lần tải đầu

    @SuppressLint("MissingInflatedId")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_orders, container, false);

        tvEmpty = v.findViewById(R.id.tvEmpty);
        rv = v.findViewById(R.id.rvGeneric);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new OrderAdapter(OrderAdapter.Mode.PENDING, new OrderAdapter.OnAction() {
            @Override public void onConfirm(String orderId) { updateStatus(orderId, "confirmed"); }
            @Override public void onCancel(String orderId)  { updateStatus(orderId, "canceled"); }
            @Override public void onPay(String orderId)     { /* not dùng ở pending */ }
            @Override public void onPrint(String orderId)   { /* not dùng ở pending */ }
        });
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        // nhận highlight_id nếu có
        Bundle args = getArguments();
        if (args != null) highlightId = args.getString("highlight_id");

        // nhận sự kiện refresh từ AdminMenu
        getParentFragmentManager().setFragmentResultListener(
                "force_refresh", this, (key, b) -> {
                    rv.scrollToPosition(0);
                    adapter.highlight(null);
                }
        );

        // ====== init âm báo ======
        initSound();

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        // Reset cờ lần tải đầu mỗi khi attach lại
        initialLoaded = false;

        // Query tất cả orders có status = pending, sắp xếp theo timestamp
        reg = db.collection("orders")
                .whereEqualTo("status", "pending")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        if (getContext()!=null) {
                            Toast.makeText(getContext(), "Lỗi tải đơn: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                        e.printStackTrace();
                        return;
                    }
                    if (snap == null) return;

                    // ====== phát ting cho document mới sau lần đầu ======
                    handleTingForNewOrders(snap);

                    // ====== render list như cũ ======
                    List<Order> list = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Order o = d.toObject(Order.class);
                        if (o == null) o = new Order();
                        o.id = d.getId();
                        o.createdAt = d.getDate("timestamp"); // map timestamp Firestore -> createdAt nếu cần
                        list.add(o);
                    }

                    adapter.submit(list);
                    boolean empty = list == null || list.isEmpty();
                    tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rv.setVisibility(empty ? View.GONE : View.VISIBLE);

                    if (!TextUtils.isEmpty(highlightId)) {
                        int idx = adapter.indexOf(highlightId);
                        if (idx >= 0) {
                            rv.scrollToPosition(idx);
                            adapter.highlight(highlightId);
                            highlightId = null;
                        }
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (reg != null) { reg.remove(); reg = null; }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // giải phóng SoundPool
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            tingReady = false;
            tingId = 0;
        }
    }

    private void updateStatus(String orderId, String newStatus) {
        db.collection("orders").document(orderId)
                .update(
                        "status", newStatus,
                        "confirmedAt", FieldValue.serverTimestamp(),
                        "updatedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> toast("Cập nhật: " + newStatus))
                .addOnFailureListener(e -> toast("Lỗi: " + e.getMessage()));
    }

    private void toast(String m) {
        if (getContext() != null) {
            Toast.makeText(getContext(), m, Toast.LENGTH_SHORT).show();
        }
    }

    // ================== Âm thanh ting ==================
    private void initSound() {
        if (getContext() == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setAudioAttributes(attrs)
                    .setMaxStreams(1)
                    .build();
        } else {
            soundPool = new SoundPool(1, AudioManager.STREAM_NOTIFICATION, 0);
        }

        tingId = soundPool.load(getContext(), R.raw.ting, 1);
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            tingReady = (status == 0);
        });
    }

    private void playTing() {
        if (soundPool != null && tingReady && tingId != 0) {
            soundPool.play(tingId, 1f, 1f, 1, 0, 1f);
        }
    }

    private void handleTingForNewOrders(@NonNull QuerySnapshot snap) {
        // lần đầu: chỉ set cờ, không phát ting cho dữ liệu cũ
        if (!initialLoaded) {
            initialLoaded = true;
            return;
        }
        // các lần sau: phát khi có tài liệu ADDED (và không phải pending write)
        for (DocumentChange dc : snap.getDocumentChanges()) {
            if (dc.getType() == DocumentChange.Type.ADDED &&
                    !dc.getDocument().getMetadata().hasPendingWrites()) {
                playTing();
                break; // chỉ cần ting 1 lần cho batch này
            }
        }
    }
}
