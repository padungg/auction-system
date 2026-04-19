package com.auction.model.dto;

/**
 * Gói tin DTO dùng khi Client muốn Đăng Nhập.
 * Chứa thông tin nhạy cảm (mật khẩu) nên chỉ đi một chiều từ Client -> Server.
 */
public class LoginDTO {

    private String username;
    private String password;

    public LoginDTO() {
    }

    public LoginDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
