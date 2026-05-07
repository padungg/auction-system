package com.auction.model.protocol;

public enum RequestType {
    LOGIN,                  // Đăng nhập
    REGISTER,               // Tạo tài khoản
    PLACE_BID,              // Đặt giá thủ công
    GET_ALL_AUCTIONS,       // Lấy danh sách tất cả phiên đấu giá
    GET_AUCTION_DETAIL,     // Xem chi tiết phiên đấu giá
    CREATE_AUCTION,         // Tạo phiên
    GET_BID_HISTORY,        // Lấy lịch sử bid của 1 phiên
    SUBSCRIBE_AUCTION,      // Đăng ký nhận realtime update giá
    UNSUBSCRIBE_AUCTION,    // Hủy đăng ký realtime
    CLOSE_AUCTION,          // Đóng phiên thủ công
    REGISTER_AUTO_BID,      // Đăng ký đặt giá tự động (maxBid + increment)
    CANCEL_AUTO_BID         // Hủy đăng ký auto-bid
}
