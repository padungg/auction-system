package com.auction.server.controller.handler;

import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;

public interface RequestHandler {
    Response handle(Request request, String loggedInUserId);
}
