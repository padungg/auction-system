package com.auction.model.protocol;

public enum RequestType {
    // AUTHENTICATION
    LOGIN,                  // Đăng nhập
    REGISTER,               // Tạo tài khoản

    // AUCTION CORE
    PLACE_BID,              // Đặt giá thủ công
    GET_ALL_AUCTIONS,       // Lấy danh sách tất cả phiên đấu giá
    GET_AUCTION_DETAIL,     // Xem chi tiết phiên đấu giá
    CREATE_AUCTION,         // Tạo phiên
    GET_BID_HISTORY,        // Lấy lịch sử bid của 1 phiên
    SUBSCRIBE_AUCTION,      // Đăng ký nhận realtime update giá
    UNSUBSCRIBE_AUCTION,    // Hủy đăng ký realtime
    CLOSE_AUCTION,          // Đóng phiên thủ công
    REGISTER_AUTO_BID,      // Đăng ký đặt giá tự động (maxBid + increment)
    CANCEL_AUTO_BID,        // Hủy đăng ký auto-bid

    // ADMIN
    GET_ALL_USERS,          // Lấy danh sách tất cả user
    LOCK_USER,              // Khóa tài khoản user
    UNLOCK_USER,            // Mở khóa tài khoản user
    ADMIN_CANCEL_AUCTION,   // Hủy phiên đấu giá

    // ACCOUNT
    GET_MY_PROFILE,         // Lấy thông tin tài khoản hiện tại (refresh từ DB)
    GET_MY_BID_HISTORY,     // Lấy lịch sử bid của user đang đăng nhập
    DEPOSIT,                // Nạp tiền vào tài khoản
    WITHDRAW,               // Rút tiền khỏi tài khoản
    UPDATE_PROFILE,         // Cập nhật hồ sơ cá nhân

    // PAYMENT
    GET_PENDING_PAYMENTS,   // Lấy danh sách phiên chờ thanh toán
    GET_PAYMENT_HISTORY,    // Lấy lịch sử thanh toán
    PAY_AUCTION,            // Thanh toán phiên đấu giá đã thắng

    // SELLER
    DELETE_AUCTION,         // Xóa phiên đấu giá
    UPDATE_AUCTION          // Cập nhật thông tin phiên
}
