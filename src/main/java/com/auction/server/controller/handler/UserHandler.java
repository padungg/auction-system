package com.auction.server.controller.handler;

import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.server.service.UserService;

public class UserHandler extends BaseHandler {
    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    public Response handle(Request request, String loggedInUserId) {
        switch (request.getType()) {
            case LOGIN:
                return handleLogin(request);
            case REGISTER:
                return handleRegister(request);
            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Loại request không được hỗ trợ trong UserHandler", null);
        }
    }

    private Response handleLogin(Request request) {
        LoginDTO dto = parsePayload(request, LoginDTO.class);
        return userService.login(dto);
    }

    private Response handleRegister(Request request) {
        RegisterDTO dto = parsePayload(request, RegisterDTO.class);
        return userService.register(dto);
    }
}
