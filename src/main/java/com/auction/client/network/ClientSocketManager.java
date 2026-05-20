package com.auction.client.network;

import com.auction.client.observer.AuctionEventObserver;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.util.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Quản lý kết nối Socket TCP từ Client đến Server.
 * Áp dụng các mẫu thiết kế và kỹ thuật phần mềm:
 * - Singleton pattern: Đảm bảo duy trì duy nhất 1 cổng kết nối trong toàn bộ vòng đời ứng dụng.
 * - Observer pattern: Cho phép các Controller đăng ký nhận sự kiện đẩy thông báo real-time.
 * - Thread-safe: Sử dụng CopyOnWriteArrayList kết hợp Platform.runLater để phân phối dữ liệu an toàn trên UI Thread.
 */
public class ClientSocketManager {

    /**
     * Khởi tạo hệ thống ghi nhật ký log theo tiêu chuẩn SLF4J phục vụ việc giám sát luồng mạng,
     * theo dõi trạng thái đóng/mở gói tin Socket và kiểm soát tiến trình truyền phát sự kiện real-time.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSocketManager.class);

    private static ClientSocketManager instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson;

    private String host;
    private int port;

    /** Hàng đợi chặn cho luồng Request/Response đồng bộ (Khống chế xử lý tuần tự tại một thời điểm) */
    private final BlockingQueue<Response> responseQueue = new ArrayBlockingQueue<>(1);

    // OBSERVER PATTERN — Thread-safe với CopyOnWriteArrayList

    /** Danh sách các Observer đang cấu hình lắng nghe sự kiện real-time đẩy về từ Server */
    private final List<AuctionEventObserver> observers = new CopyOnWriteArrayList<>();

    private Thread listenerThread;

    private ClientSocketManager() {
        this.gson = GsonProvider.getInstance();
    }

    /**
     * Lấy thực thể duy nhất của ClientSocketManager (Singleton Pattern).
     */
    public static ClientSocketManager getInstance() {
        if (instance == null) {
            instance = new ClientSocketManager();
        }
        return instance;
    }

    /**
     * Đăng ký thêm Observer vào danh sách nhận thông báo đẩy thời gian thực từ hệ thống máy chủ.
     * Thường được kích hoạt tại chu kỳ khởi tạo phương thức initialize() của các Controller lớp trên.
     */
    public void addObserver(AuctionEventObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Hủy đăng ký và loại bỏ Observer khỏi danh sách nhận thông báo đẩy.
     * Cần gọi khi người dùng thực hiện chuyển trang hoặc nhấn nút quay lại để giải phóng bộ nhớ.
     */
    public void removeObserver(AuctionEventObserver observer) {
        observers.remove(observer);
    }

    /**
     * Phương thức tương thích ngược — đóng gói nội bộ addObserver thông qua cấu trúc wrapper ẩn danh.
     * @deprecated Ưu tiên chuyển dịch sử dụng addObserver(AuctionEventObserver) trực tiếp để tối ưu phân cấp tham số.
     */
    @Deprecated
    public void setNotificationListener(Consumer<JsonObject> listener) {
        addObserver((event, auctionId, payload) -> listener.accept(payload));
    }

    // KẾT NỐI SOCKET

    /**
     * Khởi tạo liên kết Socket TCP kết nối tới máy chủ Server theo cấu hình host và port chỉ định.
     * Thiết lập các cổng truyền dữ liệu đọc/ghi áp dụng bộ mã hóa ký tự chuẩn UTF-8.
     */
    public void connect(String host, int port) throws IOException {
        if (isConnected()) return;

        this.host = host;
        this.port = port;

        socket = new Socket(this.host, this.port);

        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

        LOGGER.info("Đã kết nối thành công đến máy chủ Server tại địa chỉ {}:{}", host, port);

        startListenerThread();
    }

    /**
     * Khởi chạy luồng nền Daemon liên tục lắng nghe và đọc dữ liệu luồng văn bản từ gói tin của Server.
     * - Nếu gói tin chứa trường định danh "event": Thực hiện broadcast chuyển tiếp dữ liệu đến toàn bộ Observer trên UI Thread.
     * - Nếu gói tin là một Response thông thường: Đẩy vào hàng đợi responseQueue phục vụ cơ chế xử lý đồng bộ.
     */
    private void startListenerThread() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    LOGGER.info("<<< Nhận gói tin mạng từ Server: {}", line);
                    JsonObject json = gson.fromJson(line, JsonObject.class);

                    if (json.has("event")) {
                        // Tiếp nhận Push Notification đẩy về từ máy chủ
                        String eventName = json.get("event").getAsString();
                        String auctionId = json.has("auctionId") ? json.get("auctionId").getAsString() : "";

                        // Thực hiện Broadcast an toàn: Chuyển tiếp tiến trình xử lý đồ họa của từng Observer về JavaFX UI Thread
                        for (AuctionEventObserver observer : observers) {
                            Platform.runLater(() -> observer.onAuctionEvent(eventName, auctionId, json));
                        }
                    } else {
                        // Tiếp nhận gói tin phản hồi (Response) cho một yêu cầu đồng bộ từ Client gửi lên trước đó
                        Response response = gson.fromJson(json, Response.class);
                        responseQueue.offer(response);
                    }
                }
            } catch (IOException e) {
                if (isConnected()) {
                    LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình đọc dữ liệu gói tin từ Server", e);
                }
            }
            LOGGER.info("Luồng đọc dữ liệu mạng nền (Socket-Listener Thread) đã dừng vận hành.");
        });
        listenerThread.setDaemon(true);
        listenerThread.setName("Socket-Listener");
        listenerThread.start();
    }

    // GỬI REQUEST / NHẬN RESPONSE

    /**
     * Gửi gói tin dữ liệu Request đồng bộ lên Server và tạm chặn luồng hiện tại để chờ gói tin phản hồi.
     * Cơ chế khống chế giới hạn thời gian phản hồi (Timeout) tối đa là 10 giây để tránh nghẽn mạch hệ thống.
     */
    public synchronized Response sendRequest(Request request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Hệ thống chưa thiết lập kết nối đến Server!");
        }

        responseQueue.clear();

        String jsonRequest = gson.toJson(request);
        LOGGER.info(">>> Gửi gói tin yêu cầu lên Server: {}", jsonRequest);
        out.println(jsonRequest);

        try {
            Response response = responseQueue.poll(10, TimeUnit.SECONDS);
            if (response == null) {
                throw new IOException("Hết thời gian chờ (Timeout): Máy chủ Server không phản hồi sau 10 giây");
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Tiến trình bị gián đoạn trong khi chờ phản hồi từ máy chủ", e);
        }
    }

    // TIỆN ÍCH KẾT NỐI

    /**
     * Thực hiện thu hồi tài nguyên luồng, giải phóng danh sách Observer, đóng các cổng luồng đọc/ghi dữ liệu
     * và cưỡng chế đóng cổng ngắt kết nối thực thể Socket TCP hiện hành.
     */
    public void disconnect() {
        try {
            observers.clear();
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (listenerThread != null) listenerThread.interrupt();
            LOGGER.info("Đã ngắt kết nối Socket hệ thống một cách an toàn.");
        } catch (IOException e) {
            LOGGER.error("Gặp sự cố lỗi khi thực hiện đóng cổng tài nguyên kết nối Socket", e);
        }
    }

    /**
     * Kiểm tra trạng thái hoạt động thực tế của đường truyền Socket mạng.
     * Trả về kết quả true nếu cổng kết nối đã được thiết lập thành công và chưa bị ngắt đóng.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Lấy ra thực thể Gson dùng chung đã được cấu hình các bộ Adapter tương thích dữ liệu trong ứng dụng.
     */
    public Gson getGson() {
        return gson;
    }
}