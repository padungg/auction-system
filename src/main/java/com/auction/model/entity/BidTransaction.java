package com.auction.model.entity;

import java.time.LocalDateTime;

public class BidTransaction extends Entity{
    private String bidderId, itemId;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction(String id, String bidderId, String itemId, double bidAmount) {
        super(id);
        this.bidderId = bidderId;
        this.itemId = itemId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
    public String getInfo(){
        return "Id: " + this.getId() + " User: " + bidderId + " Item: " + itemId + " Amount: " + bidAmount + " Thời gian: " + bidTime;
    }
}
