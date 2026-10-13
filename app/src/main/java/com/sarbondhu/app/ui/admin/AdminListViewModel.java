package com.sarbondhu.app.ui.admin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.ui.common.UiState;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loads one admin screen's data off the main thread. */
public abstract class AdminListViewModel<T> extends AndroidViewModel {

    protected final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UiState<T>> state = new MutableLiveData<>();

    protected AdminListViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<UiState<T>> getState() {
        return state;
    }

    /** Shows the spinner only on the first load; later loads swap data in place. */
    public void load() {
        if (state.getValue() == null) {
            state.setValue(UiState.loading());
        }
        executor.execute(() -> {
            try {
                state.postValue(UiState.success(fetch()));
            } catch (IOException e) {
                state.postValue(UiState.error(R.string.error_load_data));
            }
        });
    }

    protected abstract T fetch() throws IOException;

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
