package com.auction.client.network;

import com.auction.model.protocol.Response;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Thread riêng liên tục lắng nghe message từ Server.
 *
 * Có 2 loại message nhận được:
 * 1. Response cho request đã gửi (LOGIN, GET_ALL_AUCTIONS, ...) -> chuyển vào callback
 * 2. Server push notification (real-time updates) -> gọi callback lên UI
 *
 * Protocol: BufferedReader.readLine() — khớp với Server dùng PrintWriter.println().
 */
public class ServerListener implements Runnable {

    private final BufferedReader in;
    private final Gson gson;
    private final MessageHandler messageHandler;
    private volatile boolean running = true;

    /**
     * Interface callback: khi nhận được 1 Response từ Server.
     */
    @FunctionalInterface
    public interface MessageHandler {
        void onMessage(Response response);
    }

    public ServerListener(BufferedReader in, Gson gson, MessageHandler messageHandler) {
        this.in = in;
        this.gson = gson;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            while (running) {
                // Đọc JSON string từ Server qua readLine (line-delimited protocol)
                String json = in.readLine();
                if (json == null) break;

                // Deserialize JSON -> Response (model.protocol.Response)
                Response response = gson.fromJson(json, Response.class);

                // Chuyển cho handler xử lý
                messageHandler.onMessage(response);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[ServerListener] Lỗi kết nối: " + e.getMessage());
            }
        }
        System.out.println("[ServerListener] Đã dừng lắng nghe.");
    }

    /**
     * Dừng thread lắng nghe.
     */
    public void stop() {
        running = false;
    }
}
