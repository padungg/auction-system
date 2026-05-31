package com.auction.server.controller.handler;

import com.auction.model.dto.AutoBidDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.service.AutoBidService;

public class AutoBidHandler extends BaseHandler {
    private final AutoBidService autoBidService;

    public AutoBidHandler(AutoBidService autoBidService) {
        this.autoBidService = autoBidService;
    }

    @Override
    public Response handle(Request request, String loggedInUserId) {
        switch (request.getType()) {
            case REGISTER_AUTO_BID:
                return handleRegisterAutoBid(request, loggedInUserId);
            case CANCEL_AUTO_BID:
                return handleCancelAutoBid(request, loggedInUserId);
            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Loại request không được hỗ trợ trong AutoBidHandler", null);
        }
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
