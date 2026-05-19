package com.auction.model.entity;

import java.time.LocalDateTime;

/**
 * Đại diện cho 1 lượt đăng ký Auto-Bid của 1 người dùng trong 1 phiên.
 */
public class AutoBidEntry implements Comparable<AutoBidEntry> {

    private final String userId;
    private final String auctionId;
    private final double maxBid;
    private final double increment;
    private final LocalDateTime registeredAt;

    public AutoBidEntry(String userId, String auctionId, double maxBid, double increment) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = LocalDateTime.now();
    }

    public AutoBidEntry(String userId, String auctionId, double maxBid, double increment, LocalDateTime registeredAt) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.increment = increment;
        this.registeredAt = registeredAt;
    }

    /**
     * So sánh theo thời điểm đăng ký — đăng ký sớm hơn = ưu tiên cao hơn trong PriorityQueue.
     */
    @Override
    public int compareTo(AutoBidEntry other) {
        return this.registeredAt.compareTo(other.registeredAt);
    }

    public String getUserId() {
        return userId;
    }
    public String getAuctionId() {
        return auctionId;
    }
    public double getMaxBid() {
        return maxBid;
    }
    public double getIncrement() {
        return increment;
    }
    public LocalDateTime getRegisteredAt(){
        return registeredAt;
    }
}
