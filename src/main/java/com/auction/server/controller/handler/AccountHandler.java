package com.auction.server.controller.handler;

import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.service.WalletService;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class AccountHandler extends BaseHandler {
    private final WalletService walletService;

    public AccountHandler(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public Response handle(Request request, String loggedInUserId) {
        switch (request.getType()) {
            case GET_MY_PROFILE:
                return walletService.getMyProfile(loggedInUserId);
            case DEPOSIT:
                return handleDeposit(request, loggedInUserId);
            case WITHDRAW:
                return handleWithdraw(request, loggedInUserId);
            case UPDATE_PROFILE:
                return handleUpdateProfile(request, loggedInUserId);
            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Loại request không được hỗ trợ trong AccountHandler", null);
        }
    }

    private Response handleDeposit(Request request, String userId) {
        double amount = parsePayload(request, Double.class);
        return walletService.deposit(userId, amount);
    }

    private Response handleWithdraw(Request request, String userId) {
        double amount = parsePayload(request, Double.class);
        return walletService.withdraw(userId, amount);
    }

    private Response handleUpdateProfile(Request request, String userId) {
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> payload = parsePayload(request, type);

        if (payload == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Payload không hợp lệ", null);
        }

        return walletService.updateProfile(userId, payload);
    }
}
