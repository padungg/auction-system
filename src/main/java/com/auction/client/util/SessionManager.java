package com.auction.client.util;

import com.auction.model.dto.UserResponseDTO;

/**
 * Bộ quản lý phiên làm việc của người dùng (User Session Manager).
 * Áp dụng thiết kế mẫu đơn thực thể (Singleton Pattern) để duy trì duy nhất một vùng không gian lưu trữ
 * thông tin tài khoản đang đăng nhập trong suốt vòng đời vận hành của ứng dụng Client.
 * * Nguyên lý hoạt động:
 * 1. Sau khi tiến trình xác thực đăng nhập (Login) thành công, Bộ điều khiển chuyển hướng sẽ cấu hình gán dữ liệu UserResponseDTO vào đây.
 * 2. Tất cả các phân hệ giao diện hoặc Controller khác trong hệ thống có thể truy cập, kiểm tra thông tin hoặc số dư tài khoản
 * một cách nhanh chóng thông qua cú pháp: SessionManager.getInstance().getCurrentUser().
 */
public class SessionManager {

    private static SessionManager instance;
    private UserResponseDTO currentUser;

    private SessionManager() {
    }

    /**
     * Lấy ra thực thể duy nhất của SessionManager (Singleton Pattern).
     * Đảm bảo kiểm soát an toàn và tập trung luồng dữ liệu phiên làm việc.
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Truy xuất thông tin hồ sơ của người dùng đang đăng nhập hiện tại dưới dạng đối tượng DTO.
     */
    public UserResponseDTO getCurrentUser() {
        return currentUser;
    }

    /**
     * Gán dữ liệu hồ sơ người dùng vào vùng nhớ Session bộ nhớ tạm sau khi xác thực thành công hoặc sau khi làm mới số dư.
     */
    public void setCurrentUser(UserResponseDTO currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Xóa sạch toàn bộ dữ liệu phiên làm việc (Session Cache) khi người dùng kích hoạt hành động Đăng xuất (Logout),
     * đưa trạng thái tài khoản hiện hành về rỗng nhằm bảo mật an toàn thông tin.
     */
    public void clear() {
        this.currentUser = null;
    }

    /**
     * Kiểm tra trạng thái đăng nhập của ứng dụng Client.
     * Trả về kết quả true nếu phiên làm việc hiện tại đang tồn tại thực thể người dùng hợp lệ.
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}