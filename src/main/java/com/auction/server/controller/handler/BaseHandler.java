package com.auction.server.controller.handler;

import com.auction.model.protocol.Request;
import com.auction.model.util.GsonProvider;
import com.google.gson.Gson;
import java.lang.reflect.Type;

public abstract class BaseHandler implements RequestHandler {
    protected static final Gson GSON = GsonProvider.getInstance();

    protected <T> T parsePayload(Request request, Class<T> clazz) {
        return GSON.fromJson(GSON.toJson(request.getPayload()), clazz);
    }

    protected <T> T parsePayload(Request request, Type typeOfT) {
        return GSON.fromJson(GSON.toJson(request.getPayload()), typeOfT);
    }
}
