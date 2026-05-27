package com.auction.client.util;

import com.auction.model.dto.UserResponseDTO;

/**
 * Quản lý thông tin phiên đăng nhập (User Session) phía Client.
 * Sử dụng Singleton Pattern để đảm bảo an toàn truy cập từ nhiều luồng.
 */
public class SessionManager {

    private volatile UserResponseDTO currentUser;

    private SessionManager() {
    }

    private static class InstanceHolder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    /**
     * Lấy instance duy nhất (Singleton) của SessionManager.
     */
    public static SessionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Lấy thông tin người dùng đang đăng nhập.
     */
    public UserResponseDTO getCurrentUser() {
        return currentUser;
    }

    /**
     * Thiết lập thông tin người dùng đăng nhập.
     */
    public void setCurrentUser(UserResponseDTO currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Đăng xuất, xóa thông tin phiên hiện tại.
     */
    public void clear() {
        this.currentUser = null;
    }

    /**
     * Kiểm tra xem người dùng đã đăng nhập chưa.
     */
    @SuppressWarnings("unused")
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}