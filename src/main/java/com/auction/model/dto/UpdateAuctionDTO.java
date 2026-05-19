package com.auction.model.dto;

/**
 * Gói tin DTO Client gửi lên khi muốn Cập nhật sản phẩm / phiên đấu giá.
 */
public class UpdateAuctionDTO extends CreateAuctionDTO {
    private String auctionId;

    public UpdateAuctionDTO() {
        super();
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }
}
