package com.auction.model.entity;

import com.auction.model.dto.UpdateAuctionDTO;

/**
 * Thực thể đại diện cho sản phẩm đồ điện tử.
 * Kế thừa lớp Item và tự triển khai (override) logic cập nhật/thông tin riêng của mình.
 */
public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;


    public Electronics() {
    }

    public Electronics(String id, String name, String description, String condition, String sellerId,
                       double startingPrice, String brand, int warrantyMonths) {
        super(id, name, description, condition, sellerId, startingPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    @Override
    public ItemType getItemType() {
        return ItemType.ELECTRONICS;
    }

    @Override
    public void applyUpdate(UpdateAuctionDTO dto) {
        if (dto.getBrand() != null && !dto.getBrand().trim().isEmpty())
            this.setBrand(dto.getBrand());
        if (dto.getWarrantyMonths() > 0)
            this.setWarrantyMonths(dto.getWarrantyMonths());
    }

    @Override
    public String getDetailInfo() {
        return "Đồ điện tử: " + this.getName() + " Hãng: " + brand + " Bảo hành: " + warrantyMonths
                + "tháng Tình trạng: " + this.getCondition();
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(int warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}
