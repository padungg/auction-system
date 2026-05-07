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
     *
     * @param bidTime ISO-8601 timestamp của bid — dùng để client vẽ realtime price curve (3.2.5)
     */
    void onBidUpdated(String auctionId, double newPrice, String bidderId, String bidTime);

    /**
     * Được gọi khi phiên đấu giá kết thúc (hết hạn hoặc người bán đóng sớm).
     * ClientHandler nhận → gửi thông báo "AUCTION_CLOSED" về client → client khóa form bid.
     *
     * @param winnerId null nếu không có ai đặt giá
     */
    void onAuctionClosed(String auctionId, double finalPrice, String winnerId);
}
