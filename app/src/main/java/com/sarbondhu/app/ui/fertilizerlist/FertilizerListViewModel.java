package com.sarbondhu.app.ui.fertilizerlist;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.HomeData;
import com.sarbondhu.app.data.repository.FertilizerRepository;
import com.sarbondhu.app.ui.common.UiState;
import com.sarbondhu.app.util.BanglaCalendar;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FertilizerListViewModel extends AndroidViewModel {

    private static final long CLOCK_TICK_MS = 1000;

    private final FertilizerRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UiState<HomeData>> state = new MutableLiveData<>();
    private final MutableLiveData<ClockState> clock = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            clock.setValue(readClock());
            handler.postDelayed(this, CLOCK_TICK_MS);
        }
    };

    public FertilizerListViewModel(@NonNull Application application) {
        super(application);
        repository = FertilizerRepository.getInstance(application);
        tick.run();
        load();
    }

    public LiveData<UiState<HomeData>> getState() {
        return state;
    }

    public LiveData<ClockState> getClock() {
        return clock;
    }

    public void load() {
        UiState<HomeData> current = state.getValue();
        if (current != null && current.getStatus() == UiState.Status.LOADING) {
            return;
        }
        state.setValue(UiState.loading());
        executor.execute(() -> {
            try {
                state.postValue(UiState.success(repository.getHomeData()));
            } catch (IOException e) {
                state.postValue(UiState.error(R.string.error_load_data));
            }
        });
    }

    /** Re-reads data without showing the spinner, so edits made elsewhere appear seamlessly. */
    public void refresh() {
        UiState<HomeData> current = state.getValue();
        if (current == null || current.getStatus() != UiState.Status.SUCCESS) {
            return;
        }
        executor.execute(() -> {
            try {
                state.postValue(UiState.success(repository.getHomeData()));
            } catch (IOException e) {
                // Keep showing the data we already have.
            }
        });
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacks(tick);
        executor.shutdownNow();
    }

    private static ClockState readClock() {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        return new ClockState(
                BanglaCalendar.getBanglaTime(hour, now.get(Calendar.MINUTE)),
                BanglaCalendar.getBanglaDay(now.get(Calendar.DAY_OF_WEEK)),
                BanglaCalendar.getBanglaDate(now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH)),
                BanglaCalendar.isDay(hour));
    }
}
