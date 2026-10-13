package com.sarbondhu.app.ui.fertilizerdetail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.FertilizerDetail;
import com.sarbondhu.app.data.repository.FertilizerRepository;
import com.sarbondhu.app.ui.common.UiState;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FertilizerDetailViewModel extends AndroidViewModel {

    private final FertilizerRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UiState<FertilizerDetail>> state = new MutableLiveData<>();
    private boolean started;

    public FertilizerDetailViewModel(@NonNull Application application) {
        super(application);
        repository = FertilizerRepository.getInstance(application);
    }

    public LiveData<UiState<FertilizerDetail>> getState() {
        return state;
    }

    /** Loads once; later calls (e.g. after rotation) reuse the existing state. */
    public void load(int fertilizerId) {
        if (started) {
            return;
        }
        started = true;
        state.setValue(UiState.loading());
        executor.execute(() -> {
            try {
                FertilizerDetail detail = repository.getFertilizerDetail(fertilizerId);
                state.postValue(detail == null
                        ? UiState.error(R.string.error_not_found)
                        : UiState.success(detail));
            } catch (IOException e) {
                state.postValue(UiState.error(R.string.error_load_data));
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
