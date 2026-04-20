package com.auction.model.protocol;

public enum RequestType {
    LOGIN,
    REGISTER,
    PLACE_BID,
    GET_ALL_AUCTIONS,
    GET_AUCTION_DETAIL,
    CREATE_AUCTION,
    // === MỚI: Phase 4-5 ===
    GET_BID_HISTORY,        // Lấy lịch sử bid của 1 phiên
    SUBSCRIBE_AUCTION,      // Đăng ký nhận realtime update giá
    UNSUBSCRIBE_AUCTION,    // Hủy đăng ký
    CLOSE_AUCTION           // Đóng phiên (Admin/Seller)
}
