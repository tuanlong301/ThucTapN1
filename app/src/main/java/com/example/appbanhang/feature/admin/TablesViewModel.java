package com.example.appbanhang.feature.admin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.repository.TableRepository;
import com.example.appbanhang.feature.admin.adapter.TableAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TablesViewModel extends ViewModel {

    private final TableRepository repo = new TableRepository();

    private final MutableLiveData<List<TableAdapter.TableRow>> tables = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();

    public LiveData<List<TableAdapter.TableRow>> getTables() { return tables; }
    public LiveData<Event<String>> getToast() { return toast; }

    public void loadTables() {
        repo.loadTablesWithState(new TableRepository.ResultCallback<List<TableRepository.TableRow>>() {
            @Override
            public void onSuccess(List<TableRepository.TableRow> result) {
                List<TableAdapter.TableRow> rows = new ArrayList<>();
                for (TableRepository.TableRow r : result) {
                    TableAdapter.TableRow row = new TableAdapter.TableRow();
                    row.userId = r.userId;
                    row.name = r.name;
                    row.status = r.status;
                    row.sub = r.sub;
                    rows.add(row);
                }
                tables.postValue(rows);
            }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void saveState(@NonNull String uid, @NonNull String status, @Nullable Date reservedAt) {
        repo.saveManualState(uid, status, reservedAt, new TableRepository.SimpleCallback() {
            @Override public void onSuccess() {
                toast.postValue(new Event<>("Đã cập nhật"));
                loadTables();
            }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }
}
