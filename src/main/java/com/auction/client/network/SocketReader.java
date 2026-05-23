package com.auction.client.network;

import com.auction.model.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CompletableFuture;

/**
 * <h2>SocketReader</h2>
 * <p>
 * Luồng chạy ngầm liên tục tiếp nhận và phân phối dữ liệu từ Socket (Asynchronous Socket Reader Component) phía Client.
 * Hiện thực hóa interface {@link Runnable} để vận hành độc lập trên một Worker Thread riêng biệt, tránh gây nghẽn luồng UI.
 * </p>
 * * <p><b>Các cơ chế kỹ thuật và mô hình tích hợp:</b></p>
 * <ul>
 * <li><b>Vòng lặp hướng dòng (Stream-Driven Loop):</b> Lắng nghe liên tục luồng văn bản (Text-based Stream) trả về từ Server qua cấu trúc `BufferedReader` cho đến khi kết nối bị ngắt.</li>
 * <li><b>Kiến trúc phân phối rẽ nhánh (Dual-Path Dispatch Engine):</b>
 * <ul>
 * <li><i>Nhánh Push Notification (Sự kiện):</i> Phát hiện trường `event`, chuyển tiếp Json về cho {@link AuctionEventDispatcher} để đẩy lên giao diện thời gian thực.</li>
 * <li><i>Nhánh Request-Response (Phản hồi trực tiếp):</i> Trích xuất mã định danh tương quan (Correlation ID - `requestId`) để tìm kiếm, gỡ bỏ và kích hoạt hoàn thành (Resolve) Future tương ứng trong `ConcurrentMap`.</li>
 * </ul>
 * </li>
 * <li><b>Cơ chế phòng vệ lỗi (Fault-Tolerant Fallback & Cleanup):</b>
 * <ul>
 * <li><i>Fallback:</i> Tự động hoàn thành request lâu nhất đang chờ nếu Server phản hồi thiếu trường `requestId` (FIFO Fallback).</li>
 * <li><i>Giải phóng bộ nhớ (Resource Cleanup):</i> Khối `finally` đảm bảo kích hoạt lỗi `completeExceptionally` cho toàn bộ các Request đang treo nếu xảy ra sự cố sập mạng hoặc ngắt kết nối Socket đột ngột.</li>
 * </ul>
 * </li>
 * </ul>
 * * @since 1.0
 * @see java.lang.Runnable
 * @see java.util.concurrent.CompletableFuture
 * @see java.util.concurrent.ConcurrentMap
 * @see com.auction.client.network.AuctionEventDispatcher
 */
public class SocketReader implements Runnable {

    /** Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng phục vụ công tác giám sát luồng IO mạng. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketReader.class);

    /** Luồng ký tự đầu vào kết nối trực tiếp tới luồng mạng của Socket Server. */
    private final BufferedReader in;

    /** Thực thể Gson dùng để phân tích cú pháp (Deserialization) chuỗi văn bản JSON thành đối tượng Java. */
    private final Gson gson;

    /** Bản đồ lưu giữ các Request đang chờ xử lý, sử dụng cơ chế an toàn đa luồng để ánh xạ giữa requestId và CompletableFuture. */
    private final ConcurrentMap<String, CompletableFuture<Response>> pendingRequests;

    /** Bộ điều phối trung tâm chịu trách nhiệm bắn sự kiện real-time (đặt giá, đóng phiên) về cho giao diện người dùng. */
    private final AuctionEventDispatcher dispatcher;

    /**
     * Khởi tạo một đối tượng SocketReader với đầy đủ các thành phần phụ thuộc (Dependency Injection Constructor).
     * * @param in              Luồng văn bản đầu vào (`BufferedReader`) của Socket
     * @param gson            Bộ công cụ JSON Parser (`Gson`)
     * @param pendingRequests Bản đồ quản lý các Request đang treo (`ConcurrentMap`)
     * @param dispatcher      Bộ phân phối sự kiện thời gian thực (`AuctionEventDispatcher`)
     */
    public SocketReader(BufferedReader in,
                        Gson gson,
                        ConcurrentMap<String, CompletableFuture<Response>> pendingRequests,
                        AuctionEventDispatcher dispatcher) {
        this.in = in;
        this.gson = gson;
        this.pendingRequests = pendingRequests;
        this.dispatcher = dispatcher;
    }

    /**
     * Thực thi luồng ngầm liên tục lắng nghe và xử lý gói tin từ Server (Thread Main Loop).
     * Phân tích cú pháp chuỗi JSON nhận được, phân loại gói tin dựa trên cấu trúc thuộc tính
     * và thực hiện phân phối bất đồng bộ về các phân hệ đích tương ứng.
     */
    @Override
    public void run() {
        try {
            String line;
            // Vòng lặp chặn (Blocking Loop) liên tục đọc từng dòng văn bản cho đến khi Server ngắt kết nối
            while ((line = in.readLine()) != null) {
                LOGGER.info("<<< Nhận gói tin mạng từ Server: {}", line);
                try {
                    JsonObject json = gson.fromJson(line, JsonObject.class);

                    // =========================================================================
                    // PHÂN NHÁNH 1: TIẾP NHẬN BẢN TIN SỰ KIỆN ĐẨY (PUSH NOTIFICATION)
                    // =========================================================================
                    if (json.has("event")) {
                        String eventName = json.get("event").getAsString();
                        String auctionId = json.has("auctionId") ? json.get("auctionId").getAsString() : "";

                        // Chuyển giao gói tin Json sự kiện sang cho Event Dispatcher để cập nhật UI
                        dispatcher.dispatch(eventName, auctionId, json);

                        // =========================================================================
                        // PHÂN NHÁNH 2: TIẾP NHẬN BẢN TIN PHẢN HỒI THEO YÊU CẦU (REQUEST-RESPONSE)
                        // =========================================================================
                    } else {
                        Response response = gson.fromJson(json, Response.class);
                        String reqId = response.getRequestId();

                        if (reqId != null) {
                            // Trích xuất và loại bỏ Future ra khỏi hàng đợi xử lý an toàn đa luồng
                            CompletableFuture<Response> future = pendingRequests.remove(reqId);
                            if (future != null) {
                                future.complete(response); // Đánh dấu hoàn thành, giải phóng Thread đang gọi phương thức get()
                            }
                        } else {
                            // RÀO CẢN DỰ PHÒNG (FALLBACK MECHANISM):
                            // Nếu Server trả về thiếu requestId, tiến hành ép hoàn thành request đầu tiên/cũ nhất đang đợi trong Map
                            if (!pendingRequests.isEmpty()) {
                                String oldestKey = pendingRequests.keySet().iterator().next();
                                CompletableFuture<Response> future = pendingRequests.remove(oldestKey);
                                if (future != null) {
                                    future.complete(response);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Lỗi phân tích cú pháp gói tin mạng JSON: {}", line, e);
                }
            }
        } catch (IOException e) {
            LOGGER.info("Luồng đọc Socket (SocketReader) dừng vận hành hoặc đã đóng kết nối.");
        } finally {
            // =========================================================================
            // ĐẢM BẢO GIẢI PHÓNG BỘ NHỚ KHI GẶP SỰ CỐ ĐỨT MẠNG ĐỘT NGỘT (CLEANUP STATE)
            // =========================================================================
            if (!pendingRequests.isEmpty()) {
                LOGGER.warn("Đường truyền Socket đóng đột ngột. Giải phóng {} request đang đợi.", pendingRequests.size());

                // Duyệt qua toàn bộ các Future đang bị treo để ném ngoại lệ, tránh việc các luồng gửi Request bị treo vô hạn
                for (CompletableFuture<Response> future : pendingRequests.values()) {
                    future.completeExceptionally(new IOException("Kết nối tới máy chủ đã bị ngắt đột ngột."));
                }
                pendingRequests.clear(); // Dọn sạch bản đồ bộ nhớ đệm
            }
        }
    }
}