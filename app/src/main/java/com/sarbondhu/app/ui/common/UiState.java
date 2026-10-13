package com.sarbondhu.app.ui.common;

import androidx.annotation.StringRes;

/** What a screen should show: a spinner, the data, or an error message. */
public final class UiState<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    private final Status status;
    private final T data;
    @StringRes
    private final int messageRes;

    private UiState(Status status, T data, @StringRes int messageRes) {
        this.status = status;
        this.data = data;
        this.messageRes = messageRes;
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, 0);
    }

    public static <T> UiState<T> success(T data) {
        return new UiState<>(Status.SUCCESS, data, 0);
    }

    public static <T> UiState<T> error(@StringRes int messageRes) {
        return new UiState<>(Status.ERROR, null, messageRes);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    @StringRes
    public int getMessageRes() {
        return messageRes;
    }
}
