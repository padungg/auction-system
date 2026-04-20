package com.auction.server.observer;

/**
 * OBSERVER PATTERN — Interface cho những ai muốn nhận thông báo khi có bid mới.
 *
 * Tại sao cần Observer?
 *   - Khi User A bid thành công → User B đang xem cùng phiên phải tự refresh → trải nghiệm tệ
 *   - Với Observer: User A bid → Server TỰ ĐỘNG đẩy giá mới cho tất cả User đang xem
 *
 * Ai implement interface này?
 *   - ClientHandler — vì nó nắm giữ đường truyền Socket về client
 *   - Khi onBidUpdated() được gọi → ClientHandler gửi JSON thông báo về client
 */
public interface AuctionObserver {

    /**
     * Được gọi tự động khi có bid mới trên phiên đấu giá mà observer đang theo dõi.
     *
     * @param auctionId Mã phiên đấu giá có bid mới
     * @param newPrice  Giá mới sau khi bid
     * @param bidderId  ID người vừa bid thành công
     */
    void onBidUpdated(String auctionId, double newPrice, String bidderId);
}
