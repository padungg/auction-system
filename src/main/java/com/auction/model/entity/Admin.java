package com.auction.model.entity;

public class Admin extends User{
    private static final long serialVersionUID = 1L;

    public Admin(String id, String username, String password, String email, String fullName, String phone, String address) {
        super(id, username, password, email, fullName, phone, address, UserRole.ADMIN);
    }
}
