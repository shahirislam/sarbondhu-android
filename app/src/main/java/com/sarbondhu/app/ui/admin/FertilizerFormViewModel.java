package com.sarbondhu.app.ui.admin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Fertilizer;
import com.sarbondhu.app.data.repository.DataException;
import com.sarbondhu.app.data.repository.FertilizerRepository;
import com.sarbondhu.app.ui.common.Errors;
import com.sarbondhu.app.ui.common.Event;
import com.sarbondhu.app.util.NumberInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FertilizerFormViewModel extends AndroidViewModel {

    public enum Field { NAME, CATEGORY, PRICE, UNIT }

    private final FertilizerRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Fertilizer> existing = new MutableLiveData<>();
    private final MutableLiveData<Map<Field, Integer>> fieldErrors = new MutableLiveData<>();
    /** Message to show before closing the screen. */
    private final MutableLiveData<Event<Integer>> done = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> failure = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private int fertilizerId;
    private boolean started;

    public FertilizerFormViewModel(@NonNull Application application) {
        super(application);
        repository = FertilizerRepository.getInstance(application);
    }

    public LiveData<Fertilizer> getExisting() {
        return existing;
    }

    public LiveData<Map<Field, Integer>> getFieldErrors() {
        return fieldErrors;
    }

    public LiveData<Event<Integer>> getDone() {
        return done;
    }

    public LiveData<Event<Integer>> getFailure() {
        return failure;
    }

    /** True while a save or delete is running; further taps are ignored. */
    public LiveData<Boolean> getSaving() {
        return saving;
    }

    /** @param id 0 for a new fertilizer */
    public void start(int id) {
        if (started) {
            return;
        }
        started = true;
        fertilizerId = id;
        if (id == 0) {
            return;
        }
        executor.execute(() -> {
            try {
                Fertilizer fertilizer = repository.getFertilizer(id);
                if (fertilizer == null) {
                    failure.postValue(new Event<>(R.string.error_not_found_item));
                } else {
                    existing.postValue(fertilizer);
                }
            } catch (IOException e) {
                failure.postValue(new Event<>(R.string.error_load_data));
            }
        });
    }

    public void save(String name, String category, String npk, String priceText, String unit,
                     String description, String usage, String cropsText) {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        Map<Field, Integer> errors = new EnumMap<>(Field.class);
        if (name.trim().isEmpty()) {
            errors.put(Field.NAME, R.string.error_required);
        }
        if (category.trim().isEmpty()) {
            errors.put(Field.CATEGORY, R.string.error_required);
        }
        Double price = NumberInput.parse(priceText);
        if (price == null || price < 0) {
            errors.put(Field.PRICE, R.string.error_invalid_number);
        }
        if (unit.trim().isEmpty()) {
            errors.put(Field.UNIT, R.string.error_required);
        }
        fieldErrors.setValue(errors);
        if (!errors.isEmpty()) {
            return;
        }

        Fertilizer fertilizer = new Fertilizer(fertilizerId, name.trim(), category.trim(), npk.trim(),
                description.trim(), usage.trim(), splitCrops(cropsText), price, unit.trim());
        saving.setValue(true);
        executor.execute(() -> {
            try {
                if (fertilizerId == 0) {
                    repository.addFertilizer(fertilizer);
                } else {
                    repository.updateFertilizer(fertilizer);
                }
                done.postValue(new Event<>(R.string.saved));
            } catch (DataException e) {
                failure.postValue(new Event<>(Errors.messageFor(e.getReason())));
                saving.postValue(false);
            } catch (IOException e) {
                failure.postValue(new Event<>(R.string.error_load_data));
                saving.postValue(false);
            }
        });
    }

    public void delete() {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        saving.setValue(true);
        executor.execute(() -> {
            try {
                repository.deleteFertilizer(fertilizerId);
                done.postValue(new Event<>(R.string.deleted));
            } catch (DataException e) {
                failure.postValue(new Event<>(Errors.messageFor(e.getReason())));
                saving.postValue(false);
            } catch (IOException e) {
                failure.postValue(new Event<>(R.string.error_load_data));
                saving.postValue(false);
            }
        });
    }

    /** Comma-separated crops, trimmed, blanks dropped. */
    static List<String> splitCrops(String text) {
        List<String> crops = new ArrayList<>();
        for (String part : text.split(",")) {
            if (!part.trim().isEmpty()) {
                crops.add(part.trim());
            }
        }
        return crops;
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
