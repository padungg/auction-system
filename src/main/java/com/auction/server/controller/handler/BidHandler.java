package com.auction.server.controller.handler;

import com.auction.model.dto.BidRequestDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.service.BidService;

public class BidHandler extends BaseHandler {
    private final BidService bidService;

    public BidHandler(BidService bidService) {
        this.bidService = bidService;
    }

    @Override
    public Response handle(Request request, String loggedInUserId) {
        switch (request.getType()) {
            case PLACE_BID:
                return handlePlaceBid(request, loggedInUserId);
            case GET_BID_HISTORY:
                return handleGetBidHistory(request);
            case GET_MY_BID_HISTORY:
                return bidService.getMyBidHistory(loggedInUserId);
            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Loại request không được hỗ trợ trong BidHandler", null);
        }
    }

    private Response handlePlaceBid(Request request, String bidderId) {
        BidRequestDTO dto = parsePayload(request, BidRequestDTO.class);
        return bidService.placeBid(dto, bidderId);
    }

    private Response handleGetBidHistory(Request request) {
        String auctionId = parsePayload(request, String.class);
        return bidService.getBidHistory(auctionId);
    }
}
