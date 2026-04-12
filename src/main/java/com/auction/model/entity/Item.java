package com.auction.model.entity;

public abstract class Item extends Entity{
    private static final long serialVersionUID = 1L;
    private String name, description, condition, sellerId;
    private double startingPirce;

    public Item(String id, String name, String description, String condition, String sellerId, double startingPirce) {
        super(id);
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.sellerId = sellerId;
        this.startingPirce = startingPirce;
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

    public double getStartingPirce() {
        return startingPirce;
    }

    public void setStartingPirce(double startingPirce) {
        this.startingPirce = startingPirce;
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
