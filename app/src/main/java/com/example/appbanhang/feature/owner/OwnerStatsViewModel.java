package com.example.appbanhang.feature.owner;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.repository.OrderRepository;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwnerStatsViewModel extends ViewModel {

    // ========== Data classes ==========

    public static class StatsData {
        public final long revenue;
        public final int orderCount;
        public final long avg;
        public final List<OrderRow> rows;
        public StatsData(long r, int c, long a, List<OrderRow> rows) {
            revenue = r; orderCount = c; avg = a; this.rows = rows;
        }
    }

    public static class OrderRow {
        public final String titleLeft, subLeft;
        public final long timeMillis, total;
        public OrderRow(String tl, String sl, long t, long tot) {
            titleLeft = tl; subLeft = sl; timeMillis = t; total = tot;
        }
    }

    // ========== State ==========

    private final OrderRepository repo = new OrderRepository();
    private final MutableLiveData<StatsData> stats = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();

    private final Calendar fromCal = Calendar.getInstance();
    private final Calendar toCal = Calendar.getInstance();

    public LiveData<StatsData> getStats() { return stats; }
    public LiveData<Event<String>> getToast() { return toast; }
    public Calendar getFromCal() { return fromCal; }
    public Calendar getToCal() { return toCal; }

    public OwnerStatsViewModel() {
        setToday();
    }

    // ========== Date helpers ==========

    public void setToday() {
        Calendar now = Calendar.getInstance();
        setDate(fromCal, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0,0,0,0);
        setDate(toCal,   now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 23,59,59,999);
    }

    public void setThisWeek() {
        Calendar now = Calendar.getInstance();
        int diff = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        Calendar start = (Calendar) now.clone(); start.add(Calendar.DAY_OF_MONTH, -diff);
        setDate(fromCal, start.get(Calendar.YEAR), start.get(Calendar.MONTH), start.get(Calendar.DAY_OF_MONTH), 0,0,0,0);
        Calendar end = (Calendar) start.clone(); end.add(Calendar.DAY_OF_MONTH, 6);
        setDate(toCal, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH), 23,59,59,999);
    }

    public void setThisMonth() {
        Calendar now = Calendar.getInstance();
        setDate(fromCal, now.get(Calendar.YEAR), now.get(Calendar.MONTH), 1, 0,0,0,0);
        Calendar end = (Calendar) now.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        setDate(toCal, end.get(Calendar.YEAR), end.get(Calendar.MONTH), end.get(Calendar.DAY_OF_MONTH), 23,59,59,999);
    }

    public void setFromDate(int y, int m, int d) { setDate(fromCal, y, m, d, 0,0,0,0); }
    public void setToDate(int y, int m, int d) { setDate(toCal, y, m, d, 23,59,59,999); }

    private void setDate(Calendar cal, int y, int m, int d, int hh, int mm, int ss, int ms) {
        cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d);
        cal.set(Calendar.HOUR_OF_DAY, hh); cal.set(Calendar.MINUTE, mm);
        cal.set(Calendar.SECOND, ss); cal.set(Calendar.MILLISECOND, ms);
    }

    // ========== Load ==========

    public void applyFilter() {
        Date from = new Date(fromCal.getTimeInMillis());
        Date to = new Date(toCal.getTimeInMillis());

        repo.queryPaidOrders(from, to, new OrderRepository.ResultCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> docs) {
                long revenue = 0;
                int count = 0;
                List<OrderRow> rows = new ArrayList<>();

                for (DocumentSnapshot d : docs) {
                    long total = parseTotalStrict(d.get("total"));
                    revenue += total;
                    count++;

                    String orderName = safeStr(d.get("name"));
                    if (TextUtils.isEmpty(orderName)) orderName = "#" + d.getId();

                    long ts = 0;
                    Date dt = d.getDate("timestamp");
                    if (dt != null) { ts = dt.getTime(); }
                    else {
                        Object tsv = d.get("timestamp");
                        if (tsv instanceof Timestamp) ts = ((Timestamp) tsv).toDate().getTime();
                        else if (tsv instanceof Number) ts = ((Number) tsv).longValue();
                    }

                    rows.add(new OrderRow(orderName, buildItemsLine(d.get("items")), ts, total));
                }

                long avg = count == 0 ? 0 : (revenue / count);
                stats.postValue(new StatsData(revenue, count, avg, rows));
            }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    // ========== Parsers ==========

    private String buildItemsLine(Object itemsObj) {
        if (itemsObj instanceof List) {
            List<?> list = (List<?>) itemsObj;
            List<String> parts = new ArrayList<>();
            for (Object it : list) {
                if (!(it instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) it;
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

    private long parseTotalStrict(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return Math.round(((Number) v).doubleValue());
        if (v instanceof String) {
            String s = ((String) v).trim().replace("đ","").replace("Đ","").replace("₫","")
                    .replace(" VNĐ","").replace(" VND","").replace(" ","");
            if (s.matches("^\\d+[\\.,]\\d+$")) {
                s = s.replace(",", ".");
                try { return Math.round(Double.parseDouble(s)); } catch (Exception ignore) {}
            }
            if (s.matches("^\\d{1,3}(\\.\\d{3})+$")) try { return Long.parseLong(s.replace(".","")); } catch (Exception ignore) {}
            if (s.matches("^\\d{1,3}(,\\d{3})+$")) try { return Long.parseLong(s.replace(",","")); } catch (Exception ignore) {}
            String digits = s.replaceAll("[^0-9]","");
            if (!digits.isEmpty()) try { return Long.parseLong(digits); } catch (Exception ignore) {}
        }
        return 0;
    }

    public static String formatVND(long amount) {
        return NumberFormat.getInstance(new Locale("vi","VN")).format(amount) + " đ";
    }

    public static String fmtTime(long ts) {
        if (ts == 0) return "";
        return new SimpleDateFormat("dd/MM - HH:mm", Locale.getDefault()).format(new Date(ts));
    }

    private static String safeStr(Object v) { return v == null ? "" : String.valueOf(v); }

    private static int valInt(Object... vs) {
        for (Object v : vs) {
            if (v instanceof Number) return ((Number) v).intValue();
            if (v instanceof String) {
                try { return Integer.parseInt(((String) v).replaceAll("[^0-9]","")); }
                catch (Exception ignore) {}
            }
        }
        return 0;
    }
}
