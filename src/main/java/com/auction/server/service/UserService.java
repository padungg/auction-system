package com.auction.server.service;

import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.User;
import com.auction.model.entity.UserRole;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.UserDAO;
import com.auction.server.util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dịch vụ xử lý nghiệp vụ Tài khoản (User).
 * Hỗ trợ Đăng nhập, Đăng ký, Quản lý và Giao dịch Nạp/Rút tiền.
 */

public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Xử lý đăng nhập.
     * Trả về UserResponseDTO ĐẦY ĐỦ (bao gồm fullName, phone, address, ...)
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

        UserResponseDTO userResponseDTO = toFullDTO(user);
        LOGGER.info("LOGIN: {} | role={} -> SUCCESS", user.getUsername(), user.getRole());
        return new Response(ResponseStatus.SUCCESS, "Đăng nhập thành công!", userResponseDTO);
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     */
    public Response register(RegisterDTO dto) {
        // Validate
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
        // Tạo User mới
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
            return new Response(ResponseStatus.ERROR, "Lỗi máy chủ: Không thể lưu tài khoản vào Database (Vui lòng kiểm tra MySQL)", null);
        }

        UserResponseDTO responseDTO = toFullDTO(newUser);
        LOGGER.info("[UserService] REGISTER: {} | id={} -> SUCCESS", newUser.getUsername(), newUser.getId());
        return new Response(ResponseStatus.SUCCESS, "Đăng ký thành công!", responseDTO);
    }

    // ADMIN OPERATIONS

    /**
     * Lấy danh sách tất cả user
     */
    public Response getAllUsers() {
        List<User> users = userDAO.findAll();
        List<UserResponseDTO> dtos = new ArrayList<>();
        for (User u : users) {
            dtos.add(toFullDTO(u));
        }
        LOGGER.info("GET_ALL_USERS: {} users", dtos.size());
        return new Response(ResponseStatus.SUCCESS, "Lấy danh sách người dùng thành công", dtos);
    }

    /**
     * Khóa tài khoản user
     */
    public Response lockUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu userId", null);
        }
        User user = userDAO.findById(userId.trim());
        if (user == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy người dùng", null);
        }
        user.setActive(false);
        userDAO.update(user);
        LOGGER.info("LOCK_USER: {}", user.getUsername());
        return new Response(ResponseStatus.SUCCESS, "Đã khóa tài khoản: " + user.getUsername(), null);
    }

    /**
     * Mở khóa tài khoản user
     */
    public Response unlockUser(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu userId", null);
        }
        User user = userDAO.findById(userId.trim());
        if (user == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy người dùng", null);
        }
        user.setActive(true);
        userDAO.update(user);
        LOGGER.info("UNLOCK_USER: {}", user.getUsername());
        return new Response(ResponseStatus.SUCCESS, "Đã mở khóa tài khoản: " + user.getUsername(), null);
    }

    // ACCOUNT OPERATIONS (NẠP/RÚT TIỀN)

    /**
     * Nạp tiền vào tài khoản.
     */
    public Response deposit(String userId, double amount) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        if (amount <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Số tiền phải lớn hơn 0", null);
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
        }
        user.deposit(amount);
        userDAO.update(user);
        LOGGER.info("DEPOSIT: user={} | +{} VNĐ | balance={} VNĐ",
                user.getUsername(),
                String.format("%,.0f", amount),
                String.format("%,.0f", user.getBalance()));
        return new Response(ResponseStatus.SUCCESS,
                "Nạp tiền thành công! Số dư: " + String.format("%,.0f", user.getBalance()) + " VNĐ",
                user.getBalance());
    }

    /**
     * Rút tiền khỏi tài khoản.
     */
    public Response withdraw(String userId, double amount) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        if (amount <= 0) {
            return new Response(ResponseStatus.BAD_REQUEST, "Số tiền phải lớn hơn 0", null);
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
        }
        if (user.getBalance() < amount) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Số dư không đủ! Hiện có: " + String.format("%,.0f", user.getBalance()) + " VNĐ", null);
        }
        user.withdraw(amount);
        userDAO.update(user);
        LOGGER.info("WITHDRAW: user={} | -{} VNĐ | balance={} VNĐ",
                user.getUsername(),
                String.format("%,.0f", amount),
                String.format("%,.0f", user.getBalance()));
        return new Response(ResponseStatus.SUCCESS,
                "Rút tiền thành công! Số dư: " + String.format("%,.0f", user.getBalance()) + " VNĐ",
                user.getBalance());
    }

    // GET MY PROFILE — Refresh bảng tài khoản từ DB

    public Response getMyProfile(String userId) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Chưa đăng nhập", null);
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
        }
        return new Response(ResponseStatus.SUCCESS, "OK", toFullDTO(user));
    }

    // UPDATE PROFILE — Cập nhật hồ sơ cá nhân

    public Response updateProfile(String userId, java.util.Map<String, String> payload) {
        if (userId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Chưa đăng nhập", null);
        }
        User user = userDAO.findById(userId);
        if (user == null) {
            return new Response(ResponseStatus.NOT_FOUND, "Không tìm thấy tài khoản", null);
        }

        String fullName  = payload.getOrDefault("fullName", "").trim();
        String phone     = payload.getOrDefault("phone", "").trim();
        String address   = payload.getOrDefault("address", "").trim();
        String storeName = payload.getOrDefault("storeName", "").trim();
        String password  = payload.getOrDefault("password", "").trim();

        if (fullName.isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Họ và tên không được để trống", null);
        }

        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        user.setStoreName(storeName);
        if (!password.isEmpty()) {
            user.setPassword(password);
        }

        userDAO.update(user);
        LOGGER.info("UPDATE_PROFILE: user={} | fullName={}", user.getUsername(), fullName);
        return new Response(ResponseStatus.SUCCESS, "Cập nhật hồ sơ thành công!", toFullDTO(user));
    }

    // HELPER

    /**
     * Chuyển User entity → UserResponseDTO đầy đủ (KHÔNG chứa password).
     */
    private UserResponseDTO toFullDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getBalance(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getStoreName(),
                user.getRating(),
                user.isActive()
        );
    }
}
