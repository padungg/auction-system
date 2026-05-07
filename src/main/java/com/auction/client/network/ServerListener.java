package com.auction.client.network;

import com.auction.model.protocol.Response;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Thread riêng liên tục lắng nghe message từ Server.
 *
 * Có 2 loại message nhận được:
 * 1. Response cho request đã gửi (LOGIN, GET_ALL_AUCTIONS, ...) -> chuyển vào callback
 * 2. Server push notification (real-time updates) -> gọi callback lên UI
 *
 * Protocol: DataInputStream.readUTF().
 */
public class ServerListener implements Runnable {

    private final DataInputStream in;
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

    public ServerListener(DataInputStream in, Gson gson, MessageHandler messageHandler) {
        this.in = in;
        this.gson = gson;
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            while (running) {
                // Đọc JSON string từ Server qua readUTF
                String json = in.readUTF();
                if (json == null) break;

                // Deserialize JSON -> Response (model.protocol.Response)
                Response response = gson.fromJson(json, Response.class);

                // Chuyển cho handler xử lý
                messageHandler.onMessage(response);
            }
        } catch (EOFException e) {
            System.out.println("[ServerListener] Server đã ngắt kết nối (EOF).");
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
