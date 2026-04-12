package com.auction.model.entity;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;
    private double balance, rating;

    public Bidder(String id, String username, String password, String email, String fullName, String phone,
            String address, double balance, double rating) {
        super(id, username, password, email, fullName, phone, address, UserRole.BIDDER);
        this.balance = balance;
        this.rating = rating;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }
}
