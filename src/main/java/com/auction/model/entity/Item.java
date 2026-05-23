package com.auction.model.entity;

import com.auction.model.dto.UpdateAuctionDTO;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm đấu giá.
 * Áp dụng đa hình thông qua các phương thức abstract
 * để các lớp con tự định nghĩa hành vi đặc thù mà không cần dùng instanceof.
 */
public abstract class Item extends Entity {
    private String name, description, condition, sellerId;
    private double startingPrice;

    public Item() {
    }

    public Item(String id, String name, String description, String condition, String sellerId, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.condition = condition;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
    }

    /**
     * Trả về loại item (ART, ELECTRONICS, VEHICLE).
     * Dùng để xác định kiểu sản phẩm mà không cần instanceof.
     */
    public abstract ItemType getItemType();

    /**
     * Áp dụng cập nhật các field đặc thù của từng loại item từ DTO.
     * Mỗi subclass chỉ cập nhật các field riêng của mình.
     */
    public abstract void applyUpdate(UpdateAuctionDTO dto);

    public abstract String getDetailInfo();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        if (startingPrice <= 0)
            throw new IllegalArgumentException("Giá khởi điểm phải > 0, nhận: " + startingPrice);
        this.startingPrice = startingPrice;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getSellerId() {
        return sellerId;
    }

    private String imageBase64;

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}