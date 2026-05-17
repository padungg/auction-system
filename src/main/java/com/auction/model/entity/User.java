package com.auction.model.entity;

/**
 * Thực thể đại diện cho người dùng hệ thống.
 * Chứa logic nạp/rút tiền (deposit/withdraw) thay vì sử dụng setter thô
 * để đảm bảo an toàn dữ liệu và tuân thủ tính đóng gói.
 */
public class User extends Entity {

    private String username;
    private String password;
    private String email;
    private String fullName;
    private String phone;
    private String address;
    private boolean isActive;
    private UserRole role;
    private double balance;
    private String storeName;
    private double rating;

    public User() {
    }

    public User(String id, String username, String password, String email, String fullName, String phone,
                String address, boolean isActive, UserRole role, double balance, String storeName, double rating) {
        super(id);
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.isActive = isActive;
        this.role = role;
        this.balance = balance;
        this.storeName = storeName;
        this.rating = rating;
    }

    public String getUsername() {
        return username;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
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
        if (balance < 0)
            throw new IllegalArgumentException("Số dư không thể âm: " + balance);
        this.balance = balance;
    }

    public void deposit(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Số tiền nạp phải > 0");
        this.balance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Số tiền rút phải > 0");
        if (amount > this.balance)
            throw new IllegalStateException("Số dư không đủ: có " + this.balance + ", cần " + amount);
        this.balance -= amount;
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
}
