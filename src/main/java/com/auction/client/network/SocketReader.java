package com.auction.client.network;

import com.auction.model.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;

/**
 * Luồng chạy ngầm liên tục nhận và phân phối dữ liệu từ Socket mạng.
 */
public class SocketReader implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketReader.class);

    private final BufferedReader in;
    private final Gson gson;
    private final ConcurrentMap<String, CompletableFuture<Response>> pendingRequests;
    private final AuctionEventDispatcher dispatcher;

    public SocketReader(BufferedReader in,
                        Gson gson,
                        ConcurrentMap<String, CompletableFuture<Response>> pendingRequests,
                        AuctionEventDispatcher dispatcher) {
        this.in = in;
        this.gson = gson;
        this.pendingRequests = pendingRequests;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                LOGGER.info("<<< Nhận gói tin mạng từ Server: {}", line);
                try {
                    JsonObject json = gson.fromJson(line, JsonObject.class);

                    if (json.has("event")) {
                        String eventName = json.get("event").getAsString();
                        String auctionId = json.has("auctionId") ? json.get("auctionId").getAsString() : "";

                        dispatcher.dispatch(eventName, auctionId, json);
                    } else {
                        Response response = gson.fromJson(json, Response.class);
                        String reqId = response.getRequestId();

                        if (reqId != null) {
                            CompletableFuture<Response> future = pendingRequests.remove(reqId);
                            if (future != null) {
                                future.complete(response);
                            }
                        } else {
                            if (!pendingRequests.isEmpty()) {
                                String oldestKey = pendingRequests.keySet().iterator().next();
                                CompletableFuture<Response> future = pendingRequests.remove(oldestKey);
                                if (future != null) {
                                    future.complete(response);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Lỗi phân tích cú pháp gói tin mạng JSON: {}", line, e);
                }
            }
        } catch (IOException e) {
            LOGGER.info("Luồng đọc Socket (SocketReader) dừng vận hành hoặc đã đóng kết nối.");
        } finally {
            if (!pendingRequests.isEmpty()) {
                LOGGER.warn("Đường truyền Socket đóng đột ngột. Giải phóng {} request đang đợi.", pendingRequests.size());

                for (CompletableFuture<Response> future : pendingRequests.values()) {
                    future.completeExceptionally(new IOException("Kết nối tới máy chủ đã bị ngắt đột ngột."));
                }
                pendingRequests.clear();
            }
        }
    }
}