package com.example.appbanhang.feature.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.appbanhang.common.utils.Event;
import com.example.appbanhang.data.repository.AuthRepository;

public class LoginViewModel extends ViewModel {

    public static class LoginResult {
        public final boolean success;
        public final String role;   // "admin" | "king" | "user"
        public final String name;
        public final String error;

        private LoginResult(boolean s, String r, String n, String e) {
            success = s; role = r; name = n; error = e;
        }
        static LoginResult ok(String role, String name) { return new LoginResult(true, role, name, null); }
        static LoginResult fail(String err) { return new LoginResult(false, null, null, err); }
    }

    private final AuthRepository repo = new AuthRepository();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Event<LoginResult>> result = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Event<LoginResult>> getResult() { return result; }

    public void login(@NonNull String email, @NonNull String pass) {
        if (email.isEmpty()) {
            result.setValue(new Event<>(LoginResult.fail("Vui lòng nhập email.")));
            return;
        }
        if (pass.isEmpty()) {
            result.setValue(new Event<>(LoginResult.fail("Vui lòng nhập mật khẩu.")));
            return;
        }
        loading.setValue(true);

        repo.signIn(email, pass, new AuthRepository.LoginCallback() {
            @Override
            public void onSuccess(String uid, String authEmail) {
                repo.fetchProfile(uid, authEmail, new AuthRepository.ProfileCallback() {
                    @Override
                    public void onResult(String name, String role) {
                        loading.postValue(false);
                        result.postValue(new Event<>(LoginResult.ok(role, name)));
                    }
                    @Override
                    public void onNotFound(String message) {
                        loading.postValue(false);
                        result.postValue(new Event<>(LoginResult.fail("Tài khoản không tồn tại trong hệ thống.\nVui lòng liên hệ chủ quán.")));
                    }
                    @Override
                    public void onError(String message) {
                        loading.postValue(false);
                        result.postValue(new Event<>(LoginResult.fail("Không thể tải thông tin tài khoản.\nVui lòng thử lại.")));
                    }
                });
            }
            @Override
            public void onError(String message) {
                loading.postValue(false);
                String msg;
                if (message != null && message.contains("password is invalid")) {
                    msg = "Mật khẩu không đúng.\nVui lòng thử lại.";
                } else if (message != null && message.contains("no user record")) {
                    msg = "Email không tồn tại.\nVui lòng kiểm tra lại.";
                } else if (message != null && message.contains("network")) {
                    msg = "Mất kết nối mạng.\nVui lòng kiểm tra internet.";
                } else if (message != null && message.contains("blocked")) {
                    msg = "Tài khoản bị khóa tạm thời do nhập sai nhiều lần.\nVui lòng thử lại sau.";
                } else {
                    msg = "Đăng nhập thất bại.\nVui lòng kiểm tra email và mật khẩu.";
                }
                result.postValue(new Event<>(LoginResult.fail(msg)));
            }
        });
    }
}
