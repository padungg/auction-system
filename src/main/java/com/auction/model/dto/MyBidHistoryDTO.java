package com.auction.model.dto;
import java.time.LocalDateTime;

/**
 * Gói tin DTO Server trả về cho Client khi xem lịch sử đấu giá của tôi.
 */
public class MyBidHistoryDTO {
    private String auctionId;
    private String itemName;
    private double bidAmount;
    private LocalDateTime bidTime;
    private String status;

    public MyBidHistoryDTO() {
    }

    public MyBidHistoryDTO(String auctionId, String itemName, double bidAmount, LocalDateTime bidTime, String status) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.status = status;
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

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}