package com.example.appbanhang.feature.user;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.model.CartItem;
import com.example.appbanhang.data.model.NoteItem;
import com.example.appbanhang.data.repository.AuthRepository;
import com.example.appbanhang.data.repository.CartRepository;
import com.example.appbanhang.data.repository.OrderRepository;
import com.example.appbanhang.common.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CartViewModel extends ViewModel {

    private final CartRepository cartRepo = new CartRepository();
    private final OrderRepository orderRepo = new OrderRepository();
    private final AuthRepository authRepo = new AuthRepository();

    private final MutableLiveData<List<Object>> cartItems = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> totalText = new MutableLiveData<>("0 đ");
    private final MutableLiveData<Boolean> cartEmpty = new MutableLiveData<>(true);
    private final MutableLiveData<Event<String>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> orderSuccess = new MutableLiveData<>();

    public LiveData<List<Object>> getCartItems() { return cartItems; }
    public LiveData<String> getTotalText() { return totalText; }
    public LiveData<Boolean> getCartEmpty() { return cartEmpty; }
    public LiveData<Event<String>> getMessage() { return message; }
    public LiveData<Event<Boolean>> getOrderSuccess() { return orderSuccess; }

    @Nullable
    private String getUid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    public void loadCart() {
        String uid = getUid();
        if (uid == null) { message.setValue(new Event<>("Phiên đăng nhập hết hạn.\nVui lòng đăng nhập lại.")); return; }

        cartRepo.loadCart(uid, new CartRepository.ResultCallback<List<CartItem>>() {
            @Override
            public void onSuccess(List<CartItem> result) {
                List<Object> items = new ArrayList<>(result);
                items.add(new NoteItem(""));
                cartItems.postValue(items);
                updateTotal(items);
            }
            @Override
            public void onError(String msg) {
                message.postValue(new Event<>("Không thể tải giỏ hàng.\nVui lòng kiểm tra kết nối."));
            }
        });
    }

    public void changeQty(int pos, int delta) {
        List<Object> items = cartItems.getValue();
        if (items == null || pos < 0 || pos >= items.size()) return;
        Object obj = items.get(pos);
        if (!(obj instanceof CartItem)) return;
        CartItem it = (CartItem) obj;
        String uid = getUid();
        if (uid == null || it.id == null) return;

        long q = it.qty != null ? it.qty : 0;
        if (delta > 0 && q >= 9) return;

        if (delta < 0 && q <= 1) {
            deleteItem(it);
            return;
        }

        cartRepo.changeQty(uid, it.id, delta, new CartRepository.SimpleCallback() {
            @Override public void onSuccess() { loadCart(); }
            @Override public void onError(String msg) {
                message.postValue(new Event<>("Không thể thay đổi số lượng.\nVui lòng thử lại."));
            }
        });
    }

    private void deleteItem(@NonNull CartItem item) {
        String uid = getUid();
        if (uid == null || item.id == null) return;
        cartRepo.deleteItem(uid, item.id, new CartRepository.SimpleCallback() {
            @Override public void onSuccess() { loadCart(); }
            @Override public void onError(String msg) {
                message.postValue(new Event<>("Không thể xóa món.\nVui lòng thử lại."));
            }
        });
    }

    public void checkout(@NonNull String note, @Nullable String paymentMethod, @Nullable String cachedName) {
        String uid = getUid();
        if (uid == null) { message.setValue(new Event<>("Phiên đăng nhập hết hạn.\nVui lòng đăng nhập lại.")); return; }
        List<Object> items = cartItems.getValue();
        if (items == null || items.size() <= 1) { message.setValue(new Event<>("Giỏ hàng trống!\nVui lòng thêm món trước khi đặt hàng.")); return; }
        if (paymentMethod == null) { message.setValue(new Event<>("Vui lòng chọn phương thức thanh toán!")); return; }

        if (cachedName != null && !cachedName.trim().isEmpty()) {
            doSaveOrder(uid, note, paymentMethod, cachedName.trim());
        } else {
            String email = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : null;
            authRepo.resolveOrdererName(uid, email, name ->
                    doSaveOrder(uid, note, paymentMethod, name != null ? name : "Khách"));
        }
    }

    private void doSaveOrder(String uid, String note, String paymentMethod, String name) {
        Map<String, Object> order = new HashMap<>();
        order.put("userId", uid);
        order.put("name", name);
        order.put("items", buildItemsString());
        order.put("total", String.valueOf(calculateTotal()));
        order.put("notes", note);
        order.put("paymentMethod", paymentMethod);
        order.put("status", Constants.STATUS_PENDING);
        order.put("paymentStatus",
                Constants.PM_TRANSFER.equalsIgnoreCase(paymentMethod)
                        ? Constants.PAY_AWAITING_TRANSFER : Constants.PAY_UNPAID);
        order.put("timestamp", FieldValue.serverTimestamp());
        order.put("timestampStr",
                new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));

        orderRepo.createOrder(order, new OrderRepository.SimpleCallback() {
            @Override public void onSuccess() { orderSuccess.postValue(new Event<>(true)); }
            @Override public void onError(String msg) {
                message.postValue(new Event<>("Đặt hàng thất bại.\nVui lòng kiểm tra kết nối và thử lại."));
            }
        });
    }

    public void clearCart() {
        String uid = getUid();
        if (uid == null) return;
        cartRepo.clearCart(uid, new CartRepository.SimpleCallback() {
            @Override public void onSuccess() { loadCart(); }
            @Override public void onError(String msg) {
                message.postValue(new Event<>("Không thể xóa giỏ hàng.\nVui lòng thử lại."));
            }
        });
    }

    public void refreshCachedName(@NonNull CacheNameListener listener) {
        String uid = getUid();
        if (uid == null) return;
        authRepo.fetchName(uid, name -> { if (name != null) listener.onName(name); });
    }

    // ========== Helpers ==========

    private void updateTotal(List<Object> items) {
        double sum = 0;
        boolean hasItems = false;
        for (Object obj : items) {
            if (obj instanceof CartItem) {
                CartItem c = (CartItem) obj;
                sum += (c.price != null ? c.price : 0) * (c.qty != null ? c.qty : 0);
                hasItems = true;
            }
        }
        NumberFormat f = NumberFormat.getInstance(new Locale("vi", "VN"));
        totalText.postValue(f.format(sum) + " đ");
        cartEmpty.postValue(!hasItems);
    }

    private double calculateTotal() {
        List<Object> items = cartItems.getValue();
        if (items == null) return 0;
        double sum = 0;
        for (Object obj : items) {
            if (obj instanceof CartItem) {
                CartItem c = (CartItem) obj;
                sum += (c.price != null ? c.price : 0) * (c.qty != null ? c.qty : 0);
            }
        }
        return sum;
    }

    private String buildItemsString() {
        List<Object> items = cartItems.getValue();
        if (items == null) return "[]";
        StringBuilder sb = new StringBuilder("[ ");
        for (Object obj : items) {
            if (obj instanceof CartItem) {
                CartItem c = (CartItem) obj;
                sb.append("{ \"name\": \"").append(c.name)
                        .append("\", \"qty\": ").append(c.qty)
                        .append(", \"price\": ").append(c.price)
                        .append(", \"notes\": \"\" }, ");
            }
        }
        if (sb.length() > 2) sb.setLength(sb.length() - 2);
        sb.append(" ]");
        return sb.toString();
    }

    public interface CacheNameListener { void onName(String name); }
}
