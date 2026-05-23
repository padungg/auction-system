package com.auction.client.util;

import com.auction.model.dto.UserResponseDTO;

/**
 * <h2>SessionManager</h2>
 * <p>
 * Bộ quản lý phiên làm việc tập trung của người dùng (Global User Session Lifecycle Manager) phía Client.
 * </p>
 * * <p><b>Các đặc tính kiến trúc và mẫu thiết kế áp dụng:</b></p>
 * <ul>
 * <li><b>Bill Pugh Singleton Pattern:</b> Sử dụng lớp lồng tĩnh `InstanceHolder` để quản lý thực thể duy nhất. Cơ chế này giúp tối ưu hóa bộ nhớ RAM, đảm bảo tiến trình khởi tạo lười (Lazy Initialization) đạt hiệu năng cao và an toàn đa luồng tuyệt đối mà không cần chịu chi phí thắt nút cổ chai của khối khóa synchronized.</li>
 * <li><b>Thread-Safe State Visibility:</b> Biến trường thuộc tính `currentUser` được đánh dấu bằng từ khóa `volatile`. Điều này đảm bảo tính hiển thị nhất quán (Visibility) giữa các luồng hệ thống, giúp các luồng Worker chạy ngầm (như luồng Socket mạng cập nhật số dư) và luồng đồ họa JavaFX UI Thread luôn đọc được cùng một trạng thái dữ liệu chính xác nhất.</li>
 * <li><b>Mô hình lưu trữ trạng thái tập trung (Centralized State Repository):</b> Đóng vai trò làm cổng dữ liệu trung tâm, cho phép mọi phân hệ giao diện đồ họa (Controllers) kiểm tra thông tin cấu hình tài khoản, phân quyền hoặc số dư tài khoản thông qua cú pháp toàn cục: {@code SessionManager.getInstance().getCurrentUser()}.</li>
 * </ul>
 * * @since 1.0
 * @see com.auction.model.dto.UserResponseDTO
 */
public class SessionManager {

    /** * Đối tượng DTO bọc toàn bộ thông tin hồ sơ tài khoản đang đăng nhập trong phiên làm việc hiện tại.
     * Được cấu hình `volatile` nhằm đảm bảo tính toàn vẹn dữ liệu xuyên suốt môi trường đa luồng (Multi-threaded Environment).
     */
    private volatile UserResponseDTO currentUser;

    /**
     * Hàm khởi tạo cấu trúc Private (Private Constructor).
     * Ngăn chặn tuyệt đối hành vi khởi tạo thực thể tự do từ các thành phần bên ngoài hệ thống.
     */
    private SessionManager() {
    }

    /**
     * Lớp giữ thực thể tĩnh (Holder Class) hỗ trợ Bill Pugh Singleton Pattern.
     * Khởi tạo luồng luân chuyển vùng nhớ an toàn khi lớp được nạp vào bộ nhớ máy ảo JVM.
     */
    private static class InstanceHolder {
        private static final SessionManager INSTANCE = new SessionManager();
    }

    /**
     * Truy xuất thực thể duy nhất toàn cục của bộ quản lý phiên (Singleton Instance Accessor).
     * Đảm bảo kiểm soát an toàn và tập trung tuyệt đối luồng dữ liệu phiên làm việc của ứng dụng Client.
     * * @return {@link SessionManager} Thực thể quản lý phiên làm việc hiện hành
     */
    public static SessionManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Truy xuất thông tin chi tiết hồ sơ của người dùng đang đăng nhập hiện tại dưới dạng đối tượng DTO.
     * Phục vụ đắc lực cho công tác hiển thị tên tài khoản, avatar, mã ID hoặc kiểm tra số dư ví đấu giá trên UI.
     * * @return {@link UserResponseDTO} Thực thể chứa thông tin tài khoản người dùng, hoặc {@code null} nếu chưa đăng nhập
     */
    public UserResponseDTO getCurrentUser() {
        return currentUser;
    }

    /**
     * Gán dữ liệu hồ sơ người dùng vào vùng nhớ Session bộ nhớ tạm (Session State Mutator).
     * <p>
     * Phương thức này được kích hoạt tự động tại 2 thời điểm cốt lõi trong chu kỳ ứng dụng:
     * 1. Ngay sau khi tiến trình xác thực đăng nhập (Authentication) từ Server phản hồi kết quả thành công.
     * 2. Khi nhận được gói tin thông báo dội về từ luồng mạng báo hiệu số dư tài khoản vừa được thay đổi (Nạp tiền, trừ tiền đặt cọc).
     * </p>
     * * @param currentUser Thực thể hồ sơ thông tin mới {@link UserResponseDTO} cần cập nhật vào bộ đệm
     */
    public void setCurrentUser(UserResponseDTO currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Xóa sạch toàn bộ dữ liệu cấu trúc phiên làm việc hiện hành (Session Cache Cleansing).
     * Được kích hoạt khi người dùng nhấn nút Đăng xuất (Logout), cưỡng chế đưa trạng thái tài khoản về rỗng
     * nhằm cô lập dữ liệu bộ nhớ, chống các nguy cơ tấn công chiếm quyền hoặc rò rỉ thông tin cá nhân.
     */
    public void clear() {
        // Giải phóng vùng liên kết bộ nhớ đối tượng DTO cũ để dọn RAM
        this.currentUser = null;
    }

    /**
     * Xác thực trạng thái liên kết và đăng nhập của ứng dụng Client (Authentication State Guard).
     * Thường được sử dụng làm chốt chặn bảo mật (Filter) trước khi chuyển hướng người dùng vào các phân hệ màn hình chức năng nội bộ.
     * * @return {@code true} Nếu phiên làm việc đang tồn tại thực thể người dùng hợp lệ; {@code false} nếu ngược lại (Chưa đăng nhập hoặc đã đăng xuất)
     */
    public boolean isLoggedIn() {
        // Thực hiện phép kiểm thử logic so sánh con trỏ vùng nhớ
        return currentUser != null;
    }
}