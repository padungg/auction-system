package com.auction.server.network;

import com.auction.model.dto.UserResponseDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import com.auction.server.controller.RequestController;
import com.auction.server.observer.AuctionManager;
import com.auction.server.observer.AuctionObserver;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * CLIENT HANDLER — Mỗi client kết nối tạo ra 1 instance, chạy trên 1 thread
 * riêng.
 *
 * Vai trò kép:
 * 1. Runnable — vòng lặp đọc Request → xử lý → gửi Response về client.
 * 2. AuctionObserver — nhận push realtime từ AuctionManager khi có bid mới /
 * phiên đóng,
 * ghi ngay xuống socket về client (không cần client phải poll).
 *
 * Vòng đời:
 * SocketServer.accept() → new ClientHandler(socket) → new
 * Thread(handler).start()
 * → run() loop đọc từng dòng JSON
 * → khi client ngắt kết nối hoặc lỗi: cleanup() → unsubscribeAll → đóng socket
 *
 * Thread-safety:
 * - PrintWriter out được dùng bởi 2 luồng:
 * (a) Thread của chính handler này (gửi Response sau khi xử lý request)
 * (b) Thread của AuctionManager (push notify khi có bid/close)
 * - Dùng synchronized(out) ở mọi chỗ ghi → tránh 2 luồng ghi socket cùng lúc.
 *
 * Session:
 * - loggedInUserId: null khi chưa login, set sau khi login thành công.
 * - Dùng để truyền vào RequestController (phân quyền bid, tạo phiên...).
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private static final Gson GSON = GsonProvider.getInstance();

    private final Socket socket;
    private final RequestController controller;

    private BufferedReader in;
    private PrintWriter out;

    /** UserId đang đăng nhập — null nếu chưa login */
    private String loggedInUserId = null;

    public ClientHandler(Socket socket, RequestController controller) {
        this.socket = socket;
        this.controller = controller;
    }

    // ════════════════════════════════════════════════════════════════
    // VÒNG LẶP CHÍNH (Runnable)
    // ════════════════════════════════════════════════════════════════

    @Override
    public void run() {
        String clientAddr = socket.getRemoteSocketAddress().toString();
        System.out.println("[ClientHandler] CONNECT: " + clientAddr);

        try {
            // Khởi tạo stream — UTF-8 để xử lý tiếng Việt trong JSON
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(socket.getOutputStream(), true); // autoFlush=true

            String line;
            // Mỗi message là 1 dòng JSON (line-delimited protocol)
            while ((line = in.readLine()) != null) {
                handleRawMessage(line);
            }

        } catch (IOException e) {
            // Client ngắt kết nối đột ngột — bình thường, không phải lỗi cần log ERROR
            System.out.println("[ClientHandler] DISCONNECT: " + clientAddr + " — " + e.getMessage());
        } finally {
            cleanup(clientAddr);
        }
    }

    // ════════════════════════════════════════════════════════════════
    // XỬ LÝ TỪNG MESSAGE
    // ════════════════════════════════════════════════════════════════

    /**
     * Parse JSON → Request → xử lý đặc biệt SUBSCRIBE/UNSUBSCRIBE (cần 'this')
     * → hoặc chuyển sang RequestController cho các loại còn lại.
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

        // SUBSCRIBE / UNSUBSCRIBE cần tham chiếu 'this' → xử lý ngay tại đây
        switch (request.getType()) {
            case SUBSCRIBE_AUCTION: {
                String auctionId = GSON.fromJson(GSON.toJson(request.getPayload()), String.class);
                if (auctionId == null || auctionId.isBlank()) {
                    sendResponse(new Response(ResponseStatus.BAD_REQUEST, "Thiếu auctionId", null));
                } else {
                    AuctionManager.getInstance().subscribe(auctionId, this);
                    sendResponse(new Response(ResponseStatus.SUCCESS,
                            "Đã đăng ký nhận cập nhật cho phiên " + auctionId, null));
                }
                return;
            }
            case UNSUBSCRIBE_AUCTION: {
                String auctionId = GSON.fromJson(GSON.toJson(request.getPayload()), String.class);
                if (auctionId != null && !auctionId.isBlank()) {
                    AuctionManager.getInstance().unsubscribe(auctionId, this);
                }
                sendResponse(new Response(ResponseStatus.SUCCESS, "Đã hủy đăng ký", null));
                return;
            }
            default:
                break;
        }

        // Mọi request khác → RequestController xử lý
        Response response = controller.handle(request, loggedInUserId);

        // Nếu login thành công → lưu userId vào session
        if (request.getType() == com.auction.model.protocol.RequestType.LOGIN
                && response.getStatus() == ResponseStatus.SUCCESS) {
            UserResponseDTO userDTO = GSON.fromJson(GSON.toJson(response.getPayload()), UserResponseDTO.class);
            if (userDTO != null) {
                loggedInUserId = userDTO.getId();
                System.out.println("[ClientHandler] SESSION: userId=" + loggedInUserId + " đã đăng nhập");
            }
        }

        sendResponse(response);
    }

    // ════════════════════════════════════════════════════════════════
    // OBSERVER CALLBACKS — push realtime từ AuctionManager
    // ════════════════════════════════════════════════════════════════

    /**
     * Được gọi bởi AuctionManager khi có bid mới — từ thread khác.
     * synchronized(out): đảm bảo chỉ 1 thread ghi socket tại 1 thời điểm.
     */
    @Override
    public void onBidUpdated(String auctionId, double newPrice, String bidderId) {
        // Tạo push notification dạng JSON riêng (không phải Response thông thường)
        JsonObject push = new JsonObject();
        push.addProperty("event", "BID_UPDATE");
        push.addProperty("auctionId", auctionId);
        push.addProperty("newPrice", newPrice);
        push.addProperty("bidderId", bidderId);
        sendPush(push.toString());
    }

    /**
     * Được gọi bởi AuctionManager khi phiên đấu giá kết thúc — từ thread khác.
     * Client nhận sự kiện này → khóa form bid, hiển thị banner "Phiên đã kết thúc".
     *
     * @param winnerId null nếu không có ai đặt giá
     */
    @Override
    public void onAuctionClosed(String auctionId, double finalPrice, String winnerId) {
        JsonObject push = new JsonObject();
        push.addProperty("event", "AUCTION_CLOSED");
        push.addProperty("auctionId", auctionId);
        push.addProperty("finalPrice", finalPrice);
        // winnerId có thể null → chỉ thêm khi != null
        if (winnerId != null) {
            push.addProperty("winnerId", winnerId);
        }
        sendPush(push.toString());
    }

    // ════════════════════════════════════════════════════════════════
    // TIỆN ÍCH
    // ════════════════════════════════════════════════════════════════

    /**
     * Gửi Response thông thường (kết quả xử lý Request) về client.
     * synchronized(out): tránh xung đột với push notify từ AuctionManager.
     */
    private void sendResponse(Response response) {
        synchronized (out) {
            out.println(GSON.toJson(response));
        }
    }

    /**
     * Gửi push notification (không phải Response) về client.
     * Dùng cho BID_UPDATE và AUCTION_CLOSED.
     */
    private void sendPush(String json) {
        synchronized (out) {
            out.println(json);
        }
    }

    /**
     * Dọn dẹp khi client ngắt kết nối:
     * 1. Hủy tất cả subscription → AuctionManager không push vào socket đã đóng
     * 2. Đóng socket
     */
    private void cleanup(String clientAddr) {
        AuctionManager.getInstance().unsubscribeAll(this);
        try {
            if (!socket.isClosed())
                socket.close();
        } catch (IOException e) {
            System.out.println("[ClientHandler] CLEANUP_ERROR: " + e.getMessage());
        }
        System.out.println("[ClientHandler] CLEANUP: " + clientAddr + " đã giải phóng tài nguyên");
    }
}
