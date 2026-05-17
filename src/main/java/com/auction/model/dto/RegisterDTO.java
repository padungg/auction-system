package com.auction.model.dto;

/**
 * Gói tin DTO Client gửi lên khi đăng ký tài khoản mới.
 */
public class RegisterDTO {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String storeName;

    public RegisterDTO() {
    }

    public RegisterDTO(String username, String password, String email, String fullName, String phone, String address) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
    }

    public RegisterDTO(String username, String password, String email, String fullName, String phone, String address, String storeName) {
        this(username, password, email, fullName, phone, address);
        this.storeName = storeName;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}

