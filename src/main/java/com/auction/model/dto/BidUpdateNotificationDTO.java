package com.auction.model.dto;

public class BidUpdateNotificationDTO {
    private final String event = "BID_UPDATE";
    private String auctionId;
    private double newPrice;
    private String bidderId;
    private String bidTime;

    public BidUpdateNotificationDTO(String auctionId, double newPrice, String bidderId, String bidTime) {
        this.auctionId = auctionId;
        this.newPrice = newPrice;
        this.bidderId = bidderId;
        this.bidTime = bidTime;
    }

    public String getEvent() { return event; }
    public String getAuctionId() { return auctionId; }
    public double getNewPrice() { return newPrice; }
    public String getBidderId() { return bidderId; }
    public String getBidTime() { return bidTime; }
}
