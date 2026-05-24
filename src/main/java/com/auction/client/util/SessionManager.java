package com.auction.client.util;

import com.auction.model.dto.UserResponseDTO;

/**
 * Bộ quản lý phiên làm việc tập trung (User Session) của người dùng phía Client.
 * Sử dụng mẫu thiết kế Singleton (Lazy Initialization qua Holder class) để đảm bảo an toàn đa luồng.
 */
public class SessionManager {

    private volatile UserResponseDTO currentUser;

    private SessionManager() {
    }

    private static class InstanceHolder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    /**
     * Lấy thực thể duy nhất của SessionManager.
     */
    public static SessionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Lấy thông tin của người dùng đang đăng nhập hiện tại.
     */
    public UserResponseDTO getCurrentUser() {
        return currentUser;
    }

    /**
     * Thiết lập thông tin người dùng đăng nhập hiện tại khi đăng nhập thành công.
     */
    public void setCurrentUser(UserResponseDTO currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Xóa thông tin phiên làm việc hiện tại (Đăng xuất).
     */
    public void clear() {
        this.currentUser = null;
    }

    /**
     * Kiểm tra người dùng đã đăng nhập hay chưa.
     */
    @SuppressWarnings("unused")
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}