package com.example.appbanhang.feature.admin;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.model.Order;
import com.example.appbanhang.data.repository.OrderRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class PendingOrdersViewModel extends ViewModel {

    private final OrderRepository repo = new OrderRepository();

    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> hasNewOrder = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();

    private ListenerRegistration reg;
    private int prevCount = -1;

    public LiveData<List<Order>> getOrders() { return orders; }
    public LiveData<Boolean> getHasNewOrder() { return hasNewOrder; }
    public LiveData<Event<String>> getToast() { return toast; }

    public void startListening() {
        stopListening();
        reg = repo.listenPending(new OrderRepository.ResultCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> result) {
                int newCount = result.size();
                if (prevCount >= 0 && newCount > prevCount) {
                    hasNewOrder.postValue(true);
                }
                prevCount = newCount;
                orders.postValue(result);
            }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void resetNewOrderFlag() { hasNewOrder.setValue(false); }

    public void confirmOrder(@NonNull String orderId) {
        repo.updateStatus(orderId, "confirmed", new OrderRepository.SimpleCallback() {
            @Override public void onSuccess() { toast.postValue(new Event<>("Đã xác nhận")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void cancelOrder(@NonNull String orderId, @NonNull String reason) {
        repo.cancelOrder(orderId, reason, new OrderRepository.SimpleCallback() {
            @Override public void onSuccess() { toast.postValue(new Event<>("Đã hủy đơn")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void stopListening() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    @Override protected void onCleared() { stopListening(); }
}
