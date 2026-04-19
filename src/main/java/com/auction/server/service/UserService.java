package com.auction.server.service;

import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.User;
import com.auction.model.entity.UserRole;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.UserDAO;

import java.util.UUID;

/**
 * Service xử lý nghiệp vụ liên quan đến User.
 *
 * Chức năng:
 *   - Login: xác thực tài khoản + mật khẩu
 *   - Register: đăng ký tài khoản mới với kiểm tra trùng username
 *
 * Service KHÔNG truy cập database trực tiếp — luôn gọi qua DAO interface.
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Xử lý đăng nhập.
     *
     * Luồng:
     *   1. Tìm user theo username
     *   2. So sánh password
     *   3. Kiểm tra tài khoản có bị khóa không
     *   4. Trả về UserResponseDTO (KHÔNG chứa password) nếu thành công
     */
    public Response login(LoginDTO dto) {
        // Validation đầu vào
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đăng nhập", null);
        }

        if (dto.getUsername().trim().isEmpty() || dto.getPassword().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Username và password không được để trống", null);
        }

        // Tìm user trong database
        User user = userDAO.findByUsername(dto.getUsername().trim());

        // Kiểm tra tồn tại
        if (user == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Tài khoản không tồn tại", null);
        }

        // Kiểm tra password
        if (!user.getPassword().equals(dto.getPassword())) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Sai mật khẩu", null);
        }

        // Kiểm tra tài khoản có bị khóa không
        if (!user.isActive()) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Tài khoản đã bị khóa bởi Admin", null);
        }

        // Tạo DTO trả về (KHÔNG chứa password để bảo mật)
        UserResponseDTO responseDTO = new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getBalance()
        );

        System.out.println(">>> [UserService] Đăng nhập thành công: " + user.getUsername());
        return new Response(ResponseStatus.SUCCESS, "Đăng nhập thành công!", responseDTO);
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     *
     * Luồng:
     *   1. Kiểm tra username đã tồn tại chưa
     *   2. Tạo User entity mới với role = MEMBER, balance = 0
     *   3. Lưu vào database
     */
    public Response register(RegisterDTO dto) {
        // Validation đầu vào
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đăng ký", null);
        }

        if (dto.getUsername().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Username không được để trống", null);
        }

        if (dto.getPassword().length() < 3) {
            return new Response(ResponseStatus.BAD_REQUEST, "Mật khẩu phải có ít nhất 3 ký tự", null);
        }

        // Kiểm tra trùng username
        if (userDAO.existsByUsername(dto.getUsername().trim())) {
            return new Response(ResponseStatus.BAD_REQUEST, "Username đã tồn tại, vui lòng chọn tên khác", null);
        }

        // Tạo User entity mới
        User newUser = new User(
                UUID.randomUUID().toString(),   // Tạo ID tự động
                dto.getUsername().trim(),
                dto.getPassword(),
                dto.getEmail(),
                dto.getFullName(),
                dto.getPhone(),
                dto.getAddress(),
                true,                           // isActive = true (mặc định kích hoạt)
                UserRole.MEMBER,                // Đăng ký mới luôn là MEMBER
                0.0,                            // balance = 0
                null,                           // storeName = null
                0.0                             // rating = 0
        );

        // Lưu vào database
        boolean saved = userDAO.save(newUser);
        if (!saved) {
            return new Response(ResponseStatus.ERROR, "Lỗi hệ thống khi lưu tài khoản", null);
        }

        System.out.println(">>> [UserService] Đăng ký thành công: " + newUser.getUsername());
        return new Response(ResponseStatus.SUCCESS, "Đăng ký thành công! Hãy đăng nhập.", null);
    }
}
