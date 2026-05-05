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
 *   - Register: đăng ký tài khoản mới, kiểm tra trùng username
 *
 * Pattern: Service layer — chỉ chứa logic nghiệp vụ, không biết gì về mạng/socket.
 */

public class UserService {
    private final UserDAO  userDAO;
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
    public Response login(LoginDTO dto){
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null){
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đăng nhập", null);
        }
        if (dto.getUsername().trim().isEmpty() || dto.getPassword().trim().isEmpty()){
            return new Response(ResponseStatus.BAD_REQUEST, "Username và password không được để trống", null);
        }
        User user = userDAO.findByUsername(dto.getUsername().trim());
        if (user == null){
            return new Response(ResponseStatus.UNAUTHORIZED, "Tài khoản không tồn tại", null);
        }
        if (!user.getPassword().equals(dto.getPassword())){
            return new Response(ResponseStatus.UNAUTHORIZED, "Sai mật khẩu", null);
        }
        if (!user.isActive()){
            return new Response(ResponseStatus.UNAUTHORIZED, "Tài khoản đã bị khóa", null);
        }
        UserResponseDTO userResponseDTO = new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getBalance()
        );
        System.out.println("[UserService] LOGIN: " + user.getUsername() + " | role=" + user.getRole() + " -> SUCCESS");
        return new Response(ResponseStatus.SUCCESS, "Đăng nhập thành công!", userResponseDTO);
    }
    /**
     * Xử lý đăng ký tài khoản mới.
     *
     * Luồng:
     *   1. Validate: null, empty
     *   2. Kiểm tra username chưa tồn tại
     *   3. Tạo User mới với UUID + role MEMBER
     *   4. Lưu vào DAO
     *   5. Trả về UserResponseDTO (KHÔNG chứa password)
     */
    public Response register(RegisterDTO dto) {
        // 1. Validate null
        if (dto == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Thiếu thông tin đăng ký", null);
        }
        // 2. Validate empty
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Username không được để trống", null);
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Password không được để trống", null);
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            return new Response(ResponseStatus.BAD_REQUEST, "Email không được để trống", null);
        }
        // 3. Kiểm tra trùng username
        if (userDAO.existsByUsername(dto.getUsername().trim())) {
            return new Response(ResponseStatus.BAD_REQUEST, "Username đã tồn tại, vui lòng chọn tên khác", null);
        }
        // 4. Tạo User mới
        User newUser = new User(
                UUID.randomUUID().toString(),   // id duy nhất
                dto.getUsername().trim(),
                dto.getPassword(),
                dto.getEmail().trim(),
                dto.getFullName()  != null ? dto.getFullName().trim()  : "",
                dto.getPhone()     != null ? dto.getPhone()            : "",
                dto.getAddress()   != null ? dto.getAddress()          : "",
                true,               // isActive = true
                UserRole.MEMBER,    // role mặc định
                0.0,                // balance ban đầu = 0
                null,               // shopName (chỉ SELLER mới có)
                0.0                 // rating
        );
        userDAO.save(newUser);

        // 5. Trả về DTO (KHÔNG trả password về client)
        UserResponseDTO responseDTO = new UserResponseDTO(
                newUser.getId(), newUser.getUsername(),
                newUser.getEmail(), newUser.getRole(), newUser.getBalance()
        );
        System.out.println("[UserService] REGISTER: " + newUser.getUsername() + " | id=" + newUser.getId() + " -> SUCCESS");
        return new Response(ResponseStatus.SUCCESS, "Đăng ký thành công!", responseDTO);
    }
}
