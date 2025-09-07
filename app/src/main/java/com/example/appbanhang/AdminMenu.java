package com.example.appbanhang;

import android.os.Bundle;
import android.widget.Toast;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Locale;

public class AdminMenu extends BaseActivity {

    // ==== Hàng đợi gọi nhân viên ====
    private ListenerRegistration callReg;
    private final LinkedList<DocumentSnapshot> callQueue = new LinkedList<>();
    private boolean isShowingCallDialog = false;
    private boolean isRefreshAllowed = true;
    private final long REFRESH_DELAY = 1000;

    // ==== UI ====
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Tabs + Pager
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Lấy orderId vừa tạo (nếu đi từ CartActivity)
        String highlightId = getIntent().getStringExtra("justCreatedOrderId");

        // Adapter có hỗ trợ truyền highlightId vào tab "Đơn hàng"
        AdminAdapter adapter = new AdminAdapter(this, highlightId);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2); // giữ 3 tab

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Đơn hàng"); break;
                case 1: tab.setText("Đơn hàng đã xác nhận"); break;
                case 2: tab.setText("Quản lý bàn"); break;
            }
        }).attach();

        // Nút Refresh: gửi "force_refresh" cho các fragment đang lắng nghe
        View btnRefresh = findViewById(R.id.btnRefresh);
        btnRefresh.setOnClickListener(v -> {
            // disable để tránh spam
            v.setEnabled(false);

            // thực hiện refresh
            getSupportFragmentManager().setFragmentResult("force_refresh", new Bundle());
            Toast.makeText(this, "Đã làm mới dữ liệu", Toast.LENGTH_SHORT).show();

            // bật lại sau 1 giây
            v.postDelayed(() -> v.setEnabled(true), 1000);
        });

        // Nút In tất cả hóa đơn (placeholder)
        findViewById(R.id.btnPrintAll).setOnClickListener(v ->
                Toast.makeText(this, "Chức năng in đang được phát triển!", Toast.LENGTH_SHORT).show()
        );
    }

    // ==== Lắng nghe staff_calls theo hàng đợi tuần tự ====
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Nếu phía user lưu status = "queued" thì đổi "pending" -> "queued" bên dưới
        callReg = FirebaseFirestore.getInstance()
                .collection("staff_calls")
                .whereEqualTo("status", "queued")                  // ← khớp với MainMenu
                .orderBy("createdAt", Query.Direction.DESCENDING)  // ← khớp field thời gian
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    callQueue.clear();
                    callQueue.addAll(snap.getDocuments());
                    maybeShowNextCall();
                });

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (callReg != null) { callReg.remove(); callReg = null; }
        isShowingCallDialog = false;
        callQueue.clear();
    }

    private void maybeShowNextCall() {
        if (isShowingCallDialog) return;
        if (callQueue.isEmpty()) return;

        isShowingCallDialog = true;

        DocumentSnapshot d = callQueue.peekFirst();
        String name = d.getString("name");
        java.util.Date t = d.getDate("createdAt");
        String when = t != null ? new SimpleDateFormat("HH:mm", Locale.getDefault()).format(t) : null;

        StringBuilder msg = new StringBuilder();
        msg.append("Bàn: ").append(name != null ? name : "?");
        if (when != null) msg.append(" • gọi lúc ").append(when);


        new AlertDialog.Builder(this)
                .setTitle("Gọi nhân viên")
                .setMessage(msg.toString())
                .setCancelable(false) // xử xong mới tới yêu cầu tiếp theo
                .setPositiveButton("Đã nhận", (dlg, w) -> acknowledgeCall(d))
                .setNegativeButton("Bỏ qua", (dlg, w) -> {
                    // Bỏ qua yêu cầu này (không cập nhật DB), chuyển sang cái tiếp theo
                    callQueue.pollFirst();
                    isShowingCallDialog = false;
                    maybeShowNextCall();
                })
                .show();
    }

    private void acknowledgeCall(DocumentSnapshot d) {
        String callId = d.getId();
        String adminUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        FirebaseFirestore.getInstance().collection("staff_calls").document(callId)
                .update(
                        "status", "handled",
                        "ackBy", adminUid,
                        "acknowledgedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener(v -> {
                    callQueue.pollFirst();
                    isShowingCallDialog = false;
                    maybeShowNextCall();
                })
                .addOnFailureListener(e -> {
                    isShowingCallDialog = false;
                    new AlertDialog.Builder(this)
                            .setTitle("Lỗi")
                            .setMessage("Không thể cập nhật yêu cầu: " + e.getMessage())
                            .setPositiveButton("OK", (d1, w1) -> maybeShowNextCall())
                            .show();
                });
    }
}
