package com.sarbondhu.app.ui.login;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sarbondhu.app.R;
import com.sarbondhu.app.data.repository.AuthRepository;
import com.sarbondhu.app.data.repository.AuthRepository.LoginOutcome;
import com.sarbondhu.app.data.repository.AuthRepository.Role;
import com.sarbondhu.app.session.Session;
import com.sarbondhu.app.ui.common.Event;
import com.sarbondhu.app.ui.common.UiState;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository auth;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final MutableLiveData<UiState<Role>> state = new MutableLiveData<>();
    private final MutableLiveData<Event<Role>> loggedIn = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);
        auth = AuthRepository.getInstance(application);
    }

    /** LOADING while checking, ERROR with a message; success is reported through {@link #getLoggedIn()}. */
    public LiveData<UiState<Role>> getState() {
        return state;
    }

    public LiveData<Event<Role>> getLoggedIn() {
        return loggedIn;
    }

    public void login(Role role, String username, String password) {
        if (username.trim().isEmpty() || password.isEmpty()) {
            state.setValue(UiState.error(R.string.error_fields_required));
            return;
        }
        state.setValue(UiState.loading());
        executor.execute(() -> {
            try {
                LoginOutcome outcome = auth.login(role, username, password);
                switch (outcome.getResult()) {
                    case SUCCESS:
                        Session.get().login(role, outcome.getUserId(), outcome.getDisplayName());
                        state.postValue(UiState.success(role));
                        loggedIn.postValue(new Event<>(role));
                        break;
                    case INACTIVE:
                        state.postValue(UiState.error(R.string.error_dealer_inactive));
                        break;
                    default:
                        state.postValue(UiState.error(R.string.error_wrong_credentials));
                }
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
