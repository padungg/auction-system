package com.auction.model.dto;

/**
 * Gói tin DTO dạng RÚT GỌN của một phiên đấu giá.
 * Được dùng khi Client muốn load dạnh sách hàng tá phiên đấu giá ở trang chủ.
 */
public class AuctionSummaryDTO {

    private String auctionId;
    private String itemName;
    private double currentPrice;
    private String status;

    public AuctionSummaryDTO() {
    }

    public AuctionSummaryDTO(String auctionId, String itemName, double currentPrice, String status) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
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

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
