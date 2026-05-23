package com.auction.server.controller.handler;

import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.service.AuctionService;
import com.auction.server.service.UserService;

public class AdminHandler extends BaseHandler {
    private final UserService userService;
    private final AuctionService auctionService;

    public AdminHandler(UserService userService, AuctionService auctionService) {
        this.userService = userService;
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, String loggedInUserId) {
        if (loggedInUserId == null || !userService.isAdmin(loggedInUserId)) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Từ chối truy cập: Bạn không có quyền quản trị viên!", null);
        }
        switch (request.getType()) {
            case GET_ALL_USERS:
                return userService.getAllUsers();
            case LOCK_USER:
                return handleLockUser(request);
            case UNLOCK_USER:
                return handleUnlockUser(request);
            case ADMIN_CANCEL_AUCTION:
                return handleAdminCancelAuction(request);
            case ADMIN_MARK_PAID:
                return handleAdminMarkPaid(request);
            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Loại request không được hỗ trợ trong AdminHandler", null);
        }
    }

    private Response handleLockUser(Request request) {
        String userId = parsePayload(request, String.class);
        return userService.lockUser(userId);
    }

    private Response handleUnlockUser(Request request) {
        String userId = parsePayload(request, String.class);
        return userService.unlockUser(userId);
    }

    private Response handleAdminCancelAuction(Request request) {
        String auctionId = parsePayload(request, String.class);
        return auctionService.adminCancelAuction(auctionId);
    }

    private Response handleAdminMarkPaid(Request request) {
        String auctionId = parsePayload(request, String.class);
        return auctionService.adminMarkPaid(auctionId);
    }
}
