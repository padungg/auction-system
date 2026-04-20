package com.auction.server.observer;
/**
 * OBSERVER PATTERN — Interface cho những ai muốn nhận thông báo khi có bid mới.
 * ClientHandler implements interface này
 *   - ClientHandler — vì nó nắm giữ đường truyền Socket về client
 *   - Khi onBidUpdated() được gọi → ClientHandler gửi JSON thông báo về client
 */
public interface AuctionObserver {
    /**
     * Được gọi tự động khi có bid mới trên phiên đấu giá mà observer đang theo dõi.
     */
    void onBidUpdated(String auctionId, double newPrice, String bidderId);
}
