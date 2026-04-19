package com.auction.server.service;

import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.entity.User;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.dao.UserDAO;

/**
 * Service xử lý nghiệp vụ liên quan đến User.
 *
 * Chức năng:
 *   - Login: xác thực tài khoản + mật khẩu
 *   - Register: đăng ký tài khoản mới với kiểm tra trùng username (chưa làm)
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
        System.out.println(" UserService: Đăng nhập thành công: " + user.getUsername());
        return new Response(ResponseStatus.SUCCESS, "Đăng nhập thành công!", userResponseDTO);
    }
    /**
     * Xử lý đăng ký tài khoản mới.
     */
}
