package com.auction.model.protocol;

/**
 * Định nghĩa các hành động (hành vi) mà Client có thể gửi yêu cầu lên Server.
 */
public enum RequestType {
    LOGIN,              // Yêu cầu đăng nhập vào hệ thống
    REGISTER,           // Yêu cầu đăng ký tài khoản mới
    PLACE_BID,          // Gửi một yêu cầu đặt giá thầu cho một phiên đấu giá
    GET_ALL_AUCTIONS,   // Lấy danh sách toàn bộ các phiên đấu giá (dùng cho mành hình chính)
    GET_AUCTION_DETAIL  // Lấy chi tiết thông tin 1 phiên đấu giá (kèm lịch sử Bid)
}
