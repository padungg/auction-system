package com.auction.server;

import com.auction.server.config.AppConfig;
import com.auction.server.network.SocketServer;
import com.auction.server.service.AuctionScheduler;
import com.auction.server.database.DatabaseInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Điểm khởi động Server: Nạp DB, khởi tạo DI, chạy Scheduler và mở SocketServer.
 */
public class ServerApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerApp.class);
    private static final int PORT = 8080;
    private static final int MAX_CLIENTS = 20;

    public static void main(String[] args) {
        // Khởi tạo Database (tạo bảng nếu chưa có)
        DatabaseInitializer.initialize();

        // Khởi tạo Dependency Injection container
        AppConfig appConfig = AppConfig.getInstance();

        // Khởi tạo Scheduler (dùng để đóng các phiên hết hạn)
        AuctionScheduler scheduler = new AuctionScheduler(appConfig.getAuctionDAO());

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
