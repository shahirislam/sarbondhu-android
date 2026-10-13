package com.sarbondhu.app.ui.dealer;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.repository.DataException;
import com.sarbondhu.app.data.repository.DealerRepository;
import com.sarbondhu.app.session.Session;
import com.sarbondhu.app.ui.admin.AdminListViewModel;
import com.sarbondhu.app.ui.common.Errors;
import com.sarbondhu.app.ui.common.Event;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

public class ProfileViewModel extends AdminListViewModel<Shop> {

    public enum Field { CURRENT, NEW, CONFIRM }

    static final int MIN_PASSWORD_LENGTH = 6;

    private final DealerRepository dealers;
    private final MutableLiveData<Map<Field, Integer>> fieldErrors = new MutableLiveData<>();
    /** Bangla message; changed == true means the form should be cleared. */
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> changed = new MutableLiveData<>();
    private boolean saving;

    /** Posted from the worker so the flag is cleared on the main thread. */
    private final MutableLiveData<Boolean> savingDone = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        dealers = DealerRepository.getInstance(application);
        savingDone.observeForever(done -> saving = false);
    }

    public LiveData<Map<Field, Integer>> getFieldErrors() {
        return fieldErrors;
    }

    public LiveData<Event<Integer>> getMessage() {
        return message;
    }

    public LiveData<Event<Boolean>> getChanged() {
        return changed;
    }

    public void changePassword(String current, String newPassword, String confirm) {
        if (saving) {
            return;
        }
        Map<Field, Integer> errors = new EnumMap<>(Field.class);
        if (current.isEmpty()) {
            errors.put(Field.CURRENT, R.string.error_required);
        }
        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            errors.put(Field.NEW, R.string.error_password_short);
        } else if (!newPassword.equals(confirm)) {
            errors.put(Field.CONFIRM, R.string.error_password_mismatch);
        }
        fieldErrors.setValue(errors);
        if (!errors.isEmpty()) {
            return;
        }
        int dealerId = Session.get().getUserId();
        saving = true;
        executor.execute(() -> {
            try {
                dealers.changePassword(dealerId, current, newPassword);
                message.postValue(new Event<>(R.string.password_reset_done));
                changed.postValue(new Event<>(true));
            } catch (DataException e) {
                if (e.getReason() == DataException.Reason.WRONG_PASSWORD) {
                    Map<Field, Integer> wrong = new EnumMap<>(Field.class);
                    wrong.put(Field.CURRENT, R.string.error_wrong_current_password);
                    fieldErrors.postValue(wrong);
                } else {
                    message.postValue(new Event<>(Errors.messageFor(e.getReason())));
                }
            } catch (IOException e) {
                message.postValue(new Event<>(R.string.error_load_data));
            }
            savingDone.postValue(true);
        });
    }

    @Override
    protected Shop fetch() throws IOException {
        return dealers.getDealer(Session.get().getUserId());
    }
}
