package com.auction.server;

import com.auction.server.controller.AuctionController;
import com.auction.server.controller.BidController;
import com.auction.server.controller.UserController;
import com.auction.server.network.ClientHandler;
import com.auction.server.controller.RequestRouter;
import com.auction.server.service.AuctionService;
import com.auction.server.service.BidService;
import com.auction.server.service.UserService;
import com.auction.server.dao.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Socket Server đa luồng — "Trái tim" của hệ thống.
 *
 * Khởi tạo theo thứ tự:
 *   1. DAO       → truy cập dữ liệu
 *   2. Service   → xử lý logic nghiệp vụ
 *   3. Controller → convert payload + gọi Service
 *   4. Router    → điều hướng request → Controller
 *   5. Server    → lắng nghe kết nối, tạo ClientHandler
 *
 * Luồng hoàn chỉnh:
 *   Client → ClientHandler → Router → Controller → Service → DAO
 */
public class ServerApp {

    private static final int PORT = 1234;
    private static final int MAX_CLIENTS = 20;

    public static void main(String[] args) {

        // ========== 1. KHỞI TẠO SINGLETON TÀI NGUYÊN ==========
        System.out.println(">>> [Hệ thống]: Đang khởi tạo kết nối Database (Singleton)...");
        com.auction.server.database.DatabaseConnection.getInstance();
        
        System.out.println(">>> [Hệ thống]: Đang khởi tạo Tổng đài Thực thi (Singleton)...");
        com.auction.server.observer.AuctionManager.getInstance();

        // ========== 2. KHỞI TẠO DAO ==========
        // TODO: Khi người làm DAO hoàn thành, thay Mock bằng Implementation thật
        UserDAO userDAO = new UserDAOImpl();
        ItemDAO itemDAO = new ItemDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();

        // ========== 2. KHỞI TẠO SERVICE ==========
        UserService userService = new UserService(userDAO);
        AuctionService auctionService = new AuctionService(auctionDAO, itemDAO, userDAO);
        BidService bidService = new BidService(auctionDAO, bidTransactionDAO);

        // ========== 3. KHỞI TẠO CONTROLLER ==========
        UserController userController = new UserController(userService);
        AuctionController auctionController = new AuctionController(auctionService);
        BidController bidController = new BidController(bidService);

        // ========== 4. KHỞI TẠO ROUTER ==========
        RequestRouter router = new RequestRouter(userController, auctionController, bidController);

        // ========== 5. KHỞI ĐỘNG SERVER SOCKET ==========
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("============================================");
            System.out.println("   HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN — SERVER");
            System.out.println("============================================");
            System.out.println(">>> [Hệ thống]: SERVER ĐANG CHẠY...");
            System.out.println(">>> [Hệ thống]: ĐANG ĐỢI KẾT NỐI TẠI CỔNG " + PORT + "...");
            System.out.println(">>> [Hệ thống]: Tối đa " + MAX_CLIENTS + " client đồng thời");
            System.out.println("--------------------------------------------");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, router);
                threadPool.execute(handler);
            }

        } catch (IOException e) {
            System.out.println(">>> [HỆ THỐNG]: LỖI SERVER: " + e.getMessage());
        } finally {
            threadPool.shutdown();
            System.out.println(">>> [HỆ THỐNG]: Server đã dừng.");
        }
    }
}