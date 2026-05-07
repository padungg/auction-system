package com.auction.server.controller;

import com.auction.model.dto.AutoBidDTO;
import com.auction.model.dto.BidRequestDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.BidService;
import com.auction.server.service.UserService;
import com.google.gson.Gson;

/**
 * CONTROLLER — Router trung tâm của server.
 *
 * Nhận Request từ ClientHandler → deserialize payload → gọi đúng Service → trả Response.
 *
 * Tại sao tách riêng khỏi ClientHandler?
 *   - ClientHandler lo việc I/O (đọc/ghi socket).
 *   - RequestController lo việc điều hướng logic (router).
 *   - Tách biệt → dễ viết Unit Test cho từng Service độc lập.
 *
 * Thread-safety:
 *   - Mỗi ClientHandler giữ 1 instance RequestController riêng → không share state.
 *   - Các Service bên trong là stateless (chỉ chứa DAO) → an toàn khi gọi từ nhiều thread.
 */
public class RequestController {

    private static final Gson GSON = GsonProvider.getInstance();

    private final UserService    userService;
    private final AuctionService auctionService;
    private final BidService     bidService;
    private final AutoBidService autoBidService;

    public RequestController(UserService userService,
                             AuctionService auctionService,
                             BidService bidService,
                             AutoBidService autoBidService) {
        this.userService    = userService;
        this.auctionService = auctionService;
        this.bidService     = bidService;
        this.autoBidService = autoBidService;
    }

    // ════════════════════════════════════════════════════════════════
    // ROUTER CHÍNH
    // ════════════════════════════════════════════════════════════════

    /**
     * Điều hướng Request đến đúng Service.
     *
     * @param request  Request đã parse từ JSON
     * @param loggedInUserId  userId đang đăng nhập (null nếu chưa login)
     * @return Response trả về cho client
     */
    public Response handle(Request request, String loggedInUserId) {
        if (request == null || request.getType() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Request không hợp lệ", null);
        }

        try {
            switch (request.getType()) {

                // ── USER ──────────────────────────────────────────────
                case LOGIN:
                    return handleLogin(request);

                case REGISTER:
                    return handleRegister(request);

                // ── AUCTION ───────────────────────────────────────────
                case GET_ALL_AUCTIONS:
                    return auctionService.getAllAuctions();

                case GET_AUCTION_DETAIL:
                    return handleGetAuctionDetail(request);

                case CREATE_AUCTION:
                    return handleCreateAuction(request, loggedInUserId);

                case CLOSE_AUCTION:
                    return handleCloseAuction(request);

                // ── BID ───────────────────────────────────────────────
                case PLACE_BID:
                    return handlePlaceBid(request, loggedInUserId);

                case GET_BID_HISTORY:
                    return handleGetBidHistory(request);

                // ── AUTO-BID ───────────────────────────────────
                case REGISTER_AUTO_BID:
                    return handleRegisterAutoBid(request, loggedInUserId);

                case CANCEL_AUTO_BID:
                    return handleCancelAutoBid(request, loggedInUserId);

                // ── SUBSCRIBE: được xử lý trực tiếp tại ClientHandler ─
                // SUBSCRIBE_AUCTION và UNSUBSCRIBE_AUCTION không qua đây
                // vì cần tham chiếu đến chính ClientHandler (observer).

                default:
                    return new Response(ResponseStatus.BAD_REQUEST,
                            "Loại request không được hỗ trợ: " + request.getType(), null);
            }
        } catch (Exception e) {
            System.err.println("[RequestController] LỖI NGOẠI LỆ nội bộ: " + e.getMessage());
            e.printStackTrace();
            return new Response(ResponseStatus.ERROR, "Lỗi máy chủ (500): " + e.getMessage(), null);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // CÁC HANDLER RIÊNG — mỗi handler phụ trách 1 loại request
    // ════════════════════════════════════════════════════════════════

    private <T> T parsePayload(Request request, Class<T> clazz) {
        return GSON.fromJson(GSON.toJson(request.getPayload()), clazz);
    }

    private Response handleLogin(Request request) {
        LoginDTO dto = parsePayload(request, LoginDTO.class);
        return userService.login(dto);
    }

    private Response handleRegister(Request request) {
        RegisterDTO dto = parsePayload(request, RegisterDTO.class);
        return userService.register(dto);
    }

    private Response handleGetAuctionDetail(Request request) {
        String auctionId = parsePayload(request, String.class);
        return auctionService.getAuctionDetail(auctionId);
    }

    private Response handleCreateAuction(Request request, String sellerId) {
        CreateAuctionDTO dto = parsePayload(request, CreateAuctionDTO.class);
        return auctionService.createAuction(dto, sellerId);
    }

    private Response handleCloseAuction(Request request) {
        String auctionId = parsePayload(request, String.class);
        return auctionService.closeAuction(auctionId);
    }

    private Response handlePlaceBid(Request request, String bidderId) {
        BidRequestDTO dto = parsePayload(request, BidRequestDTO.class);
        return bidService.placeBid(dto, bidderId);
    }

    private Response handleGetBidHistory(Request request) {
        String auctionId = parsePayload(request, String.class);
        return bidService.getBidHistory(auctionId);
    }

    private Response handleRegisterAutoBid(Request request, String userId) {
        AutoBidDTO dto = parsePayload(request, AutoBidDTO.class);
        return autoBidService.register(dto, userId);
    }

    private Response handleCancelAutoBid(Request request, String userId) {
        String auctionId = parsePayload(request, String.class);
        return autoBidService.cancel(auctionId, userId);
    }
}
