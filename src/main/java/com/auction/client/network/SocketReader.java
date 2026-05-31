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
 * Luồng chạy ngầm liên tục đọc dữ liệu từ Socket.
 * Phân tích JSON nhận từ Server để phân loại: sự kiện realtime hoặc phản hồi cho request đồng bộ.
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
            // Đọc dòng dữ liệu liên tục từ socket
            while ((line = in.readLine()) != null) {
                LOGGER.info("<<< Nhận gói tin mạng từ Server: {}", line);
                try {
                    JsonObject json = gson.fromJson(line, JsonObject.class);

                    // Xử lý sự kiện Realtime (đặt giá, đóng phiên, v.v.)
                    if (json.has("event")) {
                        String eventName = json.get("event").getAsString();
                        String auctionId = json.has("auctionId") ? json.get("auctionId").getAsString() : "";

                        dispatcher.dispatch(eventName, auctionId, json);
                    }
                    // Xử lý phản hồi cho một Request đã gửi
                    else {
                        Response response = gson.fromJson(json, Response.class);
                        String reqId = response.getRequestId();

                        // Hoàn thành CompletableFuture đang chờ kết quả
                        if (reqId != null) {
                            CompletableFuture<Response> future = pendingRequests.remove(reqId);
                            if (future != null) {
                                future.complete(response);
                            }
                        }
                        // Dự phòng: Ghép đôi phản hồi với request cũ nhất nếu thiếu requestId
                        else {
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
            // Giải phóng các request đang chờ khi ngắt kết nối để tránh treo UI
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