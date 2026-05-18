package com.auction.model.dto;

import java.time.LocalDateTime;

public class MyBidHistoryDTO {
    private String auctionId;
    private String itemName;
    private double bidAmount;
    private LocalDateTime bidTime;
    private String result;

    public MyBidHistoryDTO() {
    }

    public MyBidHistoryDTO(String auctionId, String itemName, double bidAmount, LocalDateTime bidTime, String result) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.result = result;
    }

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public double getBidAmount() { return bidAmount; }
    public void setBidAmount(double bidAmount) { this.bidAmount = bidAmount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
