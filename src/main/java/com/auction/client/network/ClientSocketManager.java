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
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * <h2>ClientSocketManager</h2>
 * <p>
 * Bộ quản lý tập trung kết nối Socket TCP (Central Network Connection Engine) phía Client.
 * Đóng vai trò là cổng giao tiếp mạng duy nhất nối tới Động cơ máy chủ Server.
 * </p>
 * * <p><b>Các mẫu thiết kế và kỹ thuật phần mềm cao cấp áp dụng:</b></p>
 * <ul>
 * <li><b>Bill Pugh Singleton Pattern:</b> Sử dụng lớp giữ thực thể tĩnh `InstanceHolder` giúp tối ưu hóa bộ nhớ, đảm bảo khởi tạo lười (Lazy Initialization) an toàn đa luồng tuyệt đối mà không cần sử dụng khối khóa synchronized hiệu năng thấp.</li>
 * <li><b>Facade Pattern:</b> Đóng vai trò là một giao diện đơn giản bọc toàn bộ sự phức tạp của hệ thống con bao gồm luồng độc lập `SocketReader` và công cụ phân phối `AuctionEventDispatcher`.</li>
 * <li><b>Non-blocking Request-Response Correlation Matrix:</b> Sử dụng ánh xạ bản đồ `ConcurrentMap` kết hợp cơ chế `CompletableFuture` và mã định danh tương quan ngẫu nhiên UUID (`requestId`). Cho phép hàng trăm luồng nghiệp vụ gọi hàm `sendRequest` song song cùng một thời điểm qua một Socket duy nhất mà không gây nghẽn luồng.</li>
 * <li><b>Resource Leak Prevention:</b> Cơ chế tự động quản lý vòng đời luồng rác Daemon Thread kết hợp giải phóng bộ nhớ ép buộc bằng `completeExceptionally` khi ngắt kết nối (`disconnect`).</li>
 * </ul>
 * * @since 1.0
 * @see com.auction.client.network.SocketReader
 * @see com.auction.client.network.AuctionEventDispatcher
 * @see java.util.concurrent.CompletableFuture
 */
public class ClientSocketManager {

    /** Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSocketManager.class);

    /** Thực thể kết nối mạng TCP Client-Server. */
    private Socket socket;

    /** Luồng xuất dữ liệu văn bản định dạng chuỗi JSON lên Server, bọc mã hóa UTF-8. */
    private PrintWriter out;

    /** Luồng nhập dữ liệu ký tự liên tục từ Server truyền về máy trạm Client. */
    private final BufferedReader in = null; // Giữ nguyên khai báo luồng in gốc của file

    /** Đối tượng định hình cấu trúc phân tích cú pháp chuỗi ký tự JSON từ cấu trúc Singleton của hệ thống. */
    private final Gson gson;

    /** Bản đồ đăng ký và theo dõi các Request bất đồng bộ đang treo để đối sánh Correlation ID với gói Response trả về. */
    private final ConcurrentMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    /** Bộ điều phối trung tâm phụ trách đẩy các sự kiện real-time (đặt giá, kết thúc) về cho các lớp View đồ họa. */
    private final AuctionEventDispatcher eventDispatcher = new AuctionEventDispatcher();

    /** Thread pool tự động co giãn (CachedThreadPool) bọc các Daemon Thread phục vụ các tác vụ kết nối luồng nền ngầm. */
    private ExecutorService executor;

    /** Đối tượng giám sát vòng đời luồng Reader chạy ngầm, hỗ trợ phát chỉ thị cưỡng chế ngắt luồng từ xa. */
    private Future<?> listenerFuture;

    /**
     * Hàm khởi tạo cấu trúc Private (Private Constructor).
     * Ngăn chặn hành vi khởi tạo thực thể tự do từ bên ngoài, duy trì tính toàn vẹn của Singleton.
     */
    private ClientSocketManager() {
        this.gson = GsonProvider.getInstance();
    }

    /**
     * Lớp giữ thực thể tĩnh (Holder Class) hỗ trợ Bill Pugh Singleton Pattern.
     * Cơ chế Classloader của máy ảo JVM sẽ đảm bảo lớp này chỉ được nạp và khởi tạo khi phương thức getInstance() được gọi lần đầu.
     */
    private static class InstanceHolder {
        private static final ClientSocketManager INSTANCE = new ClientSocketManager();
    }

    /**
     * Lấy thực thể duy nhất hiện hành của ClientSocketManager (Singleton Instance Accessor).
     * * @return {@link ClientSocketManager} Thực thể quản lý mạng tập trung
     */
    public static ClientSocketManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * Đảm bảo và duy trì trạng thái hoạt động của lõi Thread Pool (Executor Service Lifecycle Guard).
     * Nếu Thread Pool chưa được tạo hoặc đã bị hủy trước đó, tiến hành thiết lập lại tập hợp luồng Daemon
     * đính kèm cấu trúc đặt tên tường minh phục vụ công tác gỡ lỗi (Debugging).
     */
    private synchronized void ensureExecutorActive() {
        if (executor == null || executor.isShutdown()) {
            executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true); // Đánh dấu Daemon Thread để tự động giải phóng khi ứng dụng JavaFX tắt hoàn toàn
                t.setName("Socket-Worker-" + t.threadId());
                return t;
            });
        }
    }

    /**
     * Đẩy một tác vụ nghiệp vụ bất đồng bộ vào thực thi bên trong Thread Pool dùng chung (Thread Pool Delegation).
     * * @param task Đoạn mã thực thi nghiệp vụ dạng {@link Runnable}
     */
    public void execute(Runnable task) {
        ensureExecutorActive();
        executor.execute(task);
    }

    /**
     * Đăng ký thêm một bộ lắng nghe Observer vào danh sách nhận bản tin thông báo đẩy real-time từ hệ thống Server.
     * * @param observer Thực thể cấu hình {@link AuctionEventObserver}
     */
    public void addObserver(AuctionEventObserver observer) {
        eventDispatcher.addObserver(observer);
    }

    /**
     * Hủy bỏ quyền theo dõi và xóa Observer ra khỏi danh sách nhận phân phối bản tin thông báo đẩy thời gian thực.
     * * @param observer Thực thể cấu hình {@link AuctionEventObserver} cần loại bỏ
     */
    public void removeObserver(AuctionEventObserver observer) {
        eventDispatcher.removeObserver(observer);
    }

    /**
     * Phương thức tương thích ngược — đóng gói nội bộ addObserver thông qua cấu trúc wrapper ẩn danh.
     * * @param listener Đoạn mã Callback hàm Consumer tiếp nhận dữ liệu JsonObject thông báo đẩy
     * @deprecated Ưu tiên chuyển dịch sử dụng {@link #addObserver(AuctionEventObserver)} trực tiếp để tối ưu phân cấp tham số.
     */
    @Deprecated
    public void setNotificationListener(Consumer<JsonObject> listener) {
        addObserver((event, auctionId, payload) -> listener.accept(payload));
    }

    /**
     * Khởi tạo đường truyền kết nối mạng Socket TCP hướng dòng tới Server (Establish Network Connection).
     * Thiết lập cấu hình bộ đệm đọc/ghi luồng UTF-8 và kích hoạt luồng Worker Thread liên tục đọc gói tin trả về.
     * * @param host Địa chỉ IP hoặc tên miền định danh của máy chủ đích
     * @param port Cổng dịch vụ Socket của Server
     * @throws IOException Ngoại lệ phát sinh khi gán kết nối hoặc khởi tạo luồng IO luồng mạng thất bại
     */
    public void connect(String host, int port) throws IOException {
        if (isConnected()) return;

        ensureExecutorActive();
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

        // Đoạn code gán luồng in gốc của file
        BufferedReader inLocal = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

        LOGGER.info("Đã kết nối thành công đến máy chủ Server tại địa chỉ {}:{}", host, port);

        // Kích hoạt luồng Worker Thread liên tục bóc tách dữ liệu JSON
        startListener(inLocal);
    }

    /**
     * Khởi chạy luồng ngầm liên tục lắng nghe Server (Spawn Async Socket Reader Thread).
     * Khởi tạo đối tượng Runnable SocketReader và đẩy vào Executor Pool để chạy biệt lập.
     */
    private void startListener(BufferedReader inParam) {
        SocketReader reader = new SocketReader(inParam, gson, pendingRequests, eventDispatcher);
        listenerFuture = executor.submit(reader);
    }

    /**
     * Động cơ truyền nhận dữ liệu đồng thời bất đồng bộ (Concurrent Request-Response Dispatch Engine).
     * <p>
     * Sinh mã UUID ngẫu nhiên gắn vào trường `requestId` để làm Correlation ID, đăng ký Future vào bản đồ
     * quản lý an toàn đa luồng và đẩy chuỗi văn bản JSON lên đường truyền Socket.
     * </p>
     * Khối ghi được bảo vệ bằng từ khóa `synchronized` đảm bảo tính toàn vẹn gói tin dòng văn bản, ngăn hiện tượng
     * xen kẽ ký tự văn bản giữa các Thread. Hàm gọi chặn (Block) tối đa 10 giây để chờ kết quả đồng bộ bất đồng bộ từ luồng Reader.
     * * @param request Thực thể gói tin yêu cầu nghiệp vụ {@link Request}
     * @return {@link Response} Thực thể gói tin phản hồi chuẩn từ máy chủ Server
     * @throws IOException Ngoại lệ ném ra khi mất kết nối mạng, hết thời gian chờ (Timeout) hoặc gián đoạn luồng
     */
    public Response sendRequest(Request request) throws IOException {
        if (!isConnected()) {
            throw new IOException("Hệ thống chưa thiết lập kết nối đến Server!");
        }

        // Tạo Correlation ID duy nhất để ánh xạ chính xác Response khớp với Request này
        String requestId = java.util.UUID.randomUUID().toString();
        request.setRequestId(requestId);

        CompletableFuture<Response> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future); // Đăng ký Future vào bản đồ treo đợi mạng

        String jsonRequest = gson.toJson(request);
        LOGGER.info(">>> Gửi gói tin yêu cầu lên Server: {}", jsonRequest);

        // Chốt khóa luồng kết xuất PrintWriter chống hiện tượng nhiều luồng ghi đè xen kẽ văn bản lên Socket Stream
        synchronized (out) {
            out.println(jsonRequest);
        }

        try {
            // Chờ đợi phản hồi bất đồng bộ từ luồng Reader dội về (Ngưỡng khống chế an toàn tối đa 10 giây)
            Response response = future.get(10, TimeUnit.SECONDS);
            return response;
        } catch (TimeoutException e) {
            pendingRequests.remove(requestId); // Giải phóng bộ nhớ đệm đề phòng rò rỉ RAM (Memory Leak Prevention)
            throw new IOException("Hết thời gian chờ (Timeout): Máy chủ Server không phản hồi sau 10 giây");
        } catch (InterruptedException e) {
            pendingRequests.remove(requestId);
            Thread.currentThread().interrupt(); // Khôi phục lại cờ báo ngắt luồng hệ thống
            throw new IOException("Tiến trình bị gián đoạn trong khi chờ phản hồi từ máy chủ", e);
        } catch (ExecutionException e) {
            pendingRequests.remove(requestId);
            throw new IOException("Lỗi thực thi trong khi xử lý phản hồi từ máy chủ", e.getCause());
        }
    }

    /**
     * Quy trình thu hồi tài nguyên kết nối an toàn (Rigid Network Lifecycle Disconnect Resource Cleanup).
     * <p>
     * Giải phóng danh sách Observers, phát tín hiệu ném lỗi `completeExceptionally` cho toàn bộ Request đang bị treo
     * để giải phóng ngay lập tức các Thread đang bị chặn, thực hiện đóng tuần tự các luồng vào/ra, cưỡng chế đóng Socket TCP
     * và phát lệnh hủy hủy tác vụ `listenerFuture`.
     * </p>
     * Thực hiện tắt khẩn cấp lõi Thread Pool (`shutdownNow`) để triệt tiêu hoàn toàn nguy cơ rò rỉ luồng hệ thống (Thread Leak Fix).
     */
    public void disconnect() {
        try {
            eventDispatcher.clear(); // Hủy đăng ký lắng nghe sự kiện của toàn bộ các View màn hình

            // Duyệt giải phóng và phá vỡ trạng thái chặn (Blocking State) của toàn bộ các luồng nghiệp vụ đang đợi mạng
            for (CompletableFuture<Response> future : pendingRequests.values()) {
                future.completeExceptionally(new IOException("Đã chủ động ngắt kết nối hoặc mất tín hiệu từ máy chủ."));
            }
            pendingRequests.clear();

            // Đóng tuần tự hệ thống tài nguyên luồng ký tự hạ tầng mạng
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (listenerFuture != null) {
                listenerFuture.cancel(true); // Gửi cờ báo ngắt Interrupt ép luồng Reader dừng vòng lặp chặn I/O
            }

            // Triệt tiêu hoàn toàn vòng đời Thread Pool giải phóng bộ nhớ RAM hệ thống
            synchronized (this) {
                if (executor != null && !executor.isShutdown()) {
                    executor.shutdownNow(); // Cưỡng chế dừng toàn bộ luồng rác đang hoạt động
                }
            }

            LOGGER.info("Đã ngắt kết nối Socket hệ thống một cách an toàn.");
        } catch (IOException e) {
            LOGGER.error("Gặp sự cố lỗi khi thực hiện đóng cổng tài nguyên kết nối Socket", e);
        }
    }

    /**
     * Xác thực trạng thái liên kết logic và vật lý thực tế của đường truyền Socket kết nối (Connection State Broadcaster).
     * * @return {@code true} Nếu kết nối vật lý đang mở và sẵn sàng truyền tải gói tin; {@code false} nếu ngược lại
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * Truy xuất thực thể Gson dùng chung đã được tích hợp sẵn các bộ chuyển đổi Adapter dữ liệu hệ thống.
     * * @return {@link Gson} Đối tượng phân tích cú pháp JSON
     */
    public Gson getGson() {
        return gson;
    }
}