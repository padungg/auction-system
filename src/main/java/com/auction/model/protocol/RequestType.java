package com.auction.model.protocol;

public enum RequestType {
    LOGIN,                  // Đăng nhập
    REGISTER,               // Tạo tài khoản
    PLACE_BID,              // Đặt giá
    GET_ALL_AUCTIONS,       // Lấy danh sách tất cả phiên đấu giá
    GET_AUCTION_DETAIL,     // Xem chi tiết phiên đấu giá
    CREATE_AUCTION,         // Tạo phiên
    GET_BID_HISTORY,        // Lấy lịch sử bid của 1 phiên
    SUBSCRIBE_AUCTION,      // Đăng ký nhận realtime update giá
    UNSUBSCRIBE_AUCTION,    // Hủy đăng ký
    CLOSE_AUCTION           // Đóng phiên
}
