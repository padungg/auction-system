package com.auction.server.controller;

import com.auction.model.dto.BidRequestDTO;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.network.GsonConfig;
import com.auction.server.service.BidService;
import com.google.gson.Gson;

/**
 * Controller xử lý các request liên quan đến Đặt Giá (Bid).
 *
 * Vai trò: Nhận payload thô từ Router → convert sang DTO → gọi Service → trả Response.
 */
public class BidController {

    private final BidService bidService;
    private final Gson gson;

    public BidController(BidService bidService) {
        this.bidService = bidService;
        this.gson = GsonConfig.createGson();
    }

    /**
     * Xử lý request PLACE_BID.
     * Cần kiểm tra đã login chưa (currentUserId != null).
     */
    public Response handlePlaceBid(Object payload, String currentUserId) {
        if (currentUserId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        String json = gson.toJson(payload);
        BidRequestDTO dto = gson.fromJson(json, BidRequestDTO.class);
        return bidService.placeBid(dto, currentUserId);
    }

    /**
     * Xử lý request GET_BID_HISTORY.
     * Payload chỉ là auctionId (String).
     */
    public Response handleGetBidHistory(Object payload) {
        String json = gson.toJson(payload);
        String auctionId = gson.fromJson(json, String.class);
        return bidService.getBidHistory(auctionId);
    }
}
