package com.auction.server.controller.handler;

import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.service.AuctionService;

public class PaymentHandler extends BaseHandler {
    private final AuctionService auctionService;

    public PaymentHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, String loggedInUserId) {
        switch (request.getType()) {
            case GET_PENDING_PAYMENTS:
                return auctionService.getPendingPayments(loggedInUserId);
            case GET_PAYMENT_HISTORY:
                return auctionService.getPaymentHistory(loggedInUserId);
            case PAY_AUCTION:
                return handlePayAuction(request, loggedInUserId);
            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Loại request không được hỗ trợ trong PaymentHandler", null);
        }
    }

    private Response handlePayAuction(Request request, String userId) {
        String auctionId = parsePayload(request, String.class);
        return auctionService.payAuction(auctionId, userId);
    }
}
