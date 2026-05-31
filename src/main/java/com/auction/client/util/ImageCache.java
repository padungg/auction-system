package com.auction.client.util;

import javafx.scene.image.Image;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bộ nhớ đệm hình ảnh (Image Cache) lưu trên RAM.
 * Tránh việc giải mã Base64 của ảnh sản phẩm nhiều lần làm giảm hiệu năng giao diện.
 */
public class ImageCache {
    private static final ImageCache INSTANCE = new ImageCache();
    private final ConcurrentHashMap<String, Image> cache;

    private ImageCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    public static ImageCache getInstance() {
        return INSTANCE;
    }

    /**
     * Lấy ảnh từ cache theo ID phiên đấu giá.
     */
    public Image getImage(String auctionId) {
        return cache.get(auctionId);
    }

    /**
     * Lưu ảnh vào cache.
     */
    public void putImage(String auctionId, Image image) {
        if (auctionId != null && image != null) {
            cache.put(auctionId, image);
        }
    }

    /**
     * Xóa toàn bộ cache để giải phóng bộ nhớ.
     */
    public void clearCache() {
        cache.clear();
    }
}
