package com.auction.server.dao;

import com.auction.model.entity.User;

/**
 * Interface DAO cho User — "Hợp đồng" giữa Server và tầng Database.
 *
 * Người làm DAO sẽ implements interface này và viết SQL queries thật.
 * Server chỉ gọi các method qua interface, KHÔNG phụ thuộc vào implementation cụ thể.
 */
public interface UserDAO {

    /**
     * Tìm user theo username (dùng khi Login).
     * @return User nếu tìm thấy, null nếu không tồn tại
     */
    User findByUsername(String username);

    /**
     * Tìm user theo ID (dùng khi cần lấy thông tin seller, winner).
     * @return User nếu tìm thấy, null nếu không tồn tại
     */
    User findById(String id);

    /**
     * Lưu user mới vào database (dùng khi Đăng ký).
     * @return true nếu lưu thành công
     */
    boolean save(User user);

    /**
     * Kiểm tra username đã tồn tại chưa (dùng trước khi Đăng ký).
     * @return true nếu username đã có người dùng
     */
    boolean existsByUsername(String username);
}
