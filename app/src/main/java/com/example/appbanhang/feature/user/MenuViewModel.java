package com.example.appbanhang.feature.user;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.model.Product;
import com.example.appbanhang.data.repository.AuthRepository;
import com.example.appbanhang.data.repository.CartRepository;
import com.example.appbanhang.data.repository.ProductRepository;
import com.example.appbanhang.data.repository.StaffCallRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MenuViewModel extends ViewModel {

    public enum CallStaffResult { SUCCESS, SPAM, OFFLINE, ERROR }

    private final ProductRepository productRepo = new ProductRepository();
    private final CartRepository cartRepo = new CartRepository();
    private final StaffCallRepository staffRepo = new StaffCallRepository();
    private final AuthRepository authRepo = new AuthRepository();

    private final MutableLiveData<List<Product>> products = new MutableLiveData<>();
    private final MutableLiveData<Integer> cartCount = new MutableLiveData<>(0);
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();
    private final MutableLiveData<Event<CallStaffResult>> callResult = new MutableLiveData<>();

    /** Danh sách gốc (chưa filter) — dùng để search local */
    private List<Product> allProducts = new ArrayList<>();
    private String currentQuery = "";

    private ListenerRegistration cartReg;

    public LiveData<List<Product>> getProducts() { return products; }
    public LiveData<Integer> getCartCount() { return cartCount; }
    public LiveData<Event<String>> getToast() { return toast; }
    public LiveData<Event<CallStaffResult>> getCallStaffResult() { return callResult; }

    public void loadAll() {
        productRepo.loadAll(new ProductRepository.Callback<List<Product>>() {
            @Override public void onSuccess(List<Product> result) {
                allProducts = result != null ? result : new ArrayList<>();
                applySearch();
            }
            @Override public void onError(String msg) {
                toast.postValue(new Event<>("Không thể tải menu. Vui lòng thử lại."));
            }
        });
    }

    public void loadByCategory(@NonNull String cat) {
        productRepo.loadByCategory(cat, new ProductRepository.Callback<List<Product>>() {
            @Override public void onSuccess(List<Product> result) {
                allProducts = result != null ? result : new ArrayList<>();
                applySearch();
            }
            @Override public void onError(String msg) {
                toast.postValue(new Event<>("Không thể tải danh mục. Vui lòng thử lại."));
            }
        });
    }

    /** Tìm kiếm local theo tên món */
    public void search(@NonNull String query) {
        currentQuery = query.trim().toLowerCase();
        applySearch();
    }

    private void applySearch() {
        if (currentQuery.isEmpty()) {
            products.postValue(allProducts);
        } else {
            List<Product> filtered = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.getName() != null && p.getName().toLowerCase().contains(currentQuery)) {
                    filtered.add(p);
                }
            }
            products.postValue(filtered);
        }
    }

    public void addToCart(@NonNull String uid, @NonNull Product p) {
        if (p.getId() == null) return;
        cartRepo.addToCart(uid, p.getId(), p.getName(), p.getImageUrl(), p.getPrice(),
                new CartRepository.ResultCallback<Boolean>() {
                    @Override public void onSuccess(Boolean added) {
                        if (Boolean.TRUE.equals(added)) {
                            toast.postValue(new Event<>("Đã thêm món: " + p.getName()));
                        } else {
                            toast.postValue(new Event<>("__MAX_QTY__")); // special marker cho dialog
                        }
                    }
                    @Override public void onError(String msg) {
                        toast.postValue(new Event<>("Không thể thêm vào giỏ. Vui lòng thử lại."));
                    }
                });
    }

    public void startListenCartCount(@NonNull String uid) {
        stopListenCartCount();
        cartReg = cartRepo.listenCartCount(uid, new CartRepository.ResultCallback<Integer>() {
            @Override public void onSuccess(Integer result) { cartCount.postValue(result); }
            @Override public void onError(String msg) { /* silent */ }
        });
    }

    public void stopListenCartCount() {
        if (cartReg != null) { cartReg.remove(); cartReg = null; }
    }

    public void callStaff(@NonNull String uid, boolean isOnline) {
        if (!isOnline) { callResult.setValue(new Event<>(CallStaffResult.OFFLINE)); return; }

        staffRepo.checkSpam(uid, new StaffCallRepository.ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean canCall) {
                if (!canCall) { callResult.postValue(new Event<>(CallStaffResult.SPAM)); return; }
                staffRepo.createCall(uid, new StaffCallRepository.SimpleCallback() {
                    @Override public void onSuccess() { callResult.postValue(new Event<>(CallStaffResult.SUCCESS)); }
                    @Override public void onError(String msg) {
                        toast.postValue(new Event<>("Gọi nhân viên thất bại. Vui lòng thử lại."));
                        callResult.postValue(new Event<>(CallStaffResult.ERROR));
                    }
                });
            }
            @Override public void onError(String msg) {
                toast.postValue(new Event<>("Có lỗi xảy ra. Vui lòng thử lại."));
            }
        });
    }

    public void signInAnonymously(@NonNull Runnable onSuccess) {
        authRepo.signInAnonymously(onSuccess, msg ->
                toast.postValue(new Event<>("Không thể kết nối. Vui lòng kiểm tra mạng.")));
    }

    @Override
    protected void onCleared() {
        stopListenCartCount();
    }
}
