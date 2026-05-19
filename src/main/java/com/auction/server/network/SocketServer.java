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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Lắng nghe kết nối và xử lý thông qua ThreadPool.
 * Khởi tạo DI cho DAO, Service, Controller dùng chung.
 */
public class SocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketServer.class);

    private final int port;
    private final int maxClients;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private RequestController sharedController;
    private volatile boolean running = false;

    public SocketServer(int port, int maxClients) {
        this.port = port;
        this.maxClients = maxClients;
    }

    /**
     * Khởi động server: nạp dependencies, mở socket và lắng nghe kết nối.
     */
    public void start() throws IOException {
        initDependencies();

        serverSocket = new ServerSocket(port);
        threadPool = Executors.newFixedThreadPool(maxClients);
        running = true;

        LOGGER.info("START: lắng nghe port {} | tối đa {} client đồng thời", port, maxClients);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket, sharedController);
                threadPool.submit(handler);
            } catch (IOException e) {
                if (running) {
                    LOGGER.error("ACCEPT_ERROR: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Dừng server an toàn, ngắt kết nối và giải phóng tài nguyên.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            LOGGER.error("STOP_ERROR: {}", e.getMessage());
        }

        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(15, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                    LOGGER.warn("FORCE_STOP: hủy các task chưa hoàn thành");
                } else {
                    LOGGER.info("STOP: tất cả client đã ngắt kết nối sạch");
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Khởi tạo toàn bộ dependency theo thứ tự: DAO → Service → Controller.
     */
    private void initDependencies() {
        UserDAO userDAO = new UserDAOImpl();
        AuctionDAO auctionDAO = new AuctionDAOImpl();
        ItemDAO itemDAO = new ItemDAOImpl();
        BidTransactionDAO bidTransactionDAO = new BidTransactionDAOImpl();
        AutoBidDAO autoBidDAO = new AutoBidDAOImpl();

        UserService userService = new UserService(userDAO);
        AuctionService auctionService = new AuctionService(auctionDAO, itemDAO, userDAO, bidTransactionDAO);
        AutoBidService autoBidService = new AutoBidService(auctionDAO, bidTransactionDAO, autoBidDAO);
        BidService bidService = new BidService(auctionDAO, bidTransactionDAO, autoBidService, itemDAO);

        sharedController = new RequestController(userService, auctionService, bidService, autoBidService);

        LOGGER.info("INIT: Khởi tạo xong DAO, Service và Controller");
    }
}
