package com.auction.client.util;

import javafx.scene.image.Image;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiện ích bộ nhớ đệm hình ảnh (Memory Image Cache).
 * Giúp lưu trữ các hình ảnh đã được giải mã từ Base64 để tránh việc
 * giải mã lại nhiều lần gây giật lag giao diện người dùng.
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
     * Lấy ảnh từ bộ nhớ đệm.
     * @param auctionId Mã phiên đấu giá.
     * @return Đối tượng Image nếu đã tồn tại, hoặc null nếu chưa có.
     */
    public Image getImage(String auctionId) {
        return cache.get(auctionId);
    }

    /**
     * Lưu trữ ảnh vào bộ nhớ đệm.
     * @param auctionId Mã phiên đấu giá.
     * @param image Đối tượng Image cần lưu.
     */
    public void putImage(String auctionId, Image image) {
        if (auctionId != null && image != null) {
            cache.put(auctionId, image);
        }
    }
    
    /**
     * Xóa toàn bộ bộ nhớ đệm nếu cần giải phóng RAM.
     */
    public void clearCache() {
        cache.clear();
    }
}
