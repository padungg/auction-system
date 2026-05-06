package com.auction.server.dao;

import com.auction.model.entity.User;
import com.auction.model.entity.UserRole;

import java.util.ArrayList;
import java.util.List;

/**
 * Mock implementation của UserDAO — dùng ArrayList thay cho MySQL.
 *
 * MỤC ĐÍCH: Cho phép Server chạy và test NGAY khi chưa có database thật.
 * Khi người làm DAO hoàn thành UserDAOImpl (MySQL), chỉ cần thay dòng:
 * UserDAO userDAO = new UserDAOMock();
 * thành:
 * UserDAO userDAO = new UserDAOImpl();
 *
 * ⚠️ Dữ liệu sẽ MẤT khi tắt server (vì chỉ lưu trên RAM).
 */
public class UserDAOMock implements UserDAO {

    private final List<User> users = new ArrayList<>();

    public UserDAOMock() {
        // Thêm dữ liệu mẫu để test
        users.add(new User("user-001", "admin", "123", "admin@auction.vn",
                "Quản trị viên", "0901234567", "Hà Nội",
                true, UserRole.ADMIN, 10000000, "Admin Shop", 5.0));

        users.add(new User("user-002", "member1", "123", "member1@auction.vn",
                "Nguyễn Văn A", "0912345678", "TP.HCM",
                true, UserRole.MEMBER, 5000000, null, 4.0));

        users.add(new User("user-003", "member2", "123", "member2@auction.vn",
                "Trần Thị B", "0923456789", "Đà Nẵng",
                true, UserRole.MEMBER, 3000000, null, 3.5));

        System.out.println(">>> [UserDAOMock] Khởi tạo với " + users.size() + " user mẫu");
    }

    @Override
    public User findByUsername(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    @Override
    public User findById(String id) {
        return users.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean save(User user) {
        return users.add(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return users.stream().anyMatch(u -> u.getUsername().equals(username));
    }
}
