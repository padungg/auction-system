package com.auction.server.network;

import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.*;
import com.auction.server.controller.RequestRouter;
import com.auction.server.observer.AuctionManager;
import com.auction.server.observer.AuctionObserver;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Bộ xử lý cho MỘT client — chạy trên 1 thread riêng.
 *
 * Implement AuctionObserver: khi có bid mới trên phiên đang xem → tự động gửi JSON về client.
 *
 * Luồng hoạt động:
 *   1. Client gửi JSON string (Request) qua writeUTF()
 *   2. ClientHandler đọc bằng readUTF()
 *   3. Gson parse JSON → Request object
 *   4. Chuyển cho RequestRouter xử lý → nhận Response
 *   5. Nếu SUBSCRIBE → đăng ký Observer
 *   6. Gson serialize Response → JSON string
 *   7. Gửi về Client bằng writeUTF()
 *   8. Lặp lại cho đến khi client ngắt kết nối
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private final Socket socket;
    private final RequestRouter router;
    private final Gson gson;

    /**
     * ID của user đang đăng nhập trên kết nối này.
     */
    private String currentUserId;

    /**
     * Ống gửi dữ liệu về client — cần lưu lại để Observer có thể gửi thông báo.
     */
    private DataOutputStream out;

    public ClientHandler(Socket socket, RequestRouter router) {
        this.socket = socket;
        this.router = router;
        this.gson = GsonConfig.createGson();
        this.currentUserId = null;
    }

    @Override
    public void run() {
        String clientAddress = socket.getInetAddress().getHostAddress();
        System.out.println(">>> [Kết nối] Client " + clientAddress + " đã kết nối.");

        try (
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream outStream = new DataOutputStream(socket.getOutputStream())
        ) {
            this.out = outStream; // Lưu lại để onBidUpdated() có thể dùng

            // ========== VÒNG LẶP CHÍNH ==========
            while (true) {
                // 1. Đọc JSON string từ Client
                String jsonRequest = in.readUTF();
                System.out.println(">>> [Nhận] từ " + clientAddress + ": " + jsonRequest);

                // 2. Parse JSON → Request object
                Request request = gson.fromJson(jsonRequest, Request.class);

                // 3. Xử lý SUBSCRIBE/UNSUBSCRIBE trực tiếp (không qua Router)
                if (request.getType() == RequestType.SUBSCRIBE_AUCTION) {
                    handleSubscribe(request);
                    continue;
                }
                if (request.getType() == RequestType.UNSUBSCRIBE_AUCTION) {
                    handleUnsubscribe(request);
                    continue;
                }

                // 4. Chuyển cho Router xử lý → nhận Response
                Response response = router.route(request, currentUserId);

                // 5. Nếu LOGIN thành công → lưu userId cho các request sau
                if (request.getType() == RequestType.LOGIN
                        && response.getStatus() == ResponseStatus.SUCCESS) {
                    extractUserIdFromResponse(response);
                }

                // 6. Serialize Response → JSON string
                String jsonResponse = gson.toJson(response);

                // 7. Gửi JSON về Client
                out.writeUTF(jsonResponse);
                out.flush();
                System.out.println(">>> [Gửi] đến " + clientAddress + ": " + response.getStatus());
            }

        } catch (EOFException e) {
            System.out.println(">>> [Ngắt] Client " + clientAddress + " đã ngắt kết nối.");
        } catch (IOException e) {
            System.out.println(">>> [Lỗi] Client " + clientAddress + ": " + e.getMessage());
        } finally {
            // Hủy toàn bộ subscription khi client ngắt
            AuctionManager.getInstance().unsubscribeAll(this);

            try {
                socket.close();
            } catch (IOException e) {
                System.out.println(">>> [Lỗi] Không thể đóng socket: " + e.getMessage());
            }
            System.out.println(">>> [Dọn dẹp] Đã giải phóng tài nguyên cho client " + clientAddress);
        }
    }

    // ==================== OBSERVER: Nhận thông báo bid mới ====================

    /**
     * OBSERVER PATTERN — Được gọi tự động khi có bid mới trên phiên đang theo dõi.
     * Gửi JSON thông báo realtime về client mà KHÔNG CẦN client hỏi.
     */
    @Override
    public void onBidUpdated(String auctionId, double newPrice, String bidderId) {
        try {
            if (out != null) {
                // Tạo payload thông báo
                Map<String, Object> updateData = new HashMap<>();
                updateData.put("auctionId", auctionId);
                updateData.put("newPrice", newPrice);
                updateData.put("bidderId", bidderId);

                Response notification = new Response(
                        ResponseStatus.SUCCESS,
                        "BID_UPDATE",
                        updateData
                );

                String json = gson.toJson(notification);
                synchronized (out) { // Đồng bộ để tránh 2 thread ghi cùng lúc
                    out.writeUTF(json);
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.out.println(">>> [Observer] Không gửi được thông báo bid: " + e.getMessage());
        }
    }

    // ==================== SUBSCRIBE / UNSUBSCRIBE ====================

    /**
     * Xử lý đăng ký theo dõi phiên đấu giá (realtime update).
     */
    private void handleSubscribe(Request request) throws IOException {
        String auctionId = gson.toJson(request.getPayload());
        auctionId = gson.fromJson(auctionId, String.class);

        AuctionManager.getInstance().subscribe(auctionId, this);

        Response response = new Response(ResponseStatus.SUCCESS,
                "Đã đăng ký theo dõi phiên " + auctionId, null);
        out.writeUTF(gson.toJson(response));
        out.flush();
    }

    /**
     * Xử lý hủy theo dõi phiên đấu giá.
     */
    private void handleUnsubscribe(Request request) throws IOException {
        String auctionId = gson.toJson(request.getPayload());
        auctionId = gson.fromJson(auctionId, String.class);

        AuctionManager.getInstance().unsubscribe(auctionId, this);

        Response response = new Response(ResponseStatus.SUCCESS,
                "Đã hủy theo dõi phiên " + auctionId, null);
        out.writeUTF(gson.toJson(response));
        out.flush();
    }

    // ==================== HELPER ====================

    /**
     * Trích xuất userId từ Response sau khi login thành công.
     */
    private void extractUserIdFromResponse(Response response) {
        try {
            String json = gson.toJson(response.getPayload());
            UserResponseDTO userDTO = gson.fromJson(json, UserResponseDTO.class);
            if (userDTO != null && userDTO.getId() != null) {
                this.currentUserId = userDTO.getId();
                System.out.println(">>> [Login] User đã đăng nhập: " + userDTO.getUsername()
                        + " (ID: " + currentUserId + ")");
            }
        } catch (Exception e) {
            System.out.println(">>> [Cảnh báo] Không trích xuất được userId: " + e.getMessage());
        }
    }
}
