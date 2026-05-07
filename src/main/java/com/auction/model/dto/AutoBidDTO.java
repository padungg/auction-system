package com.auction.model.dto;

/**
 * DTO để client đăng ký Auto-Bid cho 1 phiên đấu giá.
 *
 * Ý nghĩa:
 *   - auctionId   : phiên muốn tham gia
 *   - maxBid      : giá tối đa sẵn sàng trả (hệ thống KHÔNG vượt ngưỡng này)
 *   - increment   : bước tăng mỗi lần tự động phản giá
 */
public class AutoBidDTO {

    private String auctionId;
    private double maxBid;
    private double increment;

    public AutoBidDTO() {}

    public AutoBidDTO(String auctionId, double maxBid, double increment) {
        this.auctionId = auctionId;
        this.maxBid    = maxBid;
        this.increment = increment;
    }

    public String getAuctionId()          { return auctionId; }
    public void   setAuctionId(String v)  { this.auctionId = v; }

    public double getMaxBid()             { return maxBid; }
    public void   setMaxBid(double v)     { this.maxBid = v; }

    public double getIncrement()          { return increment; }
    public void   setIncrement(double v)  { this.increment = v; }
}
