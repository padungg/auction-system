package com.auction.model.entity;

import java.time.LocalDateTime;

public class BidTransaction extends Entity{
    private String bidderId, auctionId;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction(String id, String bidderId, String auctionId, double bidAmount, LocalDateTime bidTime) {
        super(id);
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
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
        return "Id: " + this.getId() + " User: " + bidderId + " AuctionId: " + auctionId + " Amount: " + bidAmount + " Thời gian: " + bidTime;
    }
}
