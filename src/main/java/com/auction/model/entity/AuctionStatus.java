package com.auction.model.entity;

public enum AuctionStatus {
    OPEN,      // Chờ đấu giá (tương đương PENDING)
    RUNNING,   // Đang diễn ra (tương đương OPENING)
    FINISHED,  // Đã kết thúc (tương đương CLOSED)
    PAID,      // Đã thanh toán
    CANCELED   // Bị hủy (tương đương CANCELLED)
}
