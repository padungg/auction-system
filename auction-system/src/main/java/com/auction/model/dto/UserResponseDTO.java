package com.auction.model.dto;

import com.auction.model.entity.UserRole;

/**
 * Gói tin DTO Server trả về cho Client sau khi xác thực thành công.
 */
public class UserResponseDTO {

    private String id;
    private String username;
    private String email;
    private UserRole role;
    private double balance;

    public UserResponseDTO() {
    }

    public UserResponseDTO(String id, String username, String email, UserRole role, double balance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.balance = balance;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
