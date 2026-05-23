package com.auction.server.controller.handler;

import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.dto.UpdateAuctionDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.service.AuctionService;

public class AuctionHandler extends BaseHandler {
    private final AuctionService auctionService;

    public AuctionHandler(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public Response handle(Request request, String loggedInUserId) {
        switch (request.getType()) {
            case GET_ALL_AUCTIONS:
                return auctionService.getAllAuctions();
            case GET_AUCTION_DETAIL:
                return handleGetAuctionDetail(request);
            case CREATE_AUCTION:
                return handleCreateAuction(request, loggedInUserId);
            case CLOSE_AUCTION:
                return handleCloseAuction(request);
            case DELETE_AUCTION:
                return handleDeleteAuction(request, loggedInUserId);
            case UPDATE_AUCTION:
                return handleUpdateAuction(request, loggedInUserId);
            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Loại request không được hỗ trợ trong AuctionHandler", null);
        }
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

    private Response handleDeleteAuction(Request request, String sellerId) {
        String auctionId = parsePayload(request, String.class);
        return auctionService.deleteAuctionItem(auctionId, sellerId);
    }

    private Response handleUpdateAuction(Request request, String sellerId) {
        UpdateAuctionDTO dto = parsePayload(request, UpdateAuctionDTO.class);
        return auctionService.updateAuctionItem(dto, sellerId);
    }
}
