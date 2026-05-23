package com.auction.server.network;

import com.auction.server.controller.RequestController;
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
 */
public class SocketServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocketServer.class);

    private final int port;
    private final int maxClients;
    private final RequestController sharedController;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running = false;

    public SocketServer(int port, int maxClients, RequestController sharedController) {
        this.port = port;
        this.maxClients = maxClients;
        this.sharedController = sharedController;
    }

    /**
     * Khởi động server: mở socket và lắng nghe kết nối.
     */
    public void start() throws IOException {
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
}
