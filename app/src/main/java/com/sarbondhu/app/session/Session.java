package com.sarbondhu.app.session;

import com.sarbondhu.app.data.repository.AuthRepository.Role;

/** Who is logged in. In memory only: closing the app logs everyone out. */
public final class Session {

    private static final Session INSTANCE = new Session();

    private Role role;
    private int userId;
    private String displayName = "";

    private Session() {
    }

    public static Session get() {
        return INSTANCE;
    }

    public synchronized void login(Role role, int userId, String displayName) {
        this.role = role;
        this.userId = userId;
        this.displayName = displayName;
    }

    public synchronized void logout() {
        role = null;
        userId = 0;
        displayName = "";
    }

    public synchronized boolean isLoggedIn() {
        return role != null;
    }

    public synchronized boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public synchronized boolean isDealer() {
        return role == Role.DEALER;
    }

    public synchronized int getUserId() {
        return userId;
    }

    public synchronized String getDisplayName() {
        return displayName;
    }
}
