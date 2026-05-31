package com.auction.model.dto;

public class AuctionClosedNotificationDTO {
    private final String event = "AUCTION_CLOSED";
    private String auctionId;
    private double finalPrice;
    private String winnerId;

    public AuctionClosedNotificationDTO(String auctionId, double finalPrice, String winnerId) {
        this.auctionId = auctionId;
        this.finalPrice = finalPrice;
        this.winnerId = winnerId;
    }

    public String getEvent() { return event; }
    public String getAuctionId() { return auctionId; }
    public double getFinalPrice() { return finalPrice; }
    public String getWinnerId() { return winnerId; }
}
