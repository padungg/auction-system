package com.auction.server.service;

import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.User;
import com.auction.model.entity.UserRole;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.UserDAO;
import com.auction.server.util.LockManager;
import com.auction.server.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dịch vụ quản lý tài khoản người dùng.
 * Chịu trách nhiệm: Đăng nhập, Đăng ký, Quản lý trạng thái tài khoản (Admin).
 */
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserDAO userDAO;
    private final UserMapper userMapper;

    public UserService(UserDAO userDAO, UserMapper userMapper) {
        this.userDAO = userDAO;
        this.userMapper = userMapper;
    }

    /** Convenience constructor — tạo UserMapper mặc định. */
    public UserService(UserDAO userDAO) {
        this(userDAO, new UserMapper());
    }

    // AUTH

    /**
     * Xử lý đăng nhập.
     * Trả về UserResponseDTO đầy đủ (không chứa password).
     */
    public Response login(LoginDTO dto) {
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đăng nhập", null);
        }
        if (dto.getUsername().trim().isEmpty() || dto.getPassword().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Username và password không được để trống", null);
        }
        User user = userDAO.findByUsername(dto.getUsername().trim());
        if (user == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Tài khoản không tồn tại", null);
        }
        if (!user.getPassword().equals(dto.getPassword())) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Sai mật khẩu", null);
        }
        if (!user.isActive()) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Tài khoản đã bị khóa", null);
        }

        LOGGER.info("LOGIN: {} | role={} -> SUCCESS", user.getUsername(), user.getRole());
        return new Response(ResponseStatus.SUCCESS, "Đăng nhập thành công!", userMapper.toFullDTO(user));
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     */
    public Response register(RegisterDTO dto) {
        if (dto == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đăng ký", null);
        }
        ValidationUtils.requireNonBlank(dto.getUsername(), "Username");
        ValidationUtils.requireNonBlank(dto.getPassword(), "Password");
        ValidationUtils.requireNonBlank(dto.getEmail(), "Email");
        ValidationUtils.requireValidEmail(dto.getEmail());

        if (userDAO.existsByUsername(dto.getUsername().trim())) {
            return new Response(ResponseStatus.BAD_REQUEST, "Username đã tồn tại, vui lòng chọn tên khác", null);
        }

        User newUser = new User(
                UUID.randomUUID().toString(),
                dto.getUsername().trim(),
                dto.getPassword(),
                dto.getEmail().trim(),
                dto.getFullName() != null ? dto.getFullName().trim() : "",
                dto.getPhone() != null ? dto.getPhone() : "",
                dto.getAddress() != null ? dto.getAddress() : "",
                true,
                UserRole.MEMBER,
                0.0,
                dto.getStoreName() != null ? dto.getStoreName() : null,
                0.0
        );
        boolean saved = userDAO.save(newUser);
        if (!saved) {
            return new Response(ResponseStatus.ERROR,
                    "Lỗi máy chủ: Không thể lưu tài khoản vào Database (Vui lòng kiểm tra MySQL)", null);
        }

        LOGGER.info("REGISTER: {} | id={} -> SUCCESS", newUser.getUsername(), newUser.getId());
        return new Response(ResponseStatus.SUCCESS, "Đăng ký thành công!", userMapper.toFullDTO(newUser));
    }

    // ADMIN
    /**
     * Lấy danh sách tất cả user (Admin).
     */
    public Response getAllUsers() {
        List<User> users = userDAO.findAll();
        List<UserResponseDTO> dtos = new ArrayList<>();
        for (User u : users) {
            dtos.add(userMapper.toFullDTO(u));
        }
        LOGGER.info("GET_ALL_USERS: {} users", dtos.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy danh sách người dùng thành công", dtos);
    }

    /**
     * Khóa tài khoản user (Admin).
     */
    public Response lockUser(String userId) {
        return toggleUserStatus(userId, false, "LOCK_USER", "Đã khóa tài khoản: ");
    }

    /**
     * Mở khóa tài khoản user (Admin).
     */
    public Response unlockUser(String userId) {
        return toggleUserStatus(userId, true, "UNLOCK_USER", "Đã mở khóa tài khoản: ");
    }

    // PRIVATE

    private Response toggleUserStatus(String userId, boolean status, String logAction, String successMsg) {
        if (userId == null || userId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu userId", null);
        }

        Object lock = LockManager.getUserLock(userId.trim());
        synchronized (lock) {
            User user = userDAO.findById(userId.trim());
            if (user == null) {
                return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy người dùng", null);
            }
            user.setActive(status);
            userDAO.update(user);
            LOGGER.info("{}: {}", logAction, user.getUsername());
            return new Response(ResponseStatus.SUCCESS, successMsg + user.getUsername(), null);
        }
    }
}
