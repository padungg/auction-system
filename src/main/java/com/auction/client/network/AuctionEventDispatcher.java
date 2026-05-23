package com.auction.client.network;

import com.auction.client.observer.AuctionEventObserver;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * <h2>AuctionEventDispatcher</h2>
 * <p>
 * Bộ điều phối trung tâm sự kiện đấu giá thời gian thực (Real-time Event Dispatcher Core) phía Client.
 * Đảm nhiệm vai trò quản lý vòng đời các bộ lắng nghe (Observers) và phân phối các gói tin sự kiện mạng
 * nhận được từ Socket Server đến các thành phần giao diện một cách bất đồng bộ.
 * </p>
 * * <p><b>Các đặc tính kỹ thuật cốt lõi:</b></p>
 * <ul>
 * <li><b>An toàn đa luồng (Thread-Safe Registry):</b> Sử dụng cấu trúc `CopyOnWriteArrayList` giúp tối ưu hóa hiệu năng luồng, cho phép đăng ký hoặc hủy đăng ký Observer (Mở/Đóng phòng phiên) ngay trong lúc luồng mạng đang duyệt mảng phát tin mà không gây lỗi `ConcurrentModificationException`.</li>
 * <li><b>Cơ chế cô lập luồng UI (UI Thread Isolation):</b> Tích hợp sẵn `Platform::runLater` thông qua mẫu thiết kế Strategy, đảm bảo mọi tác vụ cập nhật giao diện đồ họa JavaFX luôn được đẩy về chạy trên JavaFX Application Thread một cách an toàn.</li>
 * <li><b>Kiến trúc kiểm thử linh hoạt (Testable Architecture):</b> Hỗ trợ ghi đè bộ chạy tác vụ (`taskExecutor`), cho phép hoán đổi luồng linh hoạt để chạy các kịch bản Unit Test (Mocking) trong môi trường không có giao diện đồ họa (Headless Environment).</li>
 * </ul>
 * * @since 1.0
 * @see com.auction.client.observer.AuctionEventObserver
 * @see java.util.concurrent.CopyOnWriteArrayList
 * @see javafx.application.Platform
 */
public class AuctionEventDispatcher {

    /** * Danh sách đăng ký Observers an toàn đa luồng.
     * Hỗ trợ tần suất đọc dữ liệu (Phát tin) cực cao từ luồng mạng và tần suất ghi (Đăng ký/Hủy phòng) thấp.
     */
    private final CopyOnWriteArrayList<AuctionEventObserver> observers = new CopyOnWriteArrayList<>();

    /** * Bộ điều phối thực thi tác vụ luồng (Functional Task Executor Strategy).
     * Mặc định liên kết tới luồng đồ họa JavaFX, được đánh dấu `volatile` để đảm bảo tính hiển thị nhất quán giữa các Thread.
     */
    private volatile Consumer<Runnable> taskExecutor = Platform::runLater;

    /**
     * Đăng ký bộ lắng nghe sự kiện mới vào hệ thống phân phối (Subscribe Observer).
     * Áp dụng cơ chế kiểm tra trùng lặp để đảm bảo một thực thể giao diện không bị nhận trùng luồng bản tin.
     * * @param observer Thực thể {@link AuctionEventObserver} đăng ký theo dõi phòng phiên đấu giá
     */
    public void addObserver(AuctionEventObserver observer) {
        observers.addIfAbsent(observer);
    }

    /**
     * Gỡ bỏ bộ lắng nghe ra khỏi danh sách phân phối sự kiện hệ thống (Unsubscribe Observer).
     * Được kích hoạt khi người dùng rút lui khỏi phòng phiên chi tiết hoặc chuyển đổi phân hệ màn hình.
     * * @param observer Thực thể {@link AuctionEventObserver} cần giải phóng bộ nhớ
     */
    public void removeObserver(AuctionEventObserver observer) {
        observers.remove(observer);
    }

    /**
     * Giải phóng sạch toàn bộ danh sách các bộ lắng nghe hiện hành (Clear All Subscriptions).
     * Thường được sử dụng trong tiến trình Đăng xuất tài khoản nhằm dọn dẹp bộ nhớ RAM và ngắt kết nối phòng phiên tổng.
     */
    public void clear() {
        observers.clear();
    }

    /**
     * Cấu hình thay đổi bộ thực thi tác vụ luồng ngầm (Dependency Injection Engine).
     * Cơ chế này vô cùng hữu ích cho việc cấu hình chạy Unit Test biệt lập (ví dụ: gán luồng chạy tuyến tính `Runnable::run`)
     * nhằm bỏ qua rào cản luồng đồ họa khi không có môi trường JavaFX Toolkit.
     * * @param taskExecutor Bộ đóng gói phân phối luồng chiến lược mới (Custom Thread Strategy)
     */
    public void setTaskExecutor(Consumer<Runnable> taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * Phát động phân phối gói tin sự kiện đa luồng (Broadcast Event Dispatcher Engine).
     * Vòng lặp duyệt qua toàn bộ danh sách Observers đã đăng ký hoạt động, đóng gói tác vụ phản hồi
     * và chuyển giao cho Strategy Executor đẩy về luồng đích xử lý an toàn.
     * * @param eventName Tên mã định danh loại sự kiện mạng đẩy về từ Server (ví dụ: BID_UPDATE, AUCTION_CLOSED)
     * @param auctionId Mã định danh duy nhất của phiên đấu giá phát sinh sự kiện mục tiêu
     * @param json      Cấu trúc dữ liệu JsonObject bọc chi tiết payload bản tin đẩy thời gian thực
     */
    public void dispatch(String eventName, String auctionId, JsonObject json) {
        // Vòng lặp an toàn không khóa (Lock-Free Thread-Safe Iteration) trên mảng CopyOnWrite
        for (AuctionEventObserver observer : observers) {
            // Ép luồng xử lý callback thực thi dựa trên chiến lược điều phối luồng đã cấu hình
            taskExecutor.accept(() -> observer.onAuctionEvent(eventName, auctionId, json));
        }
    }
}