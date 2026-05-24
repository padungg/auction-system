package com.auction.client.util;

import com.auction.model.dto.UserResponseDTO;

/**
 * Bộ quản lý phiên làm việc tập trung của người dùng phía Client.
 */
public class SessionManager {

    private volatile UserResponseDTO currentUser;

    private SessionManager() {
    }

    private static class InstanceHolder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    public static SessionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public UserResponseDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserResponseDTO currentUser) {
        this.currentUser = currentUser;
    }

    public void clear() {
        this.currentUser = null;
    }

    @SuppressWarnings("unused")
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}