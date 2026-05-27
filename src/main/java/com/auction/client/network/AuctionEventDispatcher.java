package com.auction.client.network;

import com.auction.client.observer.AuctionEventObserver;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Điều phối các sự kiện đấu giá thời gian thực tới các UI component đang lắng nghe.
 * Sử dụng CopyOnWriteArrayList để đảm bảo an toàn đa luồng khi đăng ký/hủy observer.
 */
public class AuctionEventDispatcher {

    private final CopyOnWriteArrayList<AuctionEventObserver> observers = new CopyOnWriteArrayList<>();

    // Mặc định chạy tác vụ cập nhật giao diện trên luồng đồ họa JavaFX qua Platform::runLater
    private volatile Consumer<Runnable> taskExecutor = Platform::runLater;

    /**
     * Đăng ký observer mới để nhận sự kiện realtime.
     */
    public void addObserver(AuctionEventObserver observer) {
        observers.addIfAbsent(observer);
    }

    /**
     * Hủy đăng ký observer.
     */
    public void removeObserver(AuctionEventObserver observer) {
        observers.remove(observer);
    }

    /**
     * Xóa toàn bộ danh sách observer.
     */
    public void clear() {
        observers.clear();
    }

    /**
     * Thay đổi executor thực thi tác vụ (chủ yếu dùng cho Unit Test chạy đồng bộ).
     */
    public void setTaskExecutor(Consumer<Runnable> taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * Phân phối sự kiện nhận từ Server đến toàn bộ observer đã đăng ký.
     *
     * @param eventName Tên sự kiện (ví dụ: bid_update, auction_close)
     * @param auctionId ID của phiên đấu giá liên quan
     * @param json      Dữ liệu chi tiết của sự kiện
     */
    public void dispatch(String eventName, String auctionId, JsonObject json) {
        for (AuctionEventObserver observer : observers) {
            taskExecutor.accept(() -> observer.onAuctionEvent(eventName, auctionId, json));
        }
    }
}