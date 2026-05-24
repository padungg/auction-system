package com.auction.server.network;

import com.auction.model.dto.AuctionClosedNotificationDTO;
import com.auction.model.dto.BidUpdateNotificationDTO;
import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import com.auction.server.controller.RequestController;
import com.auction.server.observer.AuctionManager;
import com.auction.server.observer.AuctionObserver;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Xử lý kết nối của mỗi client trên một thread riêng biệt.
 * Gửi/nhận Request/Response và nhận thông báo realtime từ AuctionManager.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
    private static final Gson GSON = GsonProvider.getInstance();

    private final Socket socket;
    private final RequestController controller;
    private BufferedReader in;
    private PrintWriter out;
    private final Object outLock = new Object(); // khóa cố định cho I/O ghi socket
    private String loggedInUserId = null;

    public ClientHandler(Socket socket, RequestController controller) {
        this.socket = socket;
        this.controller = controller;
    }

    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        LOGGER.info("CONNECT: {}", clientAddr);

        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(
                    new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8),
                    true);

            String line;
            while ((line = in.readLine()) != null) {
                try {
                    handleRawMessage(line);
                } catch (Exception e) {
                    LOGGER.error("LỖI NGOẠI LỆ: {}", e.getMessage(), e);
                    sendResponse(
                            new Response(ResponseStatus.ERROR, "Lỗi máy chủ nội bộ (500): " + e.getMessage(), null));
                }
            }
        } catch (IOException e) {
            LOGGER.info("DISCONNECT: {} - {}", clientAddr, e.getMessage());
        } finally {
            cleanup(clientAddr);
        }
    }

    /**
     * Xử lý JSON từ client: phân luồng cho Subscribe hoặc chuyển tới Controller.
     */
    private void handleRawMessage(String json) {
        Request request;
        try {
            request = GSON.fromJson(json, Request.class);
        } catch (Exception e) {
            sendResponse(new Response(ResponseStatus.BAD_REQUEST, "JSON không hợp lệ: " + e.getMessage(), null));
            return;
        }

        if (request == null || request.getType() == null) {
            sendResponse(new Response(ResponseStatus.BAD_REQUEST, "Request thiếu trường 'type'", null));
            return;
        }

        // Tách riêng logic xử lý đăng ký nhận realtime
        if (handleSubscriptionRequest(request)) {
            return;
        }

        // Chuyển Request còn lại cho Controller xử lý (Core Logic)
        Response response = controller.handle(request, loggedInUserId);

        // Lưu trạng thái đăng nhập
        if (request.getType() == RequestType.LOGIN && response.getStatus() == ResponseStatus.SUCCESS) {
            UserResponseDTO userDTO = GSON.fromJson(GSON.toJson(response.getPayload()), UserResponseDTO.class);
            if (userDTO != null) {
                loggedInUserId = userDTO.getId();
                LOGGER.info("SESSION: userId={} đã đăng nhập", loggedInUserId);
            }
        }

        if (response != null && request.getRequestId() != null) {
            response.setRequestId(request.getRequestId());
        }

        sendResponse(response);
    }

    /**
     * Xử lý riêng các Request liên quan đến kết nối Realtime (Observer)
     */
    private boolean handleSubscriptionRequest(Request request) {
        if (request.getType() == RequestType.SUBSCRIBE_AUCTION) {
            String auctionId = GSON.fromJson(GSON.toJson(request.getPayload()), String.class);
            Response response;
            if (auctionId == null || auctionId.isBlank()) {
                response = new Response(ResponseStatus.BAD_REQUEST, "Thiếu auctionId", null);
            } else {
                AuctionManager.getInstance().subscribe(auctionId, this);
                response = new Response(ResponseStatus.SUCCESS, "Đã đăng ký nhận cập nhật cho phiên " + auctionId,
                        null);
            }
            if (request.getRequestId() != null) {
                response.setRequestId(request.getRequestId());
            }
            sendResponse(response);
            return true;
        } else if (request.getType() == RequestType.UNSUBSCRIBE_AUCTION) {
            String auctionId = GSON.fromJson(GSON.toJson(request.getPayload()), String.class);
            if (auctionId != null && !auctionId.isBlank()) {
                AuctionManager.getInstance().unsubscribe(auctionId, this);
            }
            Response response = new Response(ResponseStatus.SUCCESS, "Đã hủy đăng ký", null);
            if (request.getRequestId() != null) {
                response.setRequestId(request.getRequestId());
            }
            sendResponse(response);
            return true;
        }
        return false;
    }

    /**
     * Cập nhật realtime khi có bid mới.
     */
    @Override
    public void onBidUpdated(String auctionId, double newPrice, String bidderId,
            String bidderName, String itemName, String bidTime) {
        BidUpdateNotificationDTO notification = new BidUpdateNotificationDTO(
                auctionId, newPrice, bidderId, bidderName, itemName, bidTime);
        sendPush(GSON.toJson(notification));
    }

    /**
     * Cập nhật realtime khi phiên đóng.
     */
    @Override
    public void onAuctionClosed(String auctionId, double finalPrice, String winnerId) {
        AuctionClosedNotificationDTO notification = new AuctionClosedNotificationDTO(auctionId, finalPrice, winnerId);
        sendPush(GSON.toJson(notification));
    }

    private void sendResponse(Response response) {
        synchronized (outLock) {
            if (out != null)
                out.println(GSON.toJson(response));
        }
    }

    private void sendPush(String json) {
        synchronized (outLock) {
            if (out != null)
                out.println(json);
        }
    }

    /**
     * Dọn dẹp tài nguyên khi ngắt kết nối.
     */
    private void cleanup(String clientAddr) {
        AuctionManager.getInstance().unsubscribeAll(this);
        try {
            if (in != null)
                in.close();
        } catch (IOException e) {
            LOGGER.error("IN_CLOSE_ERROR: {}", e.getMessage());
        }
        if (out != null)
            out.close();
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.error("CLEANUP_ERROR: {}", e.getMessage());
        }
        LOGGER.info("CLEANUP: {} đã giải phóng tài nguyên", clientAddr);
    }
}
