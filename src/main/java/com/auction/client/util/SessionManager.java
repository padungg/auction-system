package com.auction.client.util;

import com.auction.model.dto.UserResponseDTO;

/**
 * Singleton lưu trữ thông tin user đang đăng nhập.
 * Sau khi login thành công, Controller lưu UserResponseDTO vào đây.
 * Các Controller khác truy cập qua SessionManager.getInstance().getCurrentUser().
 */
public class SessionManager {

    private static SessionManager instance;
    private UserResponseDTO currentUser;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public UserResponseDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserResponseDTO currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Xóa session khi logout.
     */
    public void clear() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
