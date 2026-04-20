package com.auction.server.strategy;

import com.auction.model.entity.Auction;
import com.auction.model.protocol.Response;

/**
 * STRATEGY PATTERN — Interface cho các chiến lược đặt giá khác nhau.
 *
 * Tại sao cần Strategy?
 *   - Hiện tại chỉ có 1 kiểu bid (bid thường)
 *   - Nếu thêm Auto-Bid, Snipe-Bid → nhồi hết vào BidService → 500 dòng if/else
 *   - Strategy tách mỗi kiểu bid ra 1 class riêng → dễ thêm/sửa/xóa
 *
 * Cách dùng:
 *   BidStrategy strategy = strategies.get("NORMAL");
 *   Response error = strategy.validate(auction, bidAmount, bidderId);
 *   double actualBid = strategy.calculateActualBid(auction, bidAmount);
 */
public interface BidStrategy {

    /**
     * Kiểm tra bid có hợp lệ không theo chiến lược này.
     *
     * @param auction   Phiên đấu giá hiện tại
     * @param bidAmount Số tiền đặt
     * @param bidderId  ID người đặt
     * @return null nếu hợp lệ, Response lỗi nếu không hợp lệ
     */
    Response validate(Auction auction, double bidAmount, String bidderId);

    /**
     * Tính giá bid thực tế.
     * - Bid thường: giá giữ nguyên
     * - Auto-Bid: giá = giá hiện tại + bước nhảy (không vượt max)
     *
     * @param auction   Phiên đấu giá
     * @param bidAmount Số tiền gửi lên (hoặc maxBid cho Auto-Bid)
     * @return Giá bid thực tế sẽ được ghi vào auction
     */
    double calculateActualBid(Auction auction, double bidAmount);
}
