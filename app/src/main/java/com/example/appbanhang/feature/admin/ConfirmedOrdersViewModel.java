package com.example.appbanhang.feature.admin;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.model.Order;
import com.example.appbanhang.data.repository.OrderRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class ConfirmedOrdersViewModel extends ViewModel {

    private final OrderRepository repo = new OrderRepository();

    private final MutableLiveData<List<Order>> orders = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();

    private ListenerRegistration reg;

    public LiveData<List<Order>> getOrders() { return orders; }
    public LiveData<Event<String>> getToast() { return toast; }

    public void startListening() {
        stopListening();
        reg = repo.listenConfirmed(new OrderRepository.ResultCallback<List<Order>>() {
            @Override public void onSuccess(List<Order> result) { orders.postValue(result); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void markPaid(@NonNull String orderId) {
        repo.markPaid(orderId, new OrderRepository.SimpleCallback() {
            @Override public void onSuccess() { toast.postValue(new Event<>("Đã thu tiền")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void markPrinted(@NonNull String orderId) {
        repo.markPrinted(orderId, new OrderRepository.SimpleCallback() {
            @Override public void onSuccess() { toast.postValue(new Event<>("Đã in hoá đơn")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void getOrder(@NonNull String orderId, @NonNull OrderRepository.ResultCallback<DocumentSnapshot> cb) {
        repo.getOrder(orderId, cb);
    }

    public void stopListening() { if (reg != null) { reg.remove(); reg = null; } }

    @Override protected void onCleared() { stopListening(); }
}
