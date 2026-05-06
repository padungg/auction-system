package com.auction.server;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AuctionDAOImpl;
import com.auction.server.network.SocketServer;
import com.auction.server.service.AuctionScheduler;

/**
 * ĐIỂM KHỞI ĐỘNG SERVER.
 *
 * Thứ tự khởi động:
 * 1. AuctionScheduler.start() — đóng các phiên đã hết hạn từ trước lần restart
 * 2. SocketServer.start() — bắt đầu nhận kết nối từ client (blocking)
 *
 * Shutdown Hook (Ctrl+C hoặc kill):
 * - Đảm bảo AuctionScheduler dừng sạch (không cắt giữa lúc đang update DB)
 * - Đảm bảo SocketServer dừng sạch (không cắt giữa lúc đang xử lý bid)
 */
public class ServerApp {

    private static final int PORT = 8080;
    private static final int MAX_CLIENTS = 20;

    public static void main(String[] args) {

        // ── 1. Khởi tạo DAO cho Scheduler ───────────────────────
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        AuctionScheduler scheduler = new AuctionScheduler(auctionDAO);

        // ── 2. Khởi tạo SocketServer ─────────────────────────────
        SocketServer socketServer = new SocketServer(PORT, MAX_CLIENTS);

        // ── 3. Shutdown Hook — chạy khi JVM nhận tín hiệu tắt ───
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[ServerApp] SHUTDOWN: đang dừng server...");
            scheduler.stop(); // dừng Scheduler trước (không block lâu)
            socketServer.stop(); // dừng SocketServer sau (chờ client xử lý xong)
            System.out.println("[ServerApp] SHUTDOWN: hoàn tất — goodbye!");
        }, "ShutdownHook-Thread"));

        // ── 4. Start Scheduler (non-blocking, chạy ngầm) ─────────
        scheduler.start();

        // ── 5. Start SocketServer (blocking — giữ main thread sống) ─
        try {
            socketServer.start(); // vòng lặp accept() — block tại đây
        } catch (Exception e) {
            System.out.println("[ServerApp] FATAL: " + e.getMessage());
            System.exit(1);
        }
    }
}
