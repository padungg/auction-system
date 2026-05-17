package com.auction.model.entity;
import java.time.LocalDateTime;

/**
 * Thực thể đại diện cho một phiên đấu giá.
 * Một số trường (itemId, startTime) không có setter vì không bao giờ thay đổi sau khi tạo.
 */
public class Auction extends Entity{
    private String itemId, currentWinnerId;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public Auction() {
    }

    public Auction(String id, String itemId, double currentPrice, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.itemId = itemId;
        this.currentWinnerId = null;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AuctionStatus.OPEN;
    }

    public String getItemId() {
        return itemId;
    }

    public String getCurrentWinnerId() {
        return currentWinnerId;
    }

    public void setCurrentWinnerId(String currentWinnerId) {
        this.currentWinnerId = currentWinnerId;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    /**
     * Trả về số giây còn lại của phiên đấu giá.
     * Âm nếu phiên đã hết thời gian.
     */
    public long getSecondsRemaining() {
        return java.time.Duration.between(LocalDateTime.now(), endTime).getSeconds();
    }

    /**
     * Gia hạn thời gian kết thúc phiên thêm một số giây nhất định.
     * @param seconds số giây gia hạn, phải > 0
     */
    public void extendEndTime(long seconds) {
        if (seconds <= 0) throw new IllegalArgumentException("Thời gian gia hạn phải > 0");
        this.endTime = this.endTime.plusSeconds(seconds);
    }
}
