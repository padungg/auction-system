package com.auction.model.dto;

/**
 * Gói tin DTO do Client gởi lên khi bấm nút "Đặt Giá".
 * Lưu ý: KHÔNG cần biến 'userId' vì Server sẽ tự biết ai đang gửi dựa vào luồng
 * Socket.
 */
public class BidRequestDTO {

    private String auctionId;
    private double bidAmount;

    public BidRequestDTO() {
    }

    public BidRequestDTO(String auctionId, double bidAmount) {
        this.auctionId = auctionId;
        this.bidAmount = bidAmount;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }
}
