package com.sarbondhu.app.data.repository;

import android.content.Context;

import com.sarbondhu.app.data.model.Admin;
import com.sarbondhu.app.data.model.Shop;
import com.sarbondhu.app.data.store.MockDataStore;

import java.io.IOException;
import java.util.Locale;

/** Checks usernames and passwords against the mock accounts. */
public class AuthRepository {

    public enum Role { ADMIN, DEALER }

    public enum LoginResult { SUCCESS, WRONG_CREDENTIALS, INACTIVE }

    public static final class LoginOutcome {
        private final LoginResult result;
        private final int userId;
        private final String displayName;

        LoginOutcome(LoginResult result, int userId, String displayName) {
            this.result = result;
            this.userId = userId;
            this.displayName = displayName;
        }

        public LoginResult getResult() {
            return result;
        }

        public int getUserId() {
            return userId;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private static volatile AuthRepository instance;

    private final MockDataStore store;

    public AuthRepository(MockDataStore store) {
        this.store = store;
    }

    public static AuthRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository(MockDataStore.getInstance(context));
                }
            }
        }
        return instance;
    }

    public LoginOutcome login(Role role, String username, String password) throws IOException {
        String wanted = normalise(username);
        synchronized (store) {
            store.ensureLoaded();
            if (role == Role.ADMIN) {
                for (Admin admin : store.admins()) {
                    if (normalise(admin.getUsername()).equals(wanted)
                            && admin.getPassword().equals(password)) {
                        return new LoginOutcome(LoginResult.SUCCESS, admin.getId(), admin.getName());
                    }
                }
            } else {
                for (Shop shop : store.shops()) {
                    if (normalise(shop.getUsername()).equals(wanted)
                            && shop.getPassword().equals(password)) {
                        return shop.isActive()
                                ? new LoginOutcome(LoginResult.SUCCESS, shop.getId(), shop.getShopName())
                                : new LoginOutcome(LoginResult.INACTIVE, 0, "");
                    }
                }
            }
            return new LoginOutcome(LoginResult.WRONG_CREDENTIALS, 0, "");
        }
    }

    static String normalise(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
