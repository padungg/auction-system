package com.auction.client.network;

import com.auction.client.observer.AuctionEventObserver;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Bộ điều phối sự kiện đấu giá thời gian thực (Realtime Event Dispatcher) phía Client.
 * Sử dụng mô hình Observer để phân phối sự kiện mạng tới các thành phần UI đang lắng nghe.
 * Danh sách đăng ký là CopyOnWriteArrayList để đảm bảo an toàn đa luồng khi sửa đổi danh sách trong lúc duyệt.
 */
public class AuctionEventDispatcher {

    // Danh sách các Observer đang lắng nghe sự kiện đấu giá
    private final CopyOnWriteArrayList<AuctionEventObserver> observers = new CopyOnWriteArrayList<>();
    
    // Mặc định thực thi các callback cập nhật UI trên luồng đồ họa JavaFX (JavaFX Application Thread) qua Platform::runLater
    private volatile Consumer<Runnable> taskExecutor = Platform::runLater;

    /**
     * Đăng ký Observer mới nhận sự kiện đấu giá realtime.
     */
    public void addObserver(AuctionEventObserver observer) {
        observers.addIfAbsent(observer);
    }

    /**
     * Hủy đăng ký Observer nhận sự kiện đấu giá.
     */
    public void removeObserver(AuctionEventObserver observer) {
        observers.remove(observer);
    }

    /**
     * Xóa toàn bộ danh sách Observer đang lắng nghe.
     */
    public void clear() {
        observers.clear();
    }

    /**
     * Thay đổi bộ thực thi tác vụ (dùng trong viết Unit Test để chạy đồng bộ).
     */
    public void setTaskExecutor(Consumer<Runnable> taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * Điều hướng và phát tán sự kiện nhận được từ Server tới toàn bộ các Observer đã đăng ký.
     *
     * @param eventName Tên loại sự kiện (vd: bid_update, auction_close)
     * @param auctionId Mã định danh phiên đấu giá liên quan
     * @param json      Dữ liệu chi tiết đính kèm của sự kiện dưới dạng JsonObject
     */
    public void dispatch(String eventName, String auctionId, JsonObject json) {
        for (AuctionEventObserver observer : observers) {
            taskExecutor.accept(() -> observer.onAuctionEvent(eventName, auctionId, json));
        }
    }
}