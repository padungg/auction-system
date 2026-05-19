package com.auction.server.controller;

import com.auction.model.dto.AutoBidDTO;
import com.auction.model.dto.BidRequestDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.dto.UpdateAuctionDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.BidService;
import com.auction.server.service.UserService;
import com.auction.server.util.ValidationException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CONTROLLER — Router trung tâm của server.
 * Nhận Request từ ClientHandler → deserialize payload → gọi đúng Service → trả
 * Response.
 */
public class RequestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestController.class);
    private static final Gson GSON = GsonProvider.getInstance();

    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;
    private final AutoBidService autoBidService;

    public RequestController(UserService userService,
                             AuctionService auctionService,
                             BidService bidService,
                             AutoBidService autoBidService) {
        this.userService = userService;
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.autoBidService = autoBidService;
    }

    // ROUTER CHÍNH

    /**
     * Điều hướng Request đến đúng Service.
     */
    public Response handle(Request request, String loggedInUserId) {
        if (request == null || request.getType() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Request không hợp lệ", null);
        }

        try {
            switch (request.getType()) {

                // USER
                case LOGIN:
                    return handleLogin(request);

                case REGISTER:
                    return handleRegister(request);

                // AUCTION
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

                // BID
                case PLACE_BID:
                    return handlePlaceBid(request, loggedInUserId);

                case GET_BID_HISTORY:
                    return handleGetBidHistory(request);

                case GET_MY_BID_HISTORY:
                    return bidService.getMyBidHistory(loggedInUserId);

                // AUTO-BID
                case REGISTER_AUTO_BID:
                    return handleRegisterAutoBid(request, loggedInUserId);

                case CANCEL_AUTO_BID:
                    return handleCancelAutoBid(request, loggedInUserId);

                // ADMIN
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

                // ACCOUNT
                case GET_MY_PROFILE:
                    return userService.getMyProfile(loggedInUserId);
                case DEPOSIT:
                    return handleDeposit(request, loggedInUserId);

                case WITHDRAW:
                    return handleWithdraw(request, loggedInUserId);

                case UPDATE_PROFILE:
                    return handleUpdateProfile(request, loggedInUserId);

                // PAYMENT
                case GET_PENDING_PAYMENTS:
                    return auctionService.getPendingPayments(loggedInUserId);

                case GET_PAYMENT_HISTORY:
                    return auctionService.getPaymentHistory(loggedInUserId);

                case PAY_AUCTION:
                    return handlePayAuction(request, loggedInUserId);

                default:
                    return new Response(ResponseStatus.BAD_REQUEST,
                            "Loại request không được hỗ trợ: " + request.getType(), null);
            }
        } catch (ValidationException e) {
            return new Response(ResponseStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            LOGGER.error("[RequestController] LỖI NGOẠI LỆ nội bộ: " + e.getMessage(), e);
            return new Response(ResponseStatus.ERROR, "Lỗi máy chủ (500): " + e.getMessage(), null);
        }
    }

    // CÁC HANDLER RIÊNG — mỗi handler phụ trách 1 loại request

    /**
     * Chuyển đổi payload từ dạng Object thô sang kiểu dữ liệu mong muốn (Class).
     */
    private <T> T parsePayload(Request request, Class<T> clazz) {
        return GSON.fromJson(GSON.toJson(request.getPayload()), clazz);
    }

    /**
     * Chuyển đổi payload từ dạng Object thô sang kiểu dữ liệu Generic mong muốn
     */
    private <T> T parsePayload(Request request, Type typeOfT) {
        return GSON.fromJson(GSON.toJson(request.getPayload()), typeOfT);
    }

    // USER

    private Response handleLogin(Request request) {
        LoginDTO dto = parsePayload(request, LoginDTO.class);
        return userService.login(dto);
    }

    private Response handleRegister(Request request) {
        RegisterDTO dto = parsePayload(request, RegisterDTO.class);
        return userService.register(dto);
    }

    // AUCTION
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

    // BID

    private Response handlePlaceBid(Request request, String bidderId) {
        BidRequestDTO dto = parsePayload(request, BidRequestDTO.class);
        return bidService.placeBid(dto, bidderId);
    }

    private Response handleGetBidHistory(Request request) {
        String auctionId = parsePayload(request, String.class);
        return bidService.getBidHistory(auctionId);
    }

    // AUTO-BID

    private Response handleRegisterAutoBid(Request request, String userId) {
        AutoBidDTO dto = parsePayload(request, AutoBidDTO.class);
        return autoBidService.register(dto, userId);
    }

    private Response handleCancelAutoBid(Request request, String userId) {
        String auctionId = parsePayload(request, String.class);
        return autoBidService.cancel(auctionId, userId);
    }

    // ADMIN

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

    // ACCOUNT

    private Response handleDeposit(Request request, String userId) {
        double amount = parsePayload(request, Double.class);
        return userService.deposit(userId, amount);
    }

    private Response handleWithdraw(Request request, String userId) {
        double amount = parsePayload(request, Double.class);
        return userService.withdraw(userId, amount);
    }

    private Response handleUpdateProfile(Request request, String userId) {
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> payload = parsePayload(request, type);

        if (payload == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Payload không hợp lệ", null);
        }

        return userService.updateProfile(userId, payload);
    }

    // PAYMENT

    private Response handlePayAuction(Request request, String userId) {
        String auctionId = parsePayload(request, String.class);
        return auctionService.payAuction(auctionId, userId);
    }
}