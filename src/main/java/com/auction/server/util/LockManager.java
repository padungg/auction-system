package com.auction.server.util;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Quản lý khóa (Lock) theo Auction ID để xử lý đồng bộ
 * Dùng chung cho BidService và AutoBidService để tránh Race Condition trên cùng một phiên đấu giá
 */
public class LockManager {
    private static final ConcurrentHashMap<String, Object> auctionLocks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();

    public static Object getAuctionLock(String auctionId) {
        return auctionLocks.computeIfAbsent(auctionId, ignored -> new Object());
    }

    public static Object getUserLock(String userId) {
        return userLocks.computeIfAbsent(userId, ignored -> new Object());
    }

    /** Xóa lock của phiên đấu giá (gọi khi xóa phiên) để tránh memory leak */
    public static void removeAuctionLock(String auctionId) {
        auctionLocks.remove(auctionId);
    }

    /** Xóa lock của user (gọi khi xóa tài khoản) để tránh memory leak */
    public static void removeUserLock(String userId) {
        userLocks.remove(userId);
    }
}
