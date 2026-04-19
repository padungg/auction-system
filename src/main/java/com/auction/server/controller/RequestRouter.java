package com.auction.server.controller;

import com.auction.model.protocol.*;

/**
 * Bộ điều hướng Request — "Tổng đài viên" của Server.
 *
 * Nhận Request từ ClientHandler → dựa vào RequestType → gọi đúng Controller.
 * Router KHÔNG chứa logic gì — chỉ switch/case và gọi Controller.
 *
 * Lưu ý: SUBSCRIBE_AUCTION và UNSUBSCRIBE_AUCTION được xử lý trực tiếp
 * trong ClientHandler (vì cần truy cập AuctionManager + this), không qua Router.
 *
 * Luồng: ClientHandler → RequestRouter → Controller → Service → DAO
 */
public class RequestRouter {

    private final UserController userController;
    private final AuctionController auctionController;
    private final BidController bidController;

    public RequestRouter(UserController userController, AuctionController auctionController, BidController bidController) {
        this.userController = userController;
        this.auctionController = auctionController;
        this.bidController = bidController;
    }

    /**
     * Điều hướng request đến Controller phù hợp.
     *
     * @param request       Request từ Client (chứa type + payload)
     * @param currentUserId ID của user đang đăng nhập (null nếu chưa login)
     * @return Response để gửi lại cho Client
     */
    public Response route(Request request, String currentUserId) {
        if (request == null || request.getType() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Request không hợp lệ", null);
        }

        try {
            switch (request.getType()) {

                // ==================== USER ====================
                case LOGIN:
                    return userController.handleLogin(request.getPayload());

                case REGISTER:
                    return userController.handleRegister(request.getPayload());

                // ==================== AUCTION ====================
                case GET_ALL_AUCTIONS:
                    return auctionController.handleGetAllAuctions();

                case GET_AUCTION_DETAIL:
                    return auctionController.handleGetAuctionDetail(request.getPayload());

                case CREATE_AUCTION:
                    return auctionController.handleCreateAuction(request.getPayload(), currentUserId);

                case CLOSE_AUCTION:
                    return auctionController.handleCloseAuction(request.getPayload(), currentUserId);

                // ==================== BID ====================
                case PLACE_BID:
                    return bidController.handlePlaceBid(request.getPayload(), currentUserId);

                case GET_BID_HISTORY:
                    return bidController.handleGetBidHistory(request.getPayload());

                // ==================== MẶC ĐỊNH ====================
                default:
                    return new Response(ResponseStatus.BAD_REQUEST,
                            "Loại request không được hỗ trợ: " + request.getType(), null);
            }
        } catch (Exception e) {
            System.out.println(">>> [LỖI] RequestRouter: " + e.getMessage());
            return new Response(ResponseStatus.ERROR, "Lỗi server nội bộ: " + e.getMessage(), null);
        }
    }
}