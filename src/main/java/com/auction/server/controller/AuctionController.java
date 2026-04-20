package com.auction.server.controller;

import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.network.GsonConfig;
import com.auction.server.service.AuctionService;
import com.google.gson.Gson;

/**
 * Controller xử lý các request liên quan đến Phiên Đấu Giá.
 *
 * Vai trò: Nhận payload thô từ Router → convert sang DTO → gọi Service → trả Response.
 */
public class AuctionController {

    private final AuctionService auctionService;
    private final Gson gson;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
        this.gson = GsonConfig.createGson();
    }

    /**
     * Xử lý request GET_ALL_AUCTIONS.
     * Không cần payload — chỉ lấy danh sách toàn bộ phiên đang mở.
     */
    public Response handleGetAllAuctions() {
        return auctionService.getAllAuctions();
    }

    /**
     * Xử lý request GET_AUCTION_DETAIL.
     * Payload chỉ là auctionId (String).
     */
    public Response handleGetAuctionDetail(Object payload) {
        String json = gson.toJson(payload);
        String auctionId = gson.fromJson(json, String.class);
        return auctionService.getAuctionDetail(auctionId);
    }

    /**
     * Xử lý request CREATE_AUCTION.
     * Cần kiểm tra đã login chưa (currentUserId != null).
     */
    public Response handleCreateAuction(Object payload, String currentUserId) {
        if (currentUserId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        String json = gson.toJson(payload);
        CreateAuctionDTO dto = gson.fromJson(json, CreateAuctionDTO.class);
        return auctionService.createAuction(dto, currentUserId);
    }

    /**
     * Xử lý request CLOSE_AUCTION.
     * Chỉ Admin hoặc Seller của phiên đó mới được đóng.
     */
    public Response handleCloseAuction(Object payload, String currentUserId) {
        if (currentUserId == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Bạn chưa đăng nhập", null);
        }
        String json = gson.toJson(payload);
        String auctionId = gson.fromJson(json, String.class);
        return auctionService.closeAuction(auctionId, currentUserId);
    }
}
