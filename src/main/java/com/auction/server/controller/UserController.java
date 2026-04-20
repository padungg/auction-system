package com.auction.server.controller;

import com.auction.model.dto.LoginDTO;
import com.auction.model.dto.RegisterDTO;
import com.auction.model.protocol.Response;
import com.auction.server.network.GsonConfig;
import com.auction.server.service.UserService;
import com.google.gson.Gson;

/**
 * Controller xử lý các request liên quan đến User.
 *
 * Vai trò: Nhận payload thô từ Router → convert sang DTO → gọi Service → trả Response.
 * Controller KHÔNG chứa logic nghiệp vụ — chỉ làm "cầu nối" giữa Router và Service.
 */
public class UserController {

    private final UserService userService;
    private final Gson gson;

    public UserController(UserService userService) {
        this.userService = userService;
        this.gson = GsonConfig.createGson();
    }

    /**
     * Xử lý request LOGIN.
     * Convert payload → LoginDTO → gọi UserService.login()
     */
    public Response handleLogin(Object payload) {
        String json = gson.toJson(payload);
        LoginDTO dto = gson.fromJson(json, LoginDTO.class);
        return userService.login(dto);
    }

    /**
     * Xử lý request REGISTER.
     * Convert payload → RegisterDTO → gọi UserService.register()
     */
    public Response handleRegister(Object payload) {
        String json = gson.toJson(payload);
        RegisterDTO dto = gson.fromJson(json, RegisterDTO.class);
        return userService.register(dto);
    }
}
