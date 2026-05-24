package com.auction.client.network;

import com.auction.client.observer.AuctionEventObserver;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.Response;
import com.auction.model.util.GsonProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Bộ quản lý kết nối Socket TCP phía Client (Client Socket Connection Manager).
 * Đóng vai trò cầu nối truyền thông điệp dạng JSON giữa Client và Server.
 * Áp dụng mẫu thiết kế Singleton và an toàn đa luồng (Thread-safe) trong toàn bộ thao tác I/O.
 */
public class ClientSocketManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSocketManager.class);

    private Socket socket;
    private PrintWriter out;
    private final Gson gson;
    
    // Lưu các yêu cầu đang đợi phản hồi từ máy chủ (để khớp nối đồng bộ qua ID yêu cầu)
    private final ConcurrentMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();
    
    // Bộ điều phối sự kiện đấu giá realtime tới các thành phần đăng ký lắng nghe
    private final AuctionEventDispatcher eventDispatcher = new AuctionEventDispatcher();
    
    private ExecutorService executor;
    private Future<?> listenerFuture;

    // Khóa ghi đồng bộ tránh tranh chấp ghi socket từ nhiều luồng đồng thời
    private final Object writeLock = new Object();

    private ClientSocketManager() {
        this.gson = GsonProvider.getInstance();
    }

    private static class InstanceHolder {
        private static final ClientSocketManager INSTANCE = new ClientSocketManager();
    }

    /**
     * Lấy thực thể duy nhất của ClientSocketManager.
     */
    public static ClientSocketManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Đảm bảo Executor Service phục vụ các tác vụ mạng ngầm luôn hoạt động.
     */
    private synchronized void ensureExecutorActive() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("Socket-Worker-" + t.threadId());
                return t;
            });
        }
    }

    /**
     * Thực thi một tác vụ bất đồng bộ ngầm bằng luồng worker an toàn.
     */
    public void execute(Runnable task) {
        ensureExecutorActive();
        executor.execute(task);
    }

    /**
     * Đăng ký nhận sự kiện realtime từ Socket (Bid mới, kết thúc phiên, v.v.).
     */
    public void addObserver(AuctionEventObserver observer) {
        eventDispatcher.addObserver(observer);
    }

    /**
     * Hủy đăng ký nhận sự kiện realtime từ Socket.
     */
    public void removeObserver(AuctionEventObserver observer) {
        eventDispatcher.removeObserver(observer);
    }

    /**
     * Đăng ký lắng nghe sự kiện realtime (Phương thức cũ, khuyến khích sử dụng addObserver).
     */
    @Deprecated
    public void setNotificationListener(Consumer<JsonObject> listener) {
        addObserver((_, _, payload) -> listener.accept(payload));
    }

    /**
     * Khởi tạo đường truyền kết nối mạng Socket TCP đến Server.
     * Khởi động luồng đọc ngầm để lắng nghe dữ liệu liên tục từ Server.
     *
     * @param host Địa chỉ IP hoặc tên máy chủ (Hostname)
     * @param port Cổng kết nối (Port) của máy chủ
     * @throws IOException Xảy ra nếu lỗi thiết lập kết nối mạng
     */
    public void connect(String host, int port) throws IOException {
        if (isConnected()) return;

        ensureExecutorActive();
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        BufferedReader inLocal = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        LOGGER.info("Đã kết nối thành công đến máy chủ Server tại địa chỉ {}:{}", host, port);

        // Bắt đầu chạy luồng đọc dữ liệu từ Socket (SocketReader)
        startListener(inLocal);
    }

    /**
     * Khởi chạy luồng SocketReader ngầm để liên tục nhận gói tin JSON trả về.
     */
    private void startListener(BufferedReader inParam) {
        SocketReader reader = new SocketReader(inParam, gson, pendingRequests, eventDispatcher);
        listenerFuture = executor.submit(reader);
    }

    /**
     * Gửi yêu cầu mạng đồng bộ sang máy chủ qua Socket và chờ đợi phản hồi.
     * Phương thức này sẽ chặn luồng gọi hiện tại tối đa 10 giây để chờ kết quả khớp ID.
     *
     * @param request Gói tin yêu cầu chứa thông tin payload và loại request
     * @return Phản hồi (Response) trả về từ máy chủ
     * @throws IOException Xảy ra nếu mất kết nối hoặc máy chủ phản hồi quá hạn (Timeout)
     */
    public Response sendRequest(Request request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Hệ thống chưa thiết lập kết nối đến Server!");
        }

        // Tạo mã định danh ngẫu nhiên duy nhất cho mỗi yêu cầu để ánh xạ phản hồi chính xác
        String requestId = java.util.UUID.randomUUID().toString();
        request.setRequestId(requestId);

        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        String jsonRequest = gson.toJson(request);
        LOGGER.info(">>> Gửi gói tin yêu cầu lên Server: {}", jsonRequest);

        // Khóa đồng bộ bảo vệ luồng xuất socket tránh bị xen ngang
        synchronized (writeLock) {
            if (out != null) {
                out.println(jsonRequest);
            }
        }

        try {
            // Chờ tối đa 10 giây để nhận kết quả khớp từ luồng SocketReader
            return future.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(requestId);
            throw new IOException("Hết thời gian chờ (Timeout): Máy chủ Server không phản hồi sau 10 giây");
        } catch (InterruptedException e) {
            pendingRequests.remove(requestId);
            Thread.currentThread().interrupt();
            throw new IOException("Tiến trình bị gián đoạn trong khi chờ phản hồi từ máy chủ", e);
        } catch (ExecutionException e) {
            pendingRequests.remove(requestId);
            throw new IOException("Lỗi thực thi trong khi xử lý phản hồi từ máy chủ", e.getCause());
        }
    }

    /**
     * Giải phóng tài nguyên và ngắt kết nối an toàn với máy chủ.
     * Hoàn tất các yêu cầu mạng đang đợi với ngoại lệ ngắt kết nối và tắt luồng thread worker.
     */
    public void disconnect() {
        try {
            eventDispatcher.clear();

            // Đóng tất cả các request đang đợi để tránh treo luồng Client
            for (CompletableFuture<Response> future : pendingRequests.values()) {
                future.completeExceptionally(new IOException("Đã chủ động ngắt kết nối hoặc mất tín hiệu từ máy chủ."));
            }
            pendingRequests.clear();

            synchronized (writeLock) {
                if (out != null) {
                    out.close();
                    out = null;
                }
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            if (listenerFuture != null) {
                listenerFuture.cancel(true);
            }

            synchronized (this) {
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }

            LOGGER.info("Đã ngắt kết nối Socket hệ thống một cách an toàn.");
        } catch (IOException e) {
            LOGGER.error("Gặp sự cố lỗi khi thực hiện đóng cổng tài nguyên kết nối Socket", e);
        }
    }

    /**
     * Kiểm tra xem kết nối Socket đến Server có đang hoạt động hay không.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Lấy đối tượng Gson dùng chung trong kết nối.
     */
    public Gson getGson() {
        return gson;
    }
}