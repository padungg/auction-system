package com.auction.server.controller;

import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.controller.handler.*;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.BidService;
import com.auction.server.service.PaymentService;
import com.auction.server.service.UserService;
import com.auction.server.service.WalletService;
import com.auction.server.util.ValidationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * CONTROLLER — Router trung tâm của server.
 * Nhận Request từ ClientHandler,  gọi các Handler tương ứng qua Registry Pattern, trả Response.
 */
public class RequestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestController.class);

    private final Map<RequestType, RequestHandler> handlerMap;

    public RequestController(UserService userService,
                             WalletService walletService,
                             AuctionService auctionService,
                             BidService bidService,
                             AutoBidService autoBidService,
                             PaymentService paymentService) {

        handlerMap = new EnumMap<>(RequestType.class);

        // Khởi tạo các handlers
        UserHandler userHandler = new UserHandler(userService);
        AuctionHandler auctionHandler = new AuctionHandler(auctionService);
        BidHandler bidHandler = new BidHandler(bidService);
        AutoBidHandler autoBidHandler = new AutoBidHandler(autoBidService);
        AdminHandler adminHandler = new AdminHandler(userService, auctionService);
        AccountHandler accountHandler = new AccountHandler(walletService);
        PaymentHandler paymentHandler = new PaymentHandler(paymentService);

        // Thêm các request vào MAP
        // USER
        handlerMap.put(RequestType.LOGIN, userHandler);
        handlerMap.put(RequestType.REGISTER, userHandler);

        // AUCTION
        handlerMap.put(RequestType.GET_ALL_AUCTIONS, auctionHandler);
        handlerMap.put(RequestType.GET_AUCTION_DETAIL, auctionHandler);
        handlerMap.put(RequestType.CREATE_AUCTION, auctionHandler);
        handlerMap.put(RequestType.CLOSE_AUCTION, auctionHandler);
        handlerMap.put(RequestType.DELETE_AUCTION, auctionHandler);
        handlerMap.put(RequestType.UPDATE_AUCTION, auctionHandler);

        // BID
        handlerMap.put(RequestType.PLACE_BID, bidHandler);
        handlerMap.put(RequestType.GET_BID_HISTORY, bidHandler);
        handlerMap.put(RequestType.GET_MY_BID_HISTORY, bidHandler);

        // AUTO-BID
        handlerMap.put(RequestType.REGISTER_AUTO_BID, autoBidHandler);
        handlerMap.put(RequestType.CANCEL_AUTO_BID, autoBidHandler);

        // ADMIN
        handlerMap.put(RequestType.GET_ALL_USERS, adminHandler);
        handlerMap.put(RequestType.LOCK_USER, adminHandler);
        handlerMap.put(RequestType.UNLOCK_USER, adminHandler);
        handlerMap.put(RequestType.ADMIN_CANCEL_AUCTION, adminHandler);

        // ACCOUNT
        handlerMap.put(RequestType.GET_MY_PROFILE, accountHandler);
        handlerMap.put(RequestType.DEPOSIT, accountHandler);
        handlerMap.put(RequestType.WITHDRAW, accountHandler);
        handlerMap.put(RequestType.UPDATE_PROFILE, accountHandler);

        // PAYMENT
        handlerMap.put(RequestType.GET_PENDING_PAYMENTS, paymentHandler);
        handlerMap.put(RequestType.GET_PAYMENT_HISTORY, paymentHandler);
        handlerMap.put(RequestType.PAY_AUCTION, paymentHandler);
    }

    // ROUTER CHÍNH

    /**
     * Điều hướng Request đến đúng Handler tự động thông qua Map.
     */
    public Response handle(Request request, String loggedInUserId) {
        if (request == null || request.getType() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Request không hợp lệ", null);
        }

        try {
            RequestHandler handler = handlerMap.get(request.getType());

            if (handler != null) {
                return handler.handle(request, loggedInUserId);
            } else {
                return new Response(ResponseStatus.BAD_REQUEST,
                        "Loại request không được hỗ trợ: " + request.getType(), null);
            }
        } catch (ValidationException e) {
            return new Response(ResponseStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            LOGGER.error("[RequestController] LỖI NGOẠI LỆ nội bộ: {}", e.getMessage(), e);
            return new Response(ResponseStatus.ERROR, "Lỗi máy chủ (500): " + e.getMessage(), null);
        }
    }
}
