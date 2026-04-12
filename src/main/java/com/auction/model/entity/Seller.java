package com.auction.model.entity;

public class Seller extends User{
    private static final long serialVersionUID = 1L;
    private String storeName;
    private double rating;

    public Seller(String id, String username, String password, String email, String fullName, String phone, String address, String storeName, double rating) {
        super(id, username, password, email, fullName, phone, address, UserRole.SELLER);
        this.storeName = storeName;
        this.rating = rating;
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
