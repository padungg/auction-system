package com.auction.model.entity;

public enum AuctionStatus {
    OPEN,      // Chờ đấu giá
    RUNNING,   // Đang diễn ra
    FINISHED,  // Đã kết thúc
    PAID,      // Đã thanh toán
    CANCELED   // Bị hủy
}
