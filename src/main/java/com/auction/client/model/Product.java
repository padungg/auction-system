package com.auction.client.model;

public class Product {
    private int id;
    private String name;
    private String category;
    private double price;
    private String status;
    private String endTime;

    public Product(int id, String name, String category, double price, String status, String endTime) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.status = status;
        this.endTime = endTime;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getStatus() { return status; }
    public String getEndTime() { return endTime; }

    public void setName(String name) { this.name = name; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}