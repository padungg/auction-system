package com.auction.model.dto;

import java.time.LocalDateTime;

/**
 * Gói tin DTO Sever trả về khi Client xem chi tiết một phiên đấu giá.
 * Chứa đầy đủ mọi thông tin để vẽ giao diện rõ ràng.
 */
public class AuctionDetailDTO {

    private String auctionId;
    private String itemName;
    private String itemDetails;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private String sellerName;
    private String currentWinnerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public AuctionDetailDTO() {
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDetails() {
        return itemDetails;
    }

    public void setItemDetails(String itemDetails) {
        this.itemDetails = itemDetails;
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

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getCurrentWinnerName() {
        return currentWinnerName;
    }

    public void setCurrentWinnerName(String currentWinnerName) {
        this.currentWinnerName = currentWinnerName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
