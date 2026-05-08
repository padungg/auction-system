package com.auction.model.entity;

import java.time.LocalDateTime;

public class Auction extends Entity{
    private String itemId, currentWinnerId;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;


    public Auction() {
    }

    public Auction(String id, String itemId, double currentPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.itemId = itemId;
        this.currentWinnerId = null;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
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

    /**
     * Áp dụng quy tắc Anti-Sniping: Nếu thời gian còn lại <= 60 giây, gia hạn thêm 120 giây.
     * @return true nếu đã gia hạn, false nếu không
     */
    public boolean applyAntiSniping() {
        long secondsLeft = java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
        if (secondsLeft > 0 && secondsLeft <= 60) {
            this.endTime = this.endTime.plusSeconds(120);
            return true;
        }
        return false;
    }
}
