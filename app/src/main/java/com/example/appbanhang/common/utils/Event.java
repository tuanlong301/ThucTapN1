package com.example.appbanhang.common.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * One-shot event wrapper cho LiveData.
 * Tránh re-emit khi config change (xoay màn hình).
 * Dùng cho: Toast, navigation, dialog…
 */
public class Event<T> {
    private final T content;
    private boolean handled = false;

    public Event(@NonNull T content) {
        this.content = content;
    }

    /**
     * Trả nội dung nếu chưa xử lý, null nếu đã xử lý rồi.
     */
    @Nullable
    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }

    /** Luôn trả nội dung, bất kể đã handled hay chưa. */
    @NonNull
    public T peekContent() {
        return content;
    }
}
