package com.sarbondhu.app.ui.admin;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.repository.DataException;
import com.sarbondhu.app.data.repository.DealerRepository;
import com.sarbondhu.app.ui.common.Errors;
import com.sarbondhu.app.ui.common.Event;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DealerFormViewModel extends AndroidViewModel {

    public enum Field { SHOP_NAME, OWNER_NAME, PHONE, DISTRICT, UPAZILA, USERNAME, PASSWORD }

    static final int MIN_PASSWORD_LENGTH = 6;

    private final DealerRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<Shop> existing = new MutableLiveData<>();
    private final MutableLiveData<Map<Field, Integer>> fieldErrors = new MutableLiveData<>();
    /** Message to show before closing the screen. */
    private final MutableLiveData<Event<Integer>> done = new MutableLiveData<>();
    /** Message to show while staying on the screen. */
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saving = new MutableLiveData<>(false);
    private int dealerId;
    private boolean started;

    public DealerFormViewModel(@NonNull Application application) {
        super(application);
        repository = DealerRepository.getInstance(application);
    }

    public LiveData<Shop> getExisting() {
        return existing;
    }

    public LiveData<Map<Field, Integer>> getFieldErrors() {
        return fieldErrors;
    }

    public LiveData<Event<Integer>> getDone() {
        return done;
    }

    public LiveData<Event<Integer>> getMessage() {
        return message;
    }

    /** True while a save is running; further taps are ignored. */
    public LiveData<Boolean> getSaving() {
        return saving;
    }

    /** @param id 0 for a new dealer */
    public void start(int id) {
        if (started) {
            return;
        }
        started = true;
        dealerId = id;
        if (id == 0) {
            return;
        }
        executor.execute(() -> {
            try {
                Shop shop = repository.getDealer(id);
                if (shop == null) {
                    message.postValue(new Event<>(R.string.error_not_found_item));
                } else {
                    existing.postValue(shop);
                }
            } catch (IOException e) {
                message.postValue(new Event<>(R.string.error_load_data));
            }
        });
    }

    public void save(String shopName, String ownerName, String phone, String district,
                     String upazila, String address, String username, String password,
                     boolean active) {
        if (Boolean.TRUE.equals(saving.getValue())) {
            return;
        }
        Map<Field, Integer> errors = new EnumMap<>(Field.class);
        require(errors, Field.SHOP_NAME, shopName);
        require(errors, Field.OWNER_NAME, ownerName);
        require(errors, Field.PHONE, phone);
        require(errors, Field.DISTRICT, district);
        require(errors, Field.UPAZILA, upazila);
        require(errors, Field.USERNAME, username);
        if (dealerId == 0 && password.length() < MIN_PASSWORD_LENGTH) {
            errors.put(Field.PASSWORD, R.string.error_password_short);
        }
        fieldErrors.setValue(errors);
        if (!errors.isEmpty()) {
            return;
        }

        Shop shop = new Shop(dealerId, shopName.trim(), ownerName.trim(), phone.trim(),
                district.trim(), upazila.trim(), address.trim(), username.trim(), password, active);
        saving.setValue(true);
        executor.execute(() -> {
            try {
                int id = dealerId;
                if (id == 0) {
                    id = repository.addDealer(shop).getId();
                } else {
                    repository.updateDealer(shop);
                }
                Shop saved = repository.getDealer(id);
                if (saved != null && saved.isActive() != active) {
                    repository.setActive(id, active);
                }
                done.postValue(new Event<>(R.string.saved));
            } catch (DataException e) {
                if (e.getReason() == DataException.Reason.USERNAME_TAKEN) {
                    Map<Field, Integer> taken = new EnumMap<>(Field.class);
                    taken.put(Field.USERNAME, R.string.error_username_taken);
                    fieldErrors.postValue(taken);
                } else {
                    message.postValue(new Event<>(Errors.messageFor(e.getReason())));
                }
                saving.postValue(false);
            } catch (IOException e) {
                message.postValue(new Event<>(R.string.error_load_data));
                saving.postValue(false);
            }
        });
    }

    /** @return an error message to show in the dialog, or 0 when the reset was started */
    public int resetPassword(String newPassword) {
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            return R.string.error_password_short;
        }
        executor.execute(() -> {
            try {
                repository.resetPassword(dealerId, newPassword);
                message.postValue(new Event<>(R.string.password_reset_done));
            } catch (DataException e) {
                message.postValue(new Event<>(Errors.messageFor(e.getReason())));
            } catch (IOException e) {
                message.postValue(new Event<>(R.string.error_load_data));
            }
        });
        return 0;
    }

    private static void require(Map<Field, Integer> errors, Field field, String value) {
        if (value.trim().isEmpty()) {
            errors.put(field, R.string.error_required);
        }
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
