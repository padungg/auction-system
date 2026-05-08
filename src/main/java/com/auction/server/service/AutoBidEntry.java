package com.auction.server.service;

import java.time.LocalDateTime;

/**
 * Đại diện cho 1 lượt đăng ký Auto-Bid của 1 người dùng trong 1 phiên.
 *
 * Implements Comparable → PriorityQueue tự động sắp xếp theo thời điểm đăng ký.
 * Người đăng ký TRƯỚC = ưu tiên CAO hơn (FCFS — First Come First Served).
 *
 * Ví dụ: A đăng ký lúc 10:00, B đăng ký lúc 10:05
 *   → Khi cả 2 đều có thể phản giá, A được xét trước.
 */
public class AutoBidEntry implements Comparable<AutoBidEntry> {

    private final String        userId;
    private final String        auctionId;
    private final double        maxBid;       // giá tối đa sẵn sàng trả
    private final double        increment;    // bước tăng mỗi lần tự động phản giá
    private final LocalDateTime registeredAt; // thời điểm đăng ký — dùng để ưu tiên FCFS

    public AutoBidEntry(String userId, String auctionId, double maxBid, double increment) {
        this.userId       = userId;
        this.auctionId    = auctionId;
        this.maxBid       = maxBid;
        this.increment    = increment;
        this.registeredAt = LocalDateTime.now();
    }

    public AutoBidEntry(String userId, String auctionId, double maxBid, double increment, LocalDateTime registeredAt) {
        this.userId       = userId;
        this.auctionId    = auctionId;
        this.maxBid       = maxBid;
        this.increment    = increment;
        this.registeredAt = registeredAt;
    }

    /**
     * So sánh theo thời điểm đăng ký — đăng ký sớm hơn = ưu tiên cao hơn trong PriorityQueue.
     */
    @Override
    public int compareTo(AutoBidEntry other) {
        return this.registeredAt.compareTo(other.registeredAt);
    }

    public String        getUserId()      { return userId; }
    public String        getAuctionId()   { return auctionId; }
    public double        getMaxBid()      { return maxBid; }
    public double        getIncrement()   { return increment; }
    public LocalDateTime getRegisteredAt(){ return registeredAt; }
}
