package com.example.appbanhang.owner;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.Timestamp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanhang.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * OwnerStatsFragment (v3.1)
 * - Doanh thu = SUM(field `total`) cho các đơn confirmed + paid trong khoảng thời gian.
 * - Danh sách dưới: mỗi ĐƠN một dòng
 *      Trái: Tên bàn + món
 *      Phải: Thời gian + Tổng tiền (đ)
 */
public class OwnerStatsFragment extends Fragment {

    // Bắt buộc đã thanh toán
    private static final boolean REQUIRE_PAID = true;

    private Button btnToday, btnThisWeek, btnThisMonth, btnFromDate, btnToDate, btnApply;
    private TextView tvRevenue, tvOrderCount, tvAvg;
    private RecyclerView rvTopItems; // tái dùng làm list đơn

    private final Calendar fromCal = Calendar.getInstance();
    private final Calendar toCal = Calendar.getInstance();

    private OrdersAdapter ordersAdapter;
    private FirebaseFirestore db;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        // Bind views
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

        db = FirebaseFirestore.getInstance();

        // mặc định hôm nay
        setToday();
        applyFilterAsync();

        btnToday.setOnClickListener(x -> { setToday(); applyFilterAsync(); });
        btnThisWeek.setOnClickListener(x -> { setThisWeek(); applyFilterAsync(); });
        btnThisMonth.setOnClickListener(x -> { setThisMonth(); applyFilterAsync(); });
        btnFromDate.setOnClickListener(x -> showDatePicker(true));
        btnToDate.setOnClickListener(x -> showDatePicker(false));
        btnApply.setOnClickListener(x -> applyFilterAsync());
    }

    // ===== Date helpers =====
    private void setToday() {
        Calendar now = Calendar.getInstance();
        setDate(fromCal, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0,0,0,0);
        setDate(toCal,   now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 23,59,59,999);
        updateDateButtonsText();
    }
    private void setThisWeek() {
        Calendar now = Calendar.getInstance();
        int diff = (now.get(Calendar.DAY_OF_WEEK)+5)%7;
        Calendar start = (Calendar) now.clone(); start.add(Calendar.DAY_OF_MONTH, -diff);
        setDate(fromCal, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH), 0,0,0,0);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.DAY_OF_MONTH, 6);
        setDate(toCal, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH), 23,59,59,999);
        updateDateButtonsText();
    }
    private void setThisMonth() {
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar) now.clone(); start.set(Calendar.DAY_OF_MONTH, 1);
        setDate(fromCal, start.get(Calendar.YEAR), start.get(Calendar.MONTH), 1, 0,0,0,0);
        Calendar end = (Calendar) start.clone(); end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        setDate(toCal, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH), 23,59,59,999);
        updateDateButtonsText();
    }
    private void setDate(Calendar cal, int y, int m, int d, int hh, int mm, int ss, int ms) {
        cal.set(Calendar.YEAR,y); cal.set(Calendar.MONTH,m); cal.set(Calendar.DAY_OF_MONTH,d);
        cal.set(Calendar.HOUR_OF_DAY,hh); cal.set(Calendar.MINUTE,mm); cal.set(Calendar.SECOND,ss); cal.set(Calendar.MILLISECOND,ms);
    }
    private void showDatePicker(boolean isFrom) {
        Calendar c = isFrom ? fromCal : toCal;
        new DatePickerDialog(requireContext(), (DatePicker view, int y, int m, int d) -> {
            if (isFrom) setDate(fromCal, y,m,d,0,0,0,0); else setDate(toCal, y,m,d,23,59,59,999);
            updateDateButtonsText();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void updateDateButtonsText() {
        btnFromDate.setText(android.text.format.DateFormat.format("dd/MM/yyyy", fromCal));
        btnToDate.setText(android.text.format.DateFormat.format("dd/MM/yyyy", toCal));
    }

    // ===== Load & compute from Firestore =====
    private void applyFilterAsync() {
        long from = fromCal.getTimeInMillis();
        long to   = toCal.getTimeInMillis();

        Query q = db.collection("orders")
                .whereEqualTo("status", "confirmed")
                .whereEqualTo("paymentStatus", "paid")
                .whereGreaterThanOrEqualTo("timestamp", new java.util.Date(from))
                .whereLessThanOrEqualTo("timestamp", new java.util.Date(to));

        q.get().addOnSuccessListener(snap -> {
            long revenue = 0L;
            int count = 0;
            List<OrderRow> rows = new ArrayList<>();

            for (DocumentSnapshot d : snap.getDocuments()) {
                long total = parseTotalStrict(d.get("total"));
                revenue += total;
                count++;

                // title (bên trái)
                String orderName = safeStr(d.get("name"));
                if (TextUtils.isEmpty(orderName)) orderName = "#" + d.getId();
                String itemsLine = buildItemsLine(d.get("items"));

                // right: time + total
                long ts = 0L;
                Object tsv = d.get("timestamp");
                if (tsv instanceof java.util.Date) ts = ((java.util.Date) tsv).getTime();
                else if (tsv instanceof Number) ts = ((Number) tsv).longValue();

                rows.add(new OrderRow(orderName, itemsLine, ts, total));
            }

            long avg = count == 0 ? 0 : (revenue / count);
            tvRevenue.setText(formatVND(revenue));
            tvOrderCount.setText(String.valueOf(count));
            tvAvg.setText(formatVND(avg));

            ordersAdapter.submit(rows);
        }).addOnFailureListener(err -> {
            System.out.println("[STATS] Firestore error: " + err.getMessage());
        });
    }

    // ===== Items → text cho từng đơn =====
    private String buildItemsLine(Object itemsObj) {
        // Firestore lưu mảng (List<Map>) hoặc chuỗi JSON
        if (itemsObj instanceof List) {
            List<?> list = (List<?>) itemsObj;
            List<String> parts = new ArrayList<>();
            for (Object it : list) {
                if (!(it instanceof java.util.Map)) continue;
                java.util.Map<?,?> m = (java.util.Map<?,?>) it;
                String name = safeStr(m.get("name"));
                int qty = valInt(m.get("qty"), m.get("quantity"), m.get("soLuong"), m.get("count"));
                if (!TextUtils.isEmpty(name) && qty > 0) parts.add(name + " x" + qty);
            }
            return TextUtils.join(", ", parts);
        }
        if (itemsObj instanceof String) {
            try {
                JSONArray arr = new JSONArray((String) itemsObj);
                List<String> parts = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    String name = o.optString("name", "");
                    int qty = o.has("qty") ? o.optInt("qty", 0) : o.optInt("quantity", 0);
                    if (!TextUtils.isEmpty(name) && qty > 0) parts.add(name + " x" + qty);
                }
                return TextUtils.join(", ", parts);
            } catch (Exception ignore) {}
        }
        return "";
    }

    // ===== Parsers / utils =====
    private long parseTotalStrict(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number) return Math.round(((Number) v).doubleValue());
        if (v instanceof String) {
            String s = ((String) v).trim()
                    .replace("đ","").replace("Đ","").replace("₫","")
                    .replace(" VNĐ","").replace(" VND","").replace(" ","");
            if (s.matches("^\\d+[\\.,]\\d+$")) { // "55000.0"
                s = s.replace(",", ".");
                try { return Math.round(Double.parseDouble(s)); } catch (Exception ignore) {}
            }
            if (s.matches("^\\d{1,3}(\\.\\d{3})+$")) { try { return Long.parseLong(s.replace(".", "")); } catch (Exception ignore) {} }
            if (s.matches("^\\d{1,3}(,\\d{3})+$")) { try { return Long.parseLong(s.replace(",", "")); } catch (Exception ignore) {} }
            String digits = s.replaceAll("[^0-9]","");
            if (!digits.isEmpty()) { try { return Long.parseLong(digits); } catch (Exception ignore) {} }
        }
        return 0L;
    }

    private String formatVND(long amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi","VN"));
        return nf.format(amount) + " đ";
    }
    private static String safeStr(Object v) { return v == null ? "" : String.valueOf(v); }
    private static int valInt(Object... vs) {
        for (Object v : vs) {
            if (v instanceof Number) return ((Number)v).intValue();
            if (v instanceof String) {
                try { return Integer.parseInt(((String)v).replaceAll("[^0-9]","")); }
                catch (Exception ignore) {}
            }
        }
        return 0;
    }
    private String fmtTime(long ts) {
        if (ts == 0) return "";
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new java.util.Date(ts));
    }

    // ===== RecyclerView: mỗi đơn một dòng (trái: tên+items, phải: giờ+tiền) =====
    private static class OrderRow {
        final String titleLeft;   // tên bàn/đơn
        final String subLeft;     // danh sách món
        final long timeMillis;    // giờ tạo/confirm
        final long total;         // tổng tiền
        OrderRow(String tl, String sl, long t, long tot) { titleLeft = tl; subLeft = sl; timeMillis = t; total = tot; }
    }

    private class OrderVH extends RecyclerView.ViewHolder {
        final TextView tvTitle, tvSub, tvTime, tvMoney;
        OrderVH(@NonNull View root, TextView t1, TextView s1, TextView t2, TextView m2) {
            super(root); tvTitle = t1; tvSub = s1; tvTime = t2; tvMoney = m2;
        }
    }

    private class OrdersAdapter extends RecyclerView.Adapter<OrderVH> {
        private final List<OrderRow> data = new ArrayList<>();
        void submit(List<OrderRow> rows) { data.clear(); if (rows != null) data.addAll(rows); notifyDataSetChanged(); }

        @NonNull @Override public OrderVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // root: horizontal
            LinearLayout root = new LinearLayout(parent.getContext());
            root.setOrientation(LinearLayout.HORIZONTAL);
            root.setPadding(24, 18, 24, 18);
            root.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            // left column (title + items)
            LinearLayout left = new LinearLayout(parent.getContext());
            left.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams lpLeft = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            left.setLayoutParams(lpLeft);

            TextView title = new TextView(parent.getContext());
            title.setTextSize(16);
            title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);

            TextView sub = new TextView(parent.getContext());
            sub.setTextSize(14);

            left.addView(title);
            left.addView(sub);

            // right column (time + money)
            LinearLayout right = new LinearLayout(parent.getContext());
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);
            LinearLayout.LayoutParams lpRight = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            right.setLayoutParams(lpRight);

            TextView time = new TextView(parent.getContext());
            time.setTextSize(13);

            TextView money = new TextView(parent.getContext());
            money.setTextSize(15);
            money.setTypeface(money.getTypeface(), android.graphics.Typeface.BOLD);

            right.addView(time);
            right.addView(money);

            root.addView(left);
            root.addView(right);

            return new OrderVH(root, title, sub, time, money);
        }

        @Override public void onBindViewHolder(@NonNull OrderVH h, int position) {
            OrderRow r = data.get(position);
            h.tvTitle.setText(r.titleLeft);
            h.tvSub.setText(TextUtils.isEmpty(r.subLeft) ? "(Không có món)" : r.subLeft);
            h.tvTime.setText(fmtTime(r.timeMillis));
            h.tvMoney.setText(formatVND(r.total));
        }

        @Override public int getItemCount() { return data.size(); }
    }
}
