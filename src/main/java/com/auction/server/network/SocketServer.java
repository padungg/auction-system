package com.auction.server.network;

import com.auction.server.controller.RequestController;
import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.AuctionDAOImpl;
import com.auction.server.dao.BidTransactionDAO;
import com.auction.server.dao.BidTransactionDAOImpl;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.ItemDAOImpl;
import com.auction.server.dao.UserDAO;
import com.auction.server.dao.UserDAOImpl;
import com.auction.server.dao.AutoBidDAO;
import com.auction.server.dao.AutoBidDAOImpl;
import com.auction.server.service.AuctionService;
import com.auction.server.service.AutoBidService;
import com.auction.server.service.BidService;
import com.auction.server.service.UserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SOCKET SERVER — Lắng nghe port, tạo thread cho từng client.
 *
 * Cơ chế:
 * - Dùng ThreadPool (FixedThreadPool) thay vì new Thread() thô:
 * → Giới hạn tối đa maxClients thread đồng thời
 * → Tái sử dụng thread → tránh chi phí khởi tạo thread liên tục
 * → Khi pool đầy: client mới phải chờ (không bị reject ngay)
 *
 * Dependency injection:
 * - SocketServer tạo toàn bộ DAO → Service → RequestController 1 lần duy nhất
 * - Mỗi ClientHandler nhận RequestController đã khởi tạo sẵn (không tạo lại)
 * → Tất cả handler dùng chung cùng 1 bộ Service + DAO → nhất quán dữ liệu
 *
 * Vòng đời:
 * ServerApp.main() → socketServer.start() → vòng lặp accept()
 * ServerApp shutdown hook → socketServer.stop()
 */
public class SocketServer {

    private final int port;
    private final int maxClients;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;

    /** Controller dùng chung cho tất cả ClientHandler */
    private RequestController sharedController;

    private volatile boolean running = false;

    public SocketServer(int port, int maxClients) {
        this.port = port;
        this.maxClients = maxClients;
    }

    // VÒNG ĐỜI

    /**
     * Khởi động server:
     * 1. Khởi tạo toàn bộ DAO → Service → Controller (DI)
     * 2. Mở ServerSocket
     * 3. Vòng lặp accept() — blocking, chạy trên thread gọi start()
     */
    public void start() throws IOException {
        initDependencies();

        serverSocket = new ServerSocket(port);
        threadPool = Executors.newFixedThreadPool(maxClients);
        running = true;

        System.out.println("[SocketServer] START: lắng nghe port " + port
                + " | tối đa " + maxClients + " client đồng thời");

        // Vòng lặp accept — blocking
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, sharedController);
                threadPool.submit(handler); // giao cho ThreadPool, không tạo thread thủ công
            } catch (IOException e) {
                if (running) {
                    System.out.println("[SocketServer] ACCEPT_ERROR: " + e.getMessage());
                }
                // Nếu !running → server đang stop, bỏ qua exception
            }
        }
    }

    /**
     * Dừng server sạch:
     * 1. Đánh dấu running = false → vòng lặp accept thoát
     * 2. Đóng ServerSocket → unblock accept() đang chờ
     * 3. Chờ thread pool hoàn thành tối đa 15s, sau đó force stop
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("[SocketServer] STOP_ERROR: " + e.getMessage());
        }

        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(15, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                    System.out.println("[SocketServer] FORCE_STOP: hủy các task chưa hoàn thành");
                } else {
                    System.out.println("[SocketServer] STOP: tất cả client đã ngắt kết nối sạch");
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // KHỞI TẠO DEPENDENCY

    /**
     * Khởi tạo toàn bộ dependency theo thứ tự:
     * DAO (truy cập DB) → Service (nghiệp vụ) → Controller (router)
     *
     * Thực hiện 1 lần khi server start.
     * Tất cả ClientHandler dùng chung sharedController này.
     */
    private void initDependencies() {
        // ── Tầng DAO ─────────────────────────────────────────────
        UserDAO           userDAO           = new UserDAOImpl();
        AuctionDAO        auctionDAO        = new AuctionDAOImpl();
        ItemDAO           itemDAO           = new ItemDAOImpl();
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
        AutoBidDAO        autoBidDAO        = new AutoBidDAOImpl();

        // ── Tầng Service ─────────────────────────────────────────
        UserService    userService    = new UserService(userDAO);
        AuctionService auctionService = new AuctionService(auctionDAO, itemDAO, userDAO);

        // AutoBidService cần AuctionDAO + BidTransactionDAO để persist auto-bid
        AutoBidService autoBidService = new AutoBidService(auctionDAO, bidTransactionDAO, autoBidDAO);

        // BidService nhận AutoBidService để trigger sau mỗi bid thủ công
        BidService bidService = new BidService(auctionDAO, bidTransactionDAO, autoBidService);

        // ── Tầng Controller ──────────────────────────────────────
        sharedController = new RequestController(userService, auctionService,
                                                 bidService, autoBidService);

        System.out.println("[SocketServer] INIT: DAO → Service → AutoBidService → Controller khởi tạo xong");
    }
}
