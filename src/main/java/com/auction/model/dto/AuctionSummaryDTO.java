package com.auction.model.dto;

/**
 * Gói tin DTO dạng RÚT GỌN của một phiên đấu giá.
 * Được dùng khi Client muốn load danh sách hàng tá phiên đấu giá ở trang chủ.
 */
public class AuctionSummaryDTO {

    private String auctionId;
    private String itemName;
    private double currentPrice;
    private String status;

    // ── Các trường bổ sung cho ProductCard và Filter ──
    private double startingPrice;
    private String sellerName;
    private String sellerId;
    private String itemType;
    private int bidCount;
    private String endTime;
    private String currentWinnerId;

    public AuctionSummaryDTO() {
    }

    /** Constructor gốc (tương thích ngược) */
    public AuctionSummaryDTO(String auctionId, String itemName, double currentPrice, String status) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
    }

    /** Constructor đầy đủ */
    public AuctionSummaryDTO(String auctionId, String itemName, double currentPrice, String status,
                             double startingPrice, String sellerName, String sellerId,
                             String itemType, int bidCount, String endTime, String currentWinnerId) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.currentPrice = currentPrice;
        this.status = status;
        this.startingPrice = startingPrice;
        this.sellerName = sellerName;
        this.sellerId = sellerId;
        this.itemType = itemType;
        this.bidCount = bidCount;
        this.endTime = endTime;
        this.currentWinnerId = currentWinnerId;
    }

    // ── Getters & Setters ──

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

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public int getBidCount() {
        return bidCount;
    }

    public void setBidCount(int bidCount) {
        this.bidCount = bidCount;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getCurrentWinnerId() {
        return currentWinnerId;
    }

    public void setCurrentWinnerId(String currentWinnerId) {
        this.currentWinnerId = currentWinnerId;
    }
}
