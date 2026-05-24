package com.auction.client.observer;

import com.google.gson.JsonObject;

/**
 * Giao diện đăng ký và đón nhận sự kiện đấu giá thời gian thực.
 */
public interface AuctionEventObserver {

    /**
     * Hàm phản hồi tự động (Callback) khi máy chủ đẩy sự kiện về Client.
     */
    void onAuctionEvent(String event, String auctionId, JsonObject payload);
}