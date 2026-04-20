package com.auction.server.strategy;

import com.auction.model.entity.Auction;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;

/**
 * Chiến lược BID THƯỜNG — giá phải cao hơn giá hiện tại, đơn giản nhất.
 *
 * Luật:
 *   1. Giá bid phải > giá hiện tại
 *   2. Không được tự bid khi đang thắng
 *   3. Giá gửi lên bao nhiêu → ghi bấy nhiêu (không tự tính)
 */
public class NormalBidStrategy implements BidStrategy {

    @Override
    public Response validate(Auction auction, double bidAmount, String bidderId) {
        // Giá phải cao hơn giá hiện tại
        if (bidAmount <= auction.getCurrentPrice()) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Giá phải cao hơn giá hiện tại ("
                            + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ)",
                    null);
        }

        // Không tự bid chính mình
        if (bidderId.equals(auction.getCurrentWinnerId())) {
            return new Response(ResponseStatus.BAD_REQUEST,
                    "Bạn đang là người đặt giá cao nhất, không cần bid thêm!",
                    null);
        }

        return null; // null = hợp lệ
    }

    @Override
    public double calculateActualBid(Auction auction, double bidAmount) {
        return bidAmount; // Bid thường: giữ nguyên giá gửi lên
    }
}
