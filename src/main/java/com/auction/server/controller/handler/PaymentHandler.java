package com.auction.server.controller.handler;

import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.service.PaymentService;

public class PaymentHandler extends BaseHandler {
    private final PaymentService paymentService;

    public PaymentHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public Response handle(Request request, String loggedInUserId) {
        switch (request.getType()) {
            case GET_PENDING_PAYMENTS:
                return paymentService.getPendingPayments(loggedInUserId);
            case GET_PAYMENT_HISTORY:
                return paymentService.getPaymentHistory(loggedInUserId);
            case PAY_AUCTION:
                return handlePayAuction(request, loggedInUserId);
            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Loại request không được hỗ trợ trong PaymentHandler", null);
        }
    }

    private Response handlePayAuction(Request request, String userId) {
        String auctionId = parsePayload(request, String.class);
        return paymentService.payAuction(auctionId, userId);
    }
}
