package com.auction.client.network;

import com.auction.model.protocol.Response;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Tiến trình nền (Thread) riêng biệt liên tục lắng nghe các gói tin văn bản từ máy chủ Server gửi về.
 * * Hệ thống phân loại xử lý 2 danh mục thông điệp (Message) nhận được:
 * 1. Gói tin phản hồi (Response) cho các yêu cầu đã gửi trước đó (LOGIN, GET_ALL_AUCTIONS...) -> Chuyển hướng vào bộ lọc Callback xử lý tuần tự.
 * 2. Gói thông báo đẩy (Server Push Notification) thời gian thực -> Kích hoạt Callback đồng bộ trực tiếp lên giao diện người dùng UI.
 * * Giao thức truyền tải (Protocol): Sử dụng cơ chế BufferedReader.readLine() — Đồng bộ tương thích hoàn toàn với cấu trúc PrintWriter.println() phía đầu Server.
 */
public class ServerListener implements Runnable {

    /**
     * Khởi tạo hệ thống ghi nhật ký log theo tiêu chuẩn SLF4J nhằm giám sát vòng đời luồng nghe mạng,
     * lưu vết các lỗi ngắt kết nối Socket bất ngờ và kiểm soát tiến trình deserialize chuỗi JSON JSON string.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerListener.class);

    private final BufferedReader in;
    private final Gson gson;
    private final MessageHandler messageHandler;
    private volatile boolean running = true;

    /**
     * Giao diện chức năng Callback (Functional Interface): Tiếp nhận và xử lý gói tin Response phản hồi từ Server.
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
                // Thực hiện đọc chuỗi văn bản JSON từ Server thông qua cơ chế ngắt dòng readLine (Line-delimited protocol)
                String json = in.readLine();
                if (json == null) {
                    break;
                }

                // Chuyển đổi cấu trúc chuỗi ký tự JSON (Deserialize) về đối tượng Response dạng Java Object
                Response response = gson.fromJson(json, Response.class);

                // Chuyển tiếp đối tượng kết quả cho Handler chịu trách nhiệm điều phối xử lý
                messageHandler.onMessage(response);
            }
        } catch (IOException e) {
            if (running) {
                LOGGER.error("Gặp sự cố lỗi kết nối mạng trong chu kỳ lắng nghe gói tin từ máy chủ Server", e);
            }
        }
        LOGGER.info("Tiến trình lắng nghe dữ liệu mạng từ Server (ServerListener Thread) đã dừng vận hành.");
    }

    /**
     * Phát tín hiệu ngắt dòng lặp và cưỡng chế dừng luồng chạy ngầm lắng nghe dữ liệu.
     */
    public void stop() {
        running = false;
    }
}