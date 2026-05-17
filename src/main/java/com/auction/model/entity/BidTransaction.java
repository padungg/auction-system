package com.auction.model.entity;
import java.time.LocalDateTime;

/**
 * Lịch sử một lượt đặt giá (Bid).
 * Lớp này được thiết kế theo kiểu Bất biến (Immutable) đối với các trường quan trọng,
 * không có setter để đảm bảo lịch sử giao dịch không thể bị giả mạo hay sửa đổi sau khi tạo.
 */
public class BidTransaction extends Entity{
    private String bidderId, auctionId;
    private double bidAmount;
    private LocalDateTime bidTime;
    private boolean isAutoBid = false;

    public BidTransaction() {
    }

    public BidTransaction(String id, String bidderId, String auctionId, double bidAmount, LocalDateTime bidTime) {
        super(id);
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isAutoBid = false;
    }

    public BidTransaction(String id, String bidderId, String auctionId, double bidAmount, LocalDateTime bidTime, boolean isAutoBid) {
        super(id);
        this.bidderId = bidderId;
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
        this.bidTime = bidTime;
        this.isAutoBid = isAutoBid;
    }

    public String getBidderId() {
        return bidderId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    public boolean isAutoBid() {
        return isAutoBid;
    }

    public String getInfo(){
        return "Id: " + this.getId() + " User: " + bidderId + " AuctionId: " + auctionId + " Amount: " + bidAmount + " Thời gian: " + bidTime;
    }
}
