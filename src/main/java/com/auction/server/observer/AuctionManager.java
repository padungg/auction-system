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
 *   - ConcurrentHashMap: Bảo đảm an toàn khi tạo/xóa danh sách của từng phiên mà không chặn (block) toàn bộ đối tượng.
 *   - CopyOnWriteArrayList: Cho phép đọc + ghi đồng thời trên danh sách người xem mà không dính lỗi ConcurrentModificationException.
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
     */
    public void subscribe(String auctionId, AuctionObserver observer){
        List<AuctionObserver> observers = observerMap.get(auctionId);
        if (observers == null){
            observers = new CopyOnWriteArrayList<>();
            observerMap.put(auctionId, observers);
        }
        observers.add(observer);
        System.out.println("Observer: +1 người theo dõi phiên " + auctionId
                + " (tổng: " + observers.size() + ")");
    }
    /**
     * Hủy theo dõi 1 phiên đấu giá.
     */
    public void unsubscribe(String auctionId, AuctionObserver observer){
        List<AuctionObserver> observers = observerMap.get(auctionId);
        if (observers != null){
            observers.remove(observer);
            if (observers.isEmpty()){
                observerMap.remove(auctionId, observers);
            }
        }
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
     */
    public void notifyBidUpdate(String auctionId, double newPrice, String bidderId) {
        List<AuctionObserver> observers = observerMap.get(auctionId);
        if (observers != null && !observers.isEmpty()){
            for (AuctionObserver observer : observers){
                try{
                    observer.onBidUpdated(auctionId, newPrice, bidderId);
                }catch (Exception e){
                    System.out.println("Observer : Lỗi thông báo " + e.getMessage());
                }
            }
            System.out.println("Observer : Đã thông báo " + observers.size()
                    + " người về bid mới trên phiên " + auctionId);
        }
    }
}
