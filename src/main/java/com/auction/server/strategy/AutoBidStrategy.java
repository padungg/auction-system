package com.auction.server.strategy;

import com.auction.model.entity.Auction;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;

/**
 * Chiến lược AUTO-BID — User đặt giá tối đa, hệ thống tự tăng từng bước.
 *
 * Ví dụ:
 *   - Giá hiện tại: 100,000 VNĐ
 *   - User đặt maxBid = 500,000 VNĐ
 *   - Hệ thống bid: 110,000 VNĐ (tăng 10K mỗi bước)
 *   - Lần sau bị vượt → hệ thống tự bid tiếp cho đến khi đạt max
 *
 * Luật:
 *   1. Giá tối đa phải > giá hiện tại
 *   2. Giá thực tế = giá hiện tại + INCREMENT
 *   3. Nếu giá thực tế > max → dùng max
 */
public class AutoBidStrategy implements BidStrategy {

    // Mỗi bước tự động tăng 10,000 VNĐ
    private static final double BID_INCREMENT = 10000;

    @Override
    public Response validate(Auction auction, double maxBidAmount, String bidderId) {
        // Giá tối đa phải cao hơn giá hiện tại
        if (maxBidAmount <= auction.getCurrentPrice()) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Giá tối đa phải cao hơn giá hiện tại ("
                            + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ)",
                    null);
        }

        return null; // hợp lệ
    }

    @Override
    public double calculateActualBid(Auction auction, double maxBidAmount) {
        // Tính giá bid thực tế = giá hiện tại + bước nhảy
        double actualBid = auction.getCurrentPrice() + BID_INCREMENT;

        // Không vượt quá giá tối đa user đặt
        if (actualBid > maxBidAmount) {
            actualBid = maxBidAmount;
        }

        return actualBid;
    }
}
