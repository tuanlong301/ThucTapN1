package com.example.appbanhang.feature.owner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.model.Product;
import com.example.appbanhang.data.repository.ProductRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OwnerProductsViewModel extends ViewModel {

    private final ProductRepository repo = new ProductRepository();

    private final MutableLiveData<List<Product>> filteredProducts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Event<String>> toast = new MutableLiveData<>();

    private final List<Product> all = new ArrayList<>();
    private String currentQuery = "";
    private ListenerRegistration reg;

    public LiveData<List<Product>> getFilteredProducts() { return filteredProducts; }
    public LiveData<Event<String>> getToast() { return toast; }

    public void startListening() {
        stopListening();
        reg = repo.listenAll(new ProductRepository.Callback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> result) {
                all.clear();
                all.addAll(result);
                applyFilter();
            }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void setSearchQuery(@Nullable String query) {
        currentQuery = (query == null) ? "" : query.toLowerCase(Locale.ROOT).trim();
        applyFilter();
    }

    private void applyFilter() {
        if (currentQuery.isEmpty()) {
            filteredProducts.postValue(new ArrayList<>(all));
            return;
        }
        List<Product> filtered = new ArrayList<>();
        for (Product p : all) {
            String name = p.getName() != null ? p.getName().toLowerCase(Locale.ROOT) : "";
            String cat = p.getCategory() != null ? p.getCategory().toLowerCase(Locale.ROOT) : "";
            if (name.contains(currentQuery) || cat.contains(currentQuery)) {
                filtered.add(p);
            }
        }
        filteredProducts.postValue(filtered);
    }

    public void addProduct(@NonNull Map<String, Object> data) {
        repo.addProduct(data, new ProductRepository.Callback<Void>() {
            @Override public void onSuccess(Void result) { /* realtime will update */ }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void updateProduct(@NonNull String id, @NonNull Map<String, Object> data) {
        repo.updateProduct(id, data, new ProductRepository.Callback<Void>() {
            @Override public void onSuccess(Void result) { /* realtime will update */ }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void deleteProduct(@NonNull String id) {
        repo.deleteProduct(id, new ProductRepository.Callback<Void>() {
            @Override public void onSuccess(Void result) { toast.postValue(new Event<>("Đã xóa")); }
            @Override public void onError(String msg) { toast.postValue(new Event<>(msg)); }
        });
    }

    public void stopListening() { if (reg != null) { reg.remove(); reg = null; } }

    @Override protected void onCleared() { stopListening(); }
}
