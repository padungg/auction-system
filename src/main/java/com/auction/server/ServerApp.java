package com.auction.server;

import com.auction.server.config.AppConfig;
import com.auction.server.network.SocketServer;
import com.auction.server.service.AuctionScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Điểm khởi động Server: Nạp DB, khởi tạo DI, chạy Scheduler và mở SocketServer.
 */
public class ServerApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);
    // Cấu hình từ biến môi trường (Environment Variables) hoặc dùng giá trị mặc định
    private static final int PORT = getEnv("SERVER_PORT", 8080);
    private static final int MAX_CLIENTS = getEnv("MAX_CLIENTS", 20);

    private static int getEnv(String key, int defaultValue) {
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("Cấu hình {} = {} không hợp lệ, dùng mặc định: {}", key, val, defaultValue);
            }
        }
        return defaultValue;
    }

    public static void main(String[] args) {
        // Database đã được khởi tạo và quản lý trực tiếp trên Supabase (không cần chạy code tạo bảng ở đây nữa)

        // Khởi tạo Dependency Injection container
        AppConfig appConfig = AppConfig.getInstance();

        // Khởi tạo Scheduler (dùng để đóng các phiên hết hạn)
        AuctionScheduler scheduler = new AuctionScheduler(appConfig.getAuctionDAO(), appConfig.getAuctionService());

        // Khởi tạo SocketServer
        SocketServer socketServer = new SocketServer(PORT, MAX_CLIENTS, appConfig.getRequestController());

        // Shutdown Hook: Đảm bảo dừng an toàn Scheduler và SocketServer khi tắt
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("SHUTDOWN: đang dừng server...");
            scheduler.stop();
            socketServer.stop();
            LOGGER.info("SHUTDOWN: hoàn tất - goodbye!");
        }, "ShutdownHook-Thread"));

        // Chạy Scheduler ngầm
        scheduler.start();

        // Chạy SocketServer
        try {
            socketServer.start();
        } catch (Exception e) {
            LOGGER.error("FATAL: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
