package com.auction.server.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OBSERVER PATTERN + SINGLETON — quản lý ai đang xem phiên nào.
 * Khi có bid mới → AuctionManager duyệt danh sách observer thì gọi onBidUpdated() cho từng người.
 */
public class AuctionManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionManager.class);

    // Singleton — Holder pattern
    private static class Holder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    private final Map<String, List<AuctionObserver>> observerMap;

    private AuctionManager() {
        this.observerMap = new ConcurrentHashMap<>();
    }

    public static AuctionManager getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Đăng ký theo dõi 1 phiên đấu giá.
     */
    public void subscribe(String auctionId, AuctionObserver observer) {
        List<AuctionObserver> observers = observerMap
                .computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>());
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
        LOGGER.info("SUBSCRIBE: phiên={} | tổng observer={}", auctionId, observers.size());
    }

    /**
     * Hủy theo dõi 1 phiên đấu giá.
     */
    public void unsubscribe(String auctionId, AuctionObserver observer) {
        observerMap.computeIfPresent(auctionId, (key, observers) -> {
            observers.remove(observer);
            return observers.isEmpty() ? null : observers; // Tự động xóa khỏi Map nếu rỗng
        });
        LOGGER.info("UNSUBSCRIBE: phiên={}", auctionId);
    }

    /**
     * Hủy toàn bộ theo dõi của 1 observer (khi client ngắt kết nối).
     */
    public void unsubscribeAll(AuctionObserver observer) {
        for (String auctionId : observerMap.keySet()) {
            observerMap.computeIfPresent(auctionId, (key, observers) -> {
                observers.remove(observer);
                return observers.isEmpty() ? null : observers;
            });
        }
    }

    /**
     * Thông báo cho TẤT CẢ observer đang xem phiên này rằng có bid mới.
     */
    public void notifyBidUpdate(String auctionId, double newPrice, String bidderId, String bidTime) {
        List<AuctionObserver> observers = observerMap.get(auctionId);
        if (observers == null || observers.isEmpty()) return;

        int count = 0;
        for (AuctionObserver observer : observers) {
            try {
                observer.onBidUpdated(auctionId, newPrice, bidderId, bidTime);
                count++;
            } catch (Exception e) {
                LOGGER.error("NOTIFY_ERROR: {}", e.getMessage(), e);
            }
        }
        LOGGER.info("NOTIFY: phiên={} | giá={} VNĐ | đã notify {} client",
                auctionId, String.format("%,.0f", newPrice), count);
    }

    /**
     * Thông báo phiên đấu giá đã đóng
     * Được gọi bởi AuctionScheduler khi hết hạn, hoặc người bán kết thúc sớm.
     */
    public void notifyAuctionClosed(String auctionId, double finalPrice, String winnerId) {
        List<AuctionObserver> observers = observerMap.remove(auctionId);

        if (observers != null && !observers.isEmpty()) {
            for (AuctionObserver observer : observers) {
                try {
                    observer.onAuctionClosed(auctionId, finalPrice, winnerId);
                } catch (Exception e) {
                    LOGGER.error("CLOSE_NOTIFY_ERROR: {}", e.getMessage(), e);
                }
            }
        }

        LOGGER.info("AUCTION_CLOSED: phiên={} | giá cuối={} VNĐ | winner={}",
                auctionId, String.format("%,.0f", finalPrice), winnerId != null ? winnerId : "Không có");
    }
}
