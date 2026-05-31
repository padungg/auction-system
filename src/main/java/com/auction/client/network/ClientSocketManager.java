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
 * Quản lý kết nối Socket TCP phía client.
 * Thực hiện gửi request đồng bộ và nhận các sự kiện realtime từ Server bằng dữ liệu định dạng JSON.
 */
public class ClientSocketManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSocketManager.class);

    private Socket socket;
    private PrintWriter out;
    private final Gson gson;

    // Lưu trữ các request đang chờ phản hồi để đối chiếu bằng requestId
    private final ConcurrentMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    private final AuctionEventDispatcher eventDispatcher = new AuctionEventDispatcher();

    private ExecutorService executor;
    private Future<?> listenerFuture;

    // Khóa đồng bộ hóa thao tác ghi dữ liệu ra socket
    private final Object writeLock = new Object();

    private ClientSocketManager() {
        this.gson = GsonProvider.getInstance();
    }

    private static class InstanceHolder {
        private static final ClientSocketManager INSTANCE = new ClientSocketManager();
    }

    /**
     * Lấy instance duy nhất (Singleton) của ClientSocketManager.
     */
    public static ClientSocketManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Đảm bảo ExecutorService hoạt động để xử lý các luồng chạy ngầm.
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
     * Thực thi tác vụ bất đồng bộ bằng thread pool.
     */
    public void execute(Runnable task) {
        ensureExecutorActive();
        executor.execute(task);
    }

    /**
     * Đăng ký observer nhận sự kiện realtime.
     */
    public void addObserver(AuctionEventObserver observer) {
        eventDispatcher.addObserver(observer);
    }

    /**
     * Hủy đăng ký observer nhận sự kiện.
     */
    public void removeObserver(AuctionEventObserver observer) {
        eventDispatcher.removeObserver(observer);
    }

    @Deprecated
    public void setNotificationListener(Consumer<JsonObject> listener) {
        addObserver((_, _, payload) -> listener.accept(payload));
    }

    /**
     * Kết nối TCP tới Server và khởi động luồng đọc ngầm.
     */
    public void connect(String host, int port) throws IOException {
        if (isConnected()) return;

        ensureExecutorActive();
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        BufferedReader inLocal = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        LOGGER.info("Đã kết nối thành công đến máy chủ Server tại địa chỉ {}:{}", host, port);

        startListener(inLocal);
    }

    /**
     * Bắt đầu luồng đọc dữ liệu liên tục từ socket.
     */
    private void startListener(BufferedReader inParam) {
        SocketReader reader = new SocketReader(inParam, gson, pendingRequests, eventDispatcher);
        listenerFuture = executor.submit(reader);
    }

    /**
     * Gửi request đồng bộ lên Server và chờ phản hồi tối đa 10 giây.
     */
    public Response sendRequest(Request request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Hệ thống chưa thiết lập kết nối đến Server!");
        }

        // Tạo ID duy nhất cho request để đối chiếu response
        String requestId = java.util.UUID.randomUUID().toString();
        request.setRequestId(requestId);

        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        String jsonRequest = gson.toJson(request);
        LOGGER.info(">>> Gửi gói tin yêu cầu lên Server: {}", jsonRequest);

        // Ghi dữ liệu đồng bộ ra socket
        synchronized (writeLock) {
            if (out != null) {
                out.println(jsonRequest);
            }
        }

        try {
            // Đợi kết quả từ SocketReader
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
     * Ngắt kết nối socket, giải phóng tài nguyên và hủy các request đang chờ.
     */
    public void disconnect() {
        try {
            eventDispatcher.clear();

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
     * Kiểm tra trạng thái kết nối của socket.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public Gson getGson() {
        return gson;
    }
}