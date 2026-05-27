package com.auction.client.observer;

import com.google.gson.JsonObject;

/**
 * Nhận sự kiện đấu giá thời gian thực từ Server.
 */
public interface AuctionEventObserver {

    /**
     * Callback được gọi khi có sự kiện đẩy về từ Server.
     */
    void onAuctionEvent(String event, String auctionId, JsonObject payload);
}