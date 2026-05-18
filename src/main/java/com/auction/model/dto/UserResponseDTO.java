package com.auction.model.dto;

import com.auction.model.entity.UserRole;

/**
 * Gói tin DTO Server trả về cho Client sau khi xác thực thành công.
 * Chứa đầy đủ thông tin user (KHÔNG chứa password).
 */
public class UserResponseDTO {

    private String id;
    private String username;
    private String email;
    private UserRole role;
    private double balance;

    // ── Các trường bổ sung cho AccountController và AdminController ──
    private String fullName;
    private String phone;
    private String address;
    private String storeName;
    private double rating;
    private boolean isActive;

    public UserResponseDTO() {
    }

    /** Constructor gốc (tương thích ngược) */
    public UserResponseDTO(String id, String username, String email, UserRole role, double balance) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.balance = balance;
    }

    /** Constructor đầy đủ */
    public UserResponseDTO(String id, String username, String email, UserRole role, double balance,
                           String fullName, String phone, String address,
                           String storeName, double rating, boolean isActive) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.balance = balance;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.storeName = storeName;
        this.rating = rating;
        this.isActive = isActive;
    }

    // ── Getters & Setters ──

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

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
