package com.auction.server.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OBSERVER PATTERN + SINGLETON — "Tổng đài" quản lý ai đang xem phiên nào.
 *
 * Khi có bid mới → AuctionManager duyệt danh sách observer → gọi onBidUpdated() cho từng người.
 *
 * Thread-safety:
 *   - ConcurrentHashMap    : an toàn khi đọc/ghi map (nhiều thread subscribe cùng lúc)
 *   - CopyOnWriteArrayList : an toàn khi notify đang chạy mà có subscribe mới xảy ra
 *   - computeIfAbsent      : tạo list atomícally, không bị race condition khi khởi tạo
 *   - try-catch trong notify: 1 client lỗi không làm gãy toàn bộ thông báo
 * Không dùng polling — chỉ push đúng khi có sự kiện (event-based).
 */
public class AuctionManager {
    //Singleton
    private static AuctionManager instance;
    // Map: auctionId -> danh sách observer đang theo dõi phiên đó
    private final Map<String, List<AuctionObserver>> observerMap;
    private AuctionManager() {
        this.observerMap = new ConcurrentHashMap<>();
    }
    public static synchronized AuctionManager getInstance() {
        if (instance == null){
            instance = new AuctionManager();
        }
        return instance;
    }
    /**
     * Đăng ký theo dõi 1 phiên đấu giá.
     * computeIfAbsent: tạo list atomically — thậm chí 2 thread gọi cùng lúc cũng chỉ có 1 list.
     */
    public void subscribe(String auctionId, AuctionObserver observer) {
        List<AuctionObserver> observers = observerMap
                .computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>());
        if (!observers.contains(observer)) { // tránh subscribe trùng lặp
            observers.add(observer);
        }
        System.out.println("[AuctionManager] SUBSCRIBE: phiên=" + auctionId
                + " | tổng observer=" + observers.size());
    }
    /**
     * Hủy theo dõi 1 phiên đấu giá.
     */
    public void unsubscribe(String auctionId, AuctionObserver observer) {
        List<AuctionObserver> observers = observerMap.get(auctionId);
        if (observers != null) {
            observers.remove(observer);
            if (observers.isEmpty()) {
                observerMap.remove(auctionId, observers);
            }
        }
        System.out.println("[AuctionManager] UNSUBSCRIBE: phiên=" + auctionId);
    }
    /**
     * Hủy toàn bộ theo dõi của 1 observer (khi client ngắt kết nối).
     */
    public void unsubscribeAll(AuctionObserver observer){
        for (Map.Entry<String, List<AuctionObserver>> entry : observerMap.entrySet()) {
            List<AuctionObserver> observers = entry.getValue();
            observers.remove(observer);
            if (observers.isEmpty()){
                observerMap.remove(entry.getKey(), observers);
            }
        }
    }
    /**
     * Thông báo cho TẤT CẢ observer đang xem phiên này rằng có bid mới.
     * Không dùng polling — chỉ gọi khi có sự kiện xảy ra (event-based).
     * try-catch từng observer: 1 client lỗi không làm gãy việc thông báo các client khác.
     */
    public void notifyBidUpdate(String auctionId, double newPrice, String bidderId) {
        List<AuctionObserver> observers = observerMap.get(auctionId);
        if (observers == null || observers.isEmpty()) return;

        int count = 0;
        for (AuctionObserver observer : observers) {
            try {
                observer.onBidUpdated(auctionId, newPrice, bidderId);
                count++;
            } catch (Exception e) {
                System.out.println("[AuctionManager] NOTIFY_ERROR: " + e.getMessage());
            }
        }
        System.out.println("[AuctionManager] NOTIFY: phiên=" + auctionId
                + " | giá=" + String.format("%,.0f", newPrice)
                + " | đã notify " + count + " client");
    }
}
