package com.auction.model.dto;

/**
 * Gói tin DTO do Client gởi lên khi bấm nút "Đặt Giá".
 */
public class BidRequestDTO {

    private String auctionId;
    private double bidAmount;
    private String bidType;      // "NORMAL" hoặc "AUTO" (Strategy Pattern)
    private double maxBidAmount; // Dùng cho Auto-Bid: giá tối đa user chấp nhận

    public BidRequestDTO() {
    }

    public BidRequestDTO(String auctionId, double bidAmount) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidType = "NORMAL";
    }

    public BidRequestDTO(String auctionId, double bidAmount, String bidType, double maxBidAmount) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidType = bidType;
        this.maxBidAmount = maxBidAmount;
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

    public String getBidType() {
        return bidType;
    }

    public void setBidType(String bidType) {
        this.bidType = bidType;
    }

    public double getMaxBidAmount() {
        return maxBidAmount;
    }

    public void setMaxBidAmount(double maxBidAmount) {
        this.maxBidAmount = maxBidAmount;
    }
}

