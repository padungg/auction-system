package com.auction.model.entity;

public abstract class Item extends Entity {
    private String name, description, condition, sellerId;
    private double startingPrice;

    public Item() {
    }

    public Item(String id, String name, String description, String condition, String sellerId, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
    }

    public abstract String getDetailInfo();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
}
