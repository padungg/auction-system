package com.auction.server.observer;

/**
 * OBSERVER PATTERN — Interface cho những ai muốn nhận thông báo khi có bid mới.
 */
public interface AuctionObserver {
    void onBidUpdated(String auctionId, double newPrice, String bidderId, String bidderName, String itemName,
            String bidTime, String newEndTime);

    // Được gọi khi phiên đấu giá kết thúc (hết hạn hoặc người bán đóng sớm).

    void onAuctionClosed(String auctionId, double finalPrice, String winnerId);
}
