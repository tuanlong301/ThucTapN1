package com.example.appbanhang.feature.owner;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.repository.TableRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OwnerTablesViewModel extends ViewModel {

    private final TableRepository repo = new TableRepository();

    private final MutableLiveData<List<TableRepository.AccountDoc>> accounts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();

    private ListenerRegistration reg;

    public LiveData<List<TableRepository.AccountDoc>> getAccounts() { return accounts; }
    public LiveData<Event<String>> getToast() { return toast; }

    public void startListening() {
        stopListening();
        reg = repo.listenAccounts(new TableRepository.ResultCallback<List<TableRepository.AccountDoc>>() {
            @Override public void onSuccess(List<TableRepository.AccountDoc> result) { accounts.postValue(result); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void addAccount(@NonNull Context ctx, @NonNull String name, @NonNull String email, @NonNull String password) {
        repo.createAuthAndSave(ctx, name, email, password, new TableRepository.SimpleCallback() {
            @Override public void onSuccess() { toast.postValue(new Event<>("Đã thêm")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void updateAccount(@NonNull String id, @NonNull Map<String, Object> data) {
        repo.updateAccount(id, data, new TableRepository.SimpleCallback() {
            @Override public void onSuccess() { toast.postValue(new Event<>("Đã lưu")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void updateAuthPassword(@NonNull Context ctx, @NonNull String email,
                                    @NonNull String oldPass, @NonNull String newPass) {
        repo.updateAuthPassword(ctx, email, oldPass, newPass, new TableRepository.SimpleCallback() {
            @Override public void onSuccess() { toast.postValue(new Event<>("Đã đổi mật khẩu")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void deleteAccount(@NonNull String id) {
        repo.deleteAccount(id, new TableRepository.SimpleCallback() {
            @Override public void onSuccess() { toast.postValue(new Event<>("Đã xoá")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void stopListening() { if (reg != null) { reg.remove(); reg = null; } }

    @Override protected void onCleared() { stopListening(); }
}
