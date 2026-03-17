package com.example.appbanhang.feature.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.repository.StaffCallRepository;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

/**
 * ViewModel cho AdminMenuActivity – quản lý staff call queue.
 */
public class AdminMenuViewModel extends ViewModel {

    public static class StaffCall {
        public final String callId;
        public final String tableName;
        public StaffCall(String id, String name) { callId = id; tableName = name; }
    }

    private final StaffCallRepository repo = new StaffCallRepository();
    private final MutableLiveData<Event<StaffCall>> nextCall = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();

    private ListenerRegistration reg;

    public LiveData<Event<StaffCall>> getNextCall() { return nextCall; }
    public LiveData<Event<String>> getToast() { return toast; }

    public void startListening() {
        stopListening();
        reg = repo.listenQueuedCalls(new StaffCallRepository.ResultCallback<List<DocumentSnapshot>>() {
            @Override
            public void onSuccess(List<DocumentSnapshot> docs) {
                if (docs == null || docs.isEmpty()) return;
                DocumentSnapshot d = docs.get(0);
                String name = d.getString("name");
                nextCall.postValue(new Event<>(new StaffCall(d.getId(), name != null ? name : "Khách")));
            }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void acknowledgeCall(@NonNull String callId, @Nullable String adminUid) {
        repo.acknowledgeCall(callId, adminUid, new StaffCallRepository.SimpleCallback() {
            @Override public void onSuccess() { /* dialog dismiss */ }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void stopListening() {
        if (reg != null) { reg.remove(); reg = null; }
    }

    @Override protected void onCleared() { stopListening(); }
}
