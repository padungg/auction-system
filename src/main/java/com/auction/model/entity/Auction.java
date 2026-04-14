package com.auction.model.entity;

import java.time.LocalDateTime;

public class Auction extends Entity{
    private String itemId, currentWinnerId;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public Auction(String id, String itemId, String currentWinnerId, double currentPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.itemId = itemId;
        this.currentWinnerId = null;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.PENDING;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getCurrentWinnerId() {
        return currentWinnerId;
    }

    public void setCurrentWinnerId(String currentWinnerId) {
        this.currentWinnerId = currentWinnerId;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}
