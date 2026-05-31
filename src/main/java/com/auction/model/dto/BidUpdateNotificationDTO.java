package com.auction.model.dto;

public class BidUpdateNotificationDTO {
    private final String event = "BID_UPDATE";
    private String auctionId;
    private double newPrice;
    private String bidderId;
    private String bidderName;
    private String itemName;
    private String bidTime;
    private String newEndTime;

    public BidUpdateNotificationDTO(String auctionId, double newPrice,
                                    String bidderId, String bidderName,
                                    String itemName, String bidTime,
                                    String newEndTime) {
        this.auctionId = auctionId;
        this.newPrice = newPrice;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.itemName = itemName;
        this.bidTime = bidTime;
        this.newEndTime = newEndTime;
    }

    public String getEvent() { return event; }
    public String getAuctionId() { return auctionId; }
    public double getNewPrice() { return newPrice; }
    public String getBidderId() { return bidderId; }
    public String getBidderName() { return bidderName; }
    public String getItemName() { return itemName; }
    public String getBidTime() { return bidTime; }
    public String getNewEndTime() { return newEndTime; }
}
